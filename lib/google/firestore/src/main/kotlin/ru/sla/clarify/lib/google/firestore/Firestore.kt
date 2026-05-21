@file:Suppress("IgnoredReturnValue", "RedundantSuspendModifier")

package ru.sla.clarify.lib.google.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.domain.randomUuid
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.ConversationType
import ru.sla.clarify.lib.google.firestore.entity.FirestoreCommit
import ru.sla.clarify.lib.google.firestore.entity.FirestoreConversation
import ru.sla.clarify.lib.google.firestore.mapper.extractCommitFB
import ru.sla.clarify.lib.google.firestore.mapper.extractConversationFB
import ru.sla.clarify.lib.google.firestore.mapper.mapChanges
import java.time.LocalDateTime
import javax.inject.Inject

@SingleIn(AppScope::class)
class Firestore @Inject constructor(
  firestoreWrapper: FirestoreWrapper,
  private val authSessionPersistence: AuthSessionPersistence
) : FirestoreWrapperProvider by firestoreWrapper {

  suspend fun mergeUser(
    id: UserId,
    photoUrl: String?,
    displayName: String?
  ) {
    val params = mapOf(
      FirestoreSchema.USER_DISPLAY_NAME to displayName,
      FirestoreSchema.USER_PHOTO_URL to photoUrl,
      FirestoreSchema.USER_CREATED_AT to FieldValue.serverTimestamp(),
      FirestoreSchema.USER_UPDATED_AT to FieldValue.serverTimestamp()
    )
    userDocumentRef(id)
      .set(params, SetOptions.merge())
      .await()
  }

  suspend fun directConversation(peerId: Peer.Id): FirestoreConversation? {
    val senderId = requireUserId()
    val query = conversationsQuery(
      whereEqualTo = ConversationType.Direct,
      whereArrayContains = senderId
    )
    return query
      .get()
      .await()
      .findDirectConversation(directParticipantIds(senderId, peerId))
  }

  fun observeConversations(): Flow<List<FirestoreConversation>> {
    return callbackFlow {
      val userId = requireUserId()
      val query = conversationsQuery(
        whereArrayContains = userId
      )
      val listener = query.addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        trySend(snapshot.mapChanges { extractConversationFB(it.document, it.type) })
      }
      awaitClose { listener.remove() }
    }
  }

  suspend fun markConversationAsRead(conversationId: FirestoreConversation.Id) {
    val userId = requireUserId()
    unreadCommitsDocumentRef(conversationId, userId)
      .set(
        mapOf(FirestoreSchema.UNREAD_COMMITS_COUNT to 0L),
        SetOptions.merge()
      )
      .await()
  }

  fun observeUnreadCount(conversationId: FirestoreConversation.Id): Flow<Long> {
    return callbackFlow {
      val userId = requireUserId()
      val listener = unreadCommitsDocumentRef(conversationId, userId)
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }
          trySend(snapshot?.getLong(FirestoreSchema.UNREAD_COMMITS_COUNT) ?: 0L)
        }
      awaitClose { listener.remove() }
    }
  }

  suspend fun deleteConversations(ids: List<String>) {
    val batch = remoteDB.batch()
    val reference = conversationCollectionRef()
    ids.forEach { id -> batch.delete(reference.document(id)) }
    batch.commit().await()
  }

  fun observeDirectCommits(
    peerId: Peer.Id,
    limit: Long
  ): Flow<List<FirestoreCommit>> {
    return observeDirectConversationId(peerId)
      .distinctUntilChanged()
      .flatMapLatest { conversationId ->
        if (conversationId == null) {
          flowOf(emptyList())
        } else {
          observeConversationMessages(
            id = conversationId,
            limit = limit
          )
        }
      }
  }

  suspend fun historyCommits(
    conversationId: FirestoreConversation.Id,
    count: Int,
    before: LocalDateTime?
  ): List<FirestoreCommit> {
    var query = commitsCollectionRef(conversationId)
      .orderBy(FirestoreSchema.COMMIT_CREATED_AT, Query.Direction.DESCENDING)
      .limit(count.toLong())

    if (before != null) {
      query = query.whereLessThan(FirestoreSchema.COMMIT_CREATED_AT, before.toTimestamp())
    }

    return query
      .get()
      .await()
      .documents
      .map { extractCommitFB(it, conversationId) }
  }

  suspend fun sendCommit(
    conversationId: FirestoreConversation.Id?,
    peerId: Peer.Id,
    text: String,
    colorHex: String
  ): FirestoreCommit {
    val conversationId = conversationId ?: FirestoreConversation.Id(randomUuid())
    val senderId = requireUserId()

    val commitId = randomUuid()
    val createdAt = Timestamp.now()
    val participantIds = directParticipantIds(senderId, peerId)

    val messageData = buildMap {
      put(FirestoreSchema.COMMIT_CLIENT_COMMIT_ID, commitId)
      put(FirestoreSchema.COMMIT_SENDER_UID, senderId.value)
      put(FirestoreSchema.COMMIT_TEXT, text)
      put(FirestoreSchema.COMMIT_TYPE, FirestoreSchema.CommitType.Text.value)
      put(FirestoreSchema.COMMIT_CREATED_AT, createdAt)
      put(FirestoreSchema.COMMIT_SERVER_CREATED_AT, FieldValue.serverTimestamp())
      put(FirestoreSchema.COMMIT_READ_BY, listOf(senderId.value))
      put(FirestoreSchema.COMMIT_COLOR_HEX, colorHex)
    }

    val conversationData = buildMap {
      put(FirestoreSchema.CONVERSATION_TYPE, ConversationType.Direct.value)
      put(FirestoreSchema.CONVERSATION_PARTICIPANT_UIDS, participantIds)
      put(FirestoreSchema.CONVERSATION_LAST_COMMIT_TEXT, text)
      put(FirestoreSchema.CONVERSATION_LAST_COMMIT_SENDER_UID, senderId.value)
      put(FirestoreSchema.CONVERSATION_LAST_COMMIT_AT, createdAt)
      put(FirestoreSchema.CONVERSATION_UPDATED_AT, FieldValue.serverTimestamp())
    }

    val batch = remoteDB.batch()

    val conversationRef = conversationDocumentRef(conversationId)

    val messageRef = conversationRef
      .collection(FirestoreSchema.COMMITS_COLLECTION)
      .document(commitId)

    batch.set(conversationRef, conversationData, SetOptions.merge())
    batch.set(messageRef, messageData)

    participantIds
      .filter { it != senderId.value }
      .forEach { peerId ->
        batch.set(
          unreadCommitsDocumentRef(conversationId, UserId(peerId)),
          mapOf(FirestoreSchema.UNREAD_COMMITS_COUNT to FieldValue.increment(1)),
          SetOptions.merge()
        )
      }

    batch.commit().await()

    val message = FirestoreCommit(
      commitId = FirestoreCommit.Id(commitId),
      conversationId = conversationId,
      senderId = senderId,
      text = text,
      colorHex = colorHex,
      createdAtEpochSeconds = createdAt.toEpochSeconds(),
      changeType = null
    )
    return message
  }

  private fun observeDirectConversationId(peerId: Peer.Id): Flow<FirestoreConversation.Id?> {
    return callbackFlow {
      val currentUid = requireUserId()
      val directParticipantIds = directParticipantIds(currentUid, peerId)

      val query = conversationsQuery(
        whereEqualTo = ConversationType.Direct,
        whereArrayContains = currentUid
      )

      val listener = query.addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        trySend(snapshot?.findDirectConversation(directParticipantIds)?.id)
      }
      awaitClose { listener.remove() }
    }
  }

  private fun observeConversationMessages(
    id: FirestoreConversation.Id,
    limit: Long
  ): Flow<List<FirestoreCommit>> {
    return callbackFlow {
      val listener = commitsCollectionRef(id)
        .orderBy(FirestoreSchema.COMMIT_CREATED_AT, Query.Direction.DESCENDING)
        .limit(limit)
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }
          trySend(snapshot.mapChanges { extractCommitFB(it.document, id, it.type) })
        }
      awaitClose { listener.remove() }
    }
  }

  private fun directParticipantIds(userId: UserId, peerId: Peer.Id): List<String> {
    return setOf(userId.value, peerId.value).sorted()
  }

  private fun QuerySnapshot?.findDirectConversation(ids: List<String>): FirestoreConversation? {
    return this?.documents
      ?.map(::extractConversationFB)
      ?.firstOrNull { it.participantUids == ids }
  }

  private suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) }) {
      "Current user not found in persistence"
    }
  }
}
