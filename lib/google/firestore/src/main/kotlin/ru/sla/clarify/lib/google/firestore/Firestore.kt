@file:Suppress("IgnoredReturnValue", "RedundantSuspendModifier")

package ru.sla.clarify.lib.google.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
  private val authSessionPersistence: AuthSessionPersistence
) {
  private val remoteDB = FirebaseFirestore.getInstance().apply {
    firestoreSettings = FirebaseFirestoreSettings.Builder()
      .build()
  }

  suspend fun mergeUser(
    uid: String,
    photoUrl: String?,
    displayName: String?
  ) {
    val params = mapOf(
      FirestoreSchema.USER_DISPLAY_NAME to displayName,
      FirestoreSchema.USER_PHOTO_URL to photoUrl,
      FirestoreSchema.USER_CREATED_AT to FieldValue.serverTimestamp(),
      FirestoreSchema.USER_UPDATED_AT to FieldValue.serverTimestamp()
    )
    remoteDB.collection(FirestoreSchema.USERS_COLLECTION)
      .document(uid)
      .set(params, SetOptions.merge())
      .await()
  }

  suspend fun directConversation(peerId: Peer.Id): FirestoreConversation? {
    val currentUid = requireUserId()
    val participantUids = directParticipantUids(currentUid, peerId)
    val querySnapshot = directConversationsQuery(currentUid)
      .get()
      .await()
    return querySnapshot.findDirectConversation(participantUids)
  }

  fun observeConversations(): Flow<List<FirestoreConversation>> {
    return callbackFlow {
      val userId = requireUserId()
      val registration = remoteDB
        .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
        .whereArrayContains(FirestoreSchema.CONVERSATION_PARTICIPANT_UIDS, userId.value)
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }
          trySend(
            element = snapshot.mapChanges { extractConversationFB(it.document, it.type) }
          )
        }
      awaitClose { registration.remove() }
    }
  }

  suspend fun hasConversation(peerId: Peer.Id): Boolean {
    return directConversation(peerId) != null
  }

  suspend fun markConversationAsRead(peerId: Peer.Id) {
    val currentUid = requireUserId()
    val conversationId = directConversation(peerId) ?: return

    val updated = buildMap {
      put(FirestoreSchema.STATE_LAST_READ_AT, FieldValue.serverTimestamp())
      put(FirestoreSchema.STATE_UNREAD_COUNT, 0L)
    }
    remoteDB
      .collection(FirestoreSchema.USERS_COLLECTION)
      .document(currentUid.value)
      .collection(FirestoreSchema.CONVERSATION_STATES_COLLECTION)
      .document(conversationId.id)
      .set(updated, SetOptions.merge())
      .await()
  }

  suspend fun deleteConversations(ids: List<String>) {
    val batch = remoteDB.batch()
    ids.forEach { id -> batch.delete(conversationCollection().document(id)) }
    batch.commit().await()
  }

  fun observeMessages(
    peerId: Peer.Id,
    limit: Long
  ): Flow<List<FirestoreCommit>> {
    return callbackFlow {
      val currentUid = requireUserId()
      val participantUids = directParticipantUids(currentUid, peerId)
      var observedConversationId: String? = null
      var messagesRegistration: ListenerRegistration? = null
      val conversationsRegistration = directConversationsQuery(currentUid)
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }
          val conversation = snapshot?.findDirectConversation(participantUids)

          if (conversation == null) {
            observedConversationId = null
            messagesRegistration?.remove()
            messagesRegistration = null
            trySend(emptyList())
            return@addSnapshotListener
          }

          if (observedConversationId == conversation.id) {
            return@addSnapshotListener
          }

          observedConversationId = conversation.id
          messagesRegistration?.remove()
          messagesRegistration = messagesCollection(conversation.id)
            .orderBy(FirestoreSchema.COMMIT_CREATED_AT, Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { messagesSnapshot, messagesError ->
              if (messagesError != null) {
                close(messagesError)
                return@addSnapshotListener
              }
              trySend(
                element = messagesSnapshot.mapChanges { extractCommitFB(it.document, it.type) }
              )
            }
        }
      awaitClose {
        conversationsRegistration.remove()
        messagesRegistration?.remove()
      }
    }
  }

  suspend fun loadMessageHistory(
    peerId: Peer.Id,
    count: Int,
    before: LocalDateTime?
  ): List<FirestoreCommit> {
    val conversation = directConversation(peerId) ?: return emptyList()

    var query = messagesCollection(conversation.id)
      .orderBy(FirestoreSchema.COMMIT_CREATED_AT, Query.Direction.DESCENDING)
      .limit(count.toLong())

    if (before != null) {
      query = query.whereLessThan(FirestoreSchema.COMMIT_CREATED_AT, before.toTimestamp())
    }

    return query
      .get()
      .await()
      .documents
      .map(::extractCommitFB)
  }

  suspend fun sendCommit(
    peerId: Peer.Id,
    text: String,
    colorHex: String
  ): FirestoreCommit {
    val senderId = requireUserId()
    val participantUids = listOf(senderId.value, peerId.value)
    val conversationId = directConversation(peerId)?.id ?: randomUuid()

    val commitId = randomUuid()
    val createdAt = Timestamp.now()

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
      put(
        FirestoreSchema.CONVERSATION_PARTICIPANT_UIDS,
        participantUids.sorted()
      )
      put(FirestoreSchema.CONVERSATION_LAST_COMMIT_TEXT, text)
      put(FirestoreSchema.CONVERSATION_LAST_COMMIT_SENDER_UID, senderId.value)
      put(FirestoreSchema.CONVERSATION_LAST_COMMIT_AT, createdAt)
      put(FirestoreSchema.CONVERSATION_UPDATED_AT, FieldValue.serverTimestamp())
    }

    val batch = remoteDB.batch()

    val conversationRef = remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId)

    val messageRef = conversationRef
      .collection(FirestoreSchema.COMMITS_COLLECTION)
      .document(commitId)

    batch.set(conversationRef, conversationData, SetOptions.merge())
    batch.set(messageRef, messageData)

    val collectionPath = remoteDB
      .collection(FirestoreSchema.USERS_COLLECTION)
      .document(senderId.value)
      .collection(FirestoreSchema.CONVERSATION_STATES_COLLECTION)
      .document(conversationId)

    val collectionStateUpdate = buildMap {
      put(FirestoreSchema.STATE_LAST_READ_AT, createdAt)
      put(FirestoreSchema.STATE_UNREAD_COUNT, 0L)
    }

    batch.set(collectionPath, collectionStateUpdate, SetOptions.merge())
    batch.commit().await()
    val message = FirestoreCommit(
      commitId = FirestoreCommit.Id(commitId),
      senderId = senderId,
      text = text,
      colorHex = colorHex,
      createdAtEpochSeconds = createdAt.toEpochSeconds(),
      changeType = null
    )
    return message
  }

  private fun directConversationsQuery(currentUid: UserId): Query {
    return conversationCollection()
      .whereEqualTo(
        FirestoreSchema.CONVERSATION_TYPE,
        ConversationType.Direct.value
      )
      .whereArrayContains(
        FirestoreSchema.CONVERSATION_PARTICIPANT_UIDS,
        currentUid.value
      )
  }

  private fun directParticipantUids(currentUid: UserId, peerId: Peer.Id): Set<String> {
    return setOf(currentUid.value, peerId.value)
  }

  private fun QuerySnapshot?.findDirectConversation(ids: Set<String>): FirestoreConversation? {
    return this?.documents
      ?.map(::extractConversationFB)
      ?.firstOrNull { ids.toSet() == ids }
  }

  private fun conversationCollection(): CollectionReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
  }

  private fun messagesCollection(conversationId: String): CollectionReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(FirestoreSchema.COMMITS_COLLECTION)
  }

  private suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) }) {
      "Current user not found in persistence"
    }
  }
}
