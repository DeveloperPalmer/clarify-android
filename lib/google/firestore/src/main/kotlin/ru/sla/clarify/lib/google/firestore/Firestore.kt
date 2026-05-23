@file:Suppress("IgnoredReturnValue", "RedundantSuspendModifier")

package ru.sla.clarify.lib.google.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
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
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.domain.randomUuid
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.BranchStatus
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.ConversationType
import ru.sla.clarify.lib.google.firestore.entity.FirestoreBranch
import ru.sla.clarify.lib.google.firestore.entity.FirestoreCommit
import ru.sla.clarify.lib.google.firestore.entity.FirestoreConversation
import ru.sla.clarify.lib.google.firestore.entity.FirestoreParticipant
import ru.sla.clarify.lib.google.firestore.mapper.extractBranchFB
import ru.sla.clarify.lib.google.firestore.mapper.extractCommitFB
import ru.sla.clarify.lib.google.firestore.mapper.extractConversationFB
import ru.sla.clarify.lib.google.firestore.mapper.extractParticipantFB
import ru.sla.clarify.lib.google.firestore.mapper.mapChanges
import java.time.LocalDateTime
import javax.inject.Inject

@SingleIn(AppScope::class)
class Firestore @Inject constructor(
  firestoreWrapper: FirestoreWrapper,
  private val authSessionPersistence: AuthSessionPersistence,
  private val listenerGuard: FirestoreListenerGuard
) : FirestoreWrapperProvider by firestoreWrapper {

  // Перед добавлением новых методов — прочитай соглашение об именовании.
  // root dir -> docs/firestore-naming-rules.md

  suspend fun patchUser(
    id: UserId,
    photoUrl: String?,
    displayName: String?,
    email: String?
  ) {
    val params = buildMap {
      put(FirestoreSchema.USER_DISPLAY_NAME, displayName)
      put(FirestoreSchema.USER_PHOTO_URL, photoUrl)
      put(FirestoreSchema.USER_EMAIL, email)
      put(FirestoreSchema.USER_CREATED_AT, FieldValue.serverTimestamp())
      put(FirestoreSchema.USER_UPDATED_AT, FieldValue.serverTimestamp())
    }
    userDocumentRef(id)
      .set(params, SetOptions.merge())
      .await()
  }

  suspend fun getCurrentUserEmail(): Email? {
    val userId = requireUserId()
    val email = userDocumentRef(userId)
      .get()
      .await()
      .getString(FirestoreSchema.USER_EMAIL)
    return email?.let(::Email)
  }

  suspend fun getPeerIdByEmail(email: Email): Peer.Id? {
    val snapshot = usersQuery(whereEqualTo = email.value.lowercase())
      .limit(1)
      .get()
      .await()
    return snapshot.documents.firstOrNull()
      ?.id
      ?.let(Peer::Id)
  }

  fun conversationsLive(): Flow<List<FirestoreConversation>> {
    return callbackFlow {
      val userId = requireUserId()
      listenerGuard.trackOpen("conversationsLive:${userId.value}")

      val listener = conversationsQuery(
        whereArrayContains = userId
      ).addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        trySend(snapshot.mapChanges { extractConversationFB(it.document, it.type) })
      }

      awaitClose { listener.remove() }
    }
  }

  suspend fun patchUnreadCount(conversationId: FirestoreConversation.Id) {
    val userId = requireUserId()
    unreadCommitsDocumentRef(conversationId, userId)
      .set(
        buildMap { put(FirestoreSchema.UNREAD_COMMITS_COUNT, 0L) },
        SetOptions.merge()
      )
      .await()
  }

  fun unreadCountLive(conversationId: FirestoreConversation.Id): Flow<Long> {
    return callbackFlow {
      val userId = requireUserId()
      listenerGuard.trackOpen("unreadCountLive:${conversationId.value}:${userId.value}")

      val listener = unreadCommitsDocumentRef(
        conversationId = conversationId,
        userId = userId
      ).addSnapshotListener { snapshot, error ->
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

  suspend fun getParticipants(
    conversationId: FirestoreConversation.Id
  ): List<FirestoreParticipant> {
    return participantsCollectionRef(conversationId)
      .get()
      .await()
      .documents
      .map(::extractParticipantFB)
  }

  fun directCommitsLive(
    peerId: Peer.Id,
    branchId: FirestoreBranch.Id,
    limit: Long
  ): Flow<List<FirestoreCommit>> {
    return directConversationIdLive(peerId)
      .distinctUntilChanged()
      .flatMapLatest { conversationId ->
        if (conversationId == null) {
          flowOf(emptyList())
        } else {
          conversationMessagesLive(
            id = conversationId,
            branchId = branchId,
            limit = limit
          )
        }
      }
  }

  suspend fun getCommits(
    conversationId: FirestoreConversation.Id,
    branchId: FirestoreBranch.Id,
    count: Int,
    before: LocalDateTime?
  ): List<FirestoreCommit> {
    var query = commitsCollectionRef(conversationId)
      .whereEqualTo(FirestoreSchema.COMMIT_BRANCH_ID, branchId.value)
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

  suspend fun postCommit(
    peerId: Peer.Id,
    branchId: FirestoreBranch.Id?,
    conversationId: FirestoreConversation.Id?,
    text: String,
    colorHex: String
  ): FirestoreCommit {
    val senderId = requireUserId()
    val isNewConversation = conversationId == null
    val conversationId = conversationId ?: FirestoreConversation.Id(randomUuid())
    // null branchId означает «писать в корень conversation» (master-ветка). У master-ветки
    // нет своего документа — её id совпадает с conversationId, — поэтому резолвим лениво
    // здесь, уже после того как conversationId мог быть сгенерирован.
    val resolvedBranchId = branchId ?: FirestoreBranch.Id(conversationId.value)

    val commitId = randomUuid()
    val createdAt = Timestamp.now()
    val participantIds = directParticipantIds(senderId, peerId)
    val isRoot = resolvedBranchId.value == conversationId.value

    val messageData = buildMap {
      put(FirestoreSchema.COMMIT_CLIENT_COMMIT_ID, commitId)
      put(FirestoreSchema.COMMIT_SENDER_UID, senderId.value)
      put(FirestoreSchema.COMMIT_TEXT, text)
      put(FirestoreSchema.COMMIT_TYPE, FirestoreSchema.CommitType.Text.value)
      put(FirestoreSchema.COMMIT_CREATED_AT, createdAt)
      put(FirestoreSchema.COMMIT_SERVER_CREATED_AT, FieldValue.serverTimestamp())
      put(FirestoreSchema.COMMIT_READ_BY, listOf(senderId.value))
      put(FirestoreSchema.COMMIT_COLOR_HEX, colorHex)
      put(FirestoreSchema.COMMIT_BRANCH_ID, resolvedBranchId.value)
    }

    val conversationData = buildMap {
      put(FirestoreSchema.CONVERSATION_TYPE, ConversationType.Direct.value)
      put(FirestoreSchema.CONVERSATION_PARTICIPANT_UIDS, participantIds)
      if (isRoot) {
        put(FirestoreSchema.CONVERSATION_LAST_COMMIT_TEXT, text)
        put(FirestoreSchema.CONVERSATION_LAST_COMMIT_SENDER_UID, senderId.value)
        put(FirestoreSchema.CONVERSATION_LAST_COMMIT_AT, createdAt)
      }
      put(FirestoreSchema.CONVERSATION_UPDATED_AT, FieldValue.serverTimestamp())
    }

    val batch = remoteDB.batch()

    val conversationRef = conversationDocumentRef(conversationId)

    val messageRef = conversationRef
      .collection(FirestoreSchema.COMMITS_COLLECTION)
      .document(commitId)

    batch.set(conversationRef, conversationData, SetOptions.merge())
    batch.set(messageRef, messageData)

    if (isNewConversation) {
      participantIds.forEach { uid ->
        val userId = UserId(uid)
        val userSnapshot = userDocumentRef(userId)
          .get()
          .await()
        val participantData = buildMap {
          put(FirestoreSchema.PARTICIPANT_ID, uid)
          put(FirestoreSchema.PARTICIPANT_DISPLAY_NAME, userSnapshot.getString(FirestoreSchema.USER_DISPLAY_NAME))
          put(FirestoreSchema.PARTICIPANT_PHOTO_URL, userSnapshot.getString(FirestoreSchema.USER_PHOTO_URL))
        }
        batch.set(
          participantDocumentRef(conversationId, userId),
          participantData
        )
      }
    }

    if (isRoot) {
      participantIds
        .filter { it != senderId.value }
        .forEach { peerId ->
          batch.set(
            unreadCommitsDocumentRef(conversationId, UserId(peerId)),
            buildMap { put(FirestoreSchema.UNREAD_COMMITS_COUNT, FieldValue.increment(1)) },
            SetOptions.merge()
          )
        }
    }

    batch.commit().await()

    val message = FirestoreCommit(
      commitId = FirestoreCommit.Id(commitId),
      conversationId = conversationId,
      branchId = resolvedBranchId,
      senderId = senderId,
      text = text,
      colorHex = colorHex,
      createdAtEpochSeconds = createdAt.toEpochSeconds(),
      changeType = null
    )
    return message
  }

  fun branchesLive(
    conversationId: FirestoreConversation.Id
  ): Flow<List<FirestoreBranch>> {
    return callbackFlow {
      listenerGuard.trackOpen("branchesLive:${conversationId.value}")

      val listener = branchesCollectionRef(
        conversationId = conversationId
      ).addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        trySend(snapshot.mapChanges { extractBranchFB(it.document, conversationId, it.type) })
      }

      awaitClose { listener.remove() }
    }
  }

  suspend fun postBranch(
    conversationId: FirestoreConversation.Id,
    parentBranchId: FirestoreBranch.Id,
    branchedFromCommitId: FirestoreCommit.Id,
    name: String
  ): FirestoreBranch {
    val createdAt = Timestamp.now()
    val createdByUserId = requireUserId()

    val branchId = FirestoreBranch.Id(randomUuid())

    val branchData = buildMap {
      put(FirestoreSchema.BRANCH_PARENT_ID, parentBranchId.value)
      put(FirestoreSchema.BRANCH_BRANCHED_FROM_COMMIT_ID, branchedFromCommitId.value)
      put(FirestoreSchema.BRANCH_NAME, name)
      put(FirestoreSchema.BRANCH_STATUS, BranchStatus.Active.value)
      put(FirestoreSchema.BRANCH_CREATED_AT, createdAt)
      put(FirestoreSchema.BRANCH_CREATED_BY_UID, createdByUserId.value)
    }

    branchDocumentRef(conversationId, branchId)
      .set(branchData)
      .await()

    return FirestoreBranch(
      id = branchId,
      conversationId = conversationId,
      parentBranchId = parentBranchId,
      branchedFromCommitId = branchedFromCommitId,
      name = name,
      status = BranchStatus.Active,
      createdAtEpochSeconds = createdAt.toEpochSeconds(),
      createdByUid = createdByUserId,
      mergeRequest = null,
      mergedAtEpochSeconds = null,
      mergedIntoBranchId = null,
      changeType = null
    )
  }

  /**
   * Открывает merge request для ветки. Атомарно: проходит только если ветка сейчас
   * в статусе [BranchStatus.Active]. Записывает `mergeRequest = { initiator, requestedAt,
   * approvedByUids: [initiator] }` и переводит статус в [BranchStatus.MergeInProgress].
   */
  suspend fun postMergeRequest(
    conversationId: FirestoreConversation.Id,
    branchId: FirestoreBranch.Id
  ) {
    val initiator = requireUserId()
    val branchRef = branchDocumentRef(conversationId, branchId)
    val requestedAt = Timestamp.now()

    val transaction = remoteDB.runTransaction { txn ->
      val snapshot = txn.get(branchRef)
      val currentStatus = snapshot.getString(FirestoreSchema.BRANCH_STATUS)
        ?: error("branch has no status")
      check(currentStatus == BranchStatus.Active.value) {
        "Cannot request merge: branch status is $currentStatus, expected ${BranchStatus.Active.value}"
      }
      val mergeRequestData = buildMap {
        put(FirestoreSchema.BRANCH_MERGE_REQUEST_INITIATOR_UID, initiator.value)
        put(FirestoreSchema.BRANCH_MERGE_REQUEST_REQUESTED_AT, requestedAt)
        put(FirestoreSchema.BRANCH_MERGE_REQUEST_APPROVED_BY_UIDS, listOf(initiator.value))
      }
      txn.update(
        branchRef,
        buildMap {
          put(FirestoreSchema.BRANCH_STATUS, BranchStatus.MergeInProgress.value)
          put(FirestoreSchema.BRANCH_MERGE_REQUEST, mergeRequestData)
        }
      )
    }

    transaction.await()
  }

  /**
   * Добавляет текущего пользователя в список approvers активного merge request'а. Атомарно.
   *
   * Если с учётом текущего пользователя [participantUids] покрыт полностью, ветка финализируется
   * в той же транзакции: status -> [BranchStatus.Merged], `mergedAt = now`,
   * `mergedIntoBranchId = parentBranchId`, `mergeRequest` удаляется.
   */
  suspend fun patchMergeApproval(
    conversationId: FirestoreConversation.Id,
    branchId: FirestoreBranch.Id,
    participantUids: List<String>
  ) {
    val approver = requireUserId()
    val branchRef = branchDocumentRef(conversationId, branchId)
    val now = Timestamp.now()
    val transaction = remoteDB.runTransaction { txn ->
      val snapshot = txn.get(branchRef)
      val currentStatus = snapshot.getString(FirestoreSchema.BRANCH_STATUS)
        ?: error("branch has no status")
      check(currentStatus == BranchStatus.MergeInProgress.value) {
        "Cannot approve merge: branch status is $currentStatus"
      }
      val approvedRaw = snapshot.mergeRequestApprovedUids().orEmpty()
      val updatedApproved = (approvedRaw + approver.value).distinct()
      val parentBranchId = snapshot.getString(FirestoreSchema.BRANCH_PARENT_ID)
        ?: error("branch has no parentBranchId")

      val finalize = participantUids
        .toSet()
        .subtract(updatedApproved.toSet())
        .isEmpty() && participantUids.isNotEmpty()

      if (finalize) {
        txn.update(
          branchRef,
          buildMap {
            put(FirestoreSchema.BRANCH_STATUS, BranchStatus.Merged.value)
            put(FirestoreSchema.BRANCH_MERGED_AT, now)
            put(FirestoreSchema.BRANCH_MERGED_INTO_BRANCH_ID, parentBranchId)
            put(FirestoreSchema.BRANCH_MERGE_REQUEST, FieldValue.delete())
          }
        )
      } else {
        txn.update(
          branchRef,
          buildMap {
            put(
              "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_APPROVED_BY_UIDS}",
              updatedApproved
            )
          }
        )
      }
    }

    transaction.await()
  }

  /**
   * Убирает текущего пользователя из списка approvers. Если текущий пользователь — инициатор,
   * весь merge request отменяется (status -> [BranchStatus.Active]).
   */
  suspend fun deleteMergeApproval(
    conversationId: FirestoreConversation.Id,
    branchId: FirestoreBranch.Id
  ) {
    val approver = requireUserId()
    val branchRef = branchDocumentRef(conversationId, branchId)
    val transaction = remoteDB.runTransaction { txn ->
      val snapshot = txn.get(branchRef)
      val currentStatus = snapshot.getString(FirestoreSchema.BRANCH_STATUS)
        ?: error("branch has no status")
      check(currentStatus == BranchStatus.MergeInProgress.value) {
        "Cannot revoke approval: branch status is $currentStatus"
      }
      val initiator = snapshot.mergeRequestInitiatorUid()
      if (initiator == approver.value) {
        // Отзыв со стороны инициатора означает отмену всего merge request'а.
        txn.update(
          branchRef,
          buildMap {
            put(FirestoreSchema.BRANCH_STATUS, BranchStatus.Active.value)
            put(FirestoreSchema.BRANCH_MERGE_REQUEST, FieldValue.delete())
          }
        )
      } else {
        val approvedRaw = snapshot.mergeRequestApprovedUids().orEmpty()
        val updatedApproved = approvedRaw.filter { it != approver.value }
        txn.update(
          branchRef,
          buildMap {
            put(
              "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_APPROVED_BY_UIDS}",
              updatedApproved
            )
          }
        )
      }
    }

    transaction.await()
  }

  /**
   * Инициатор отменяет merge request. Ветка возвращается в [BranchStatus.Active].
   */
  suspend fun deleteMergeRequest(
    conversationId: FirestoreConversation.Id,
    branchId: FirestoreBranch.Id
  ) {
    val canceller = requireUserId()
    val branchRef = branchDocumentRef(conversationId, branchId)
    val transaction = remoteDB.runTransaction { txn ->
      val snapshot = txn.get(branchRef)
      val currentStatus = snapshot.getString(FirestoreSchema.BRANCH_STATUS)
        ?: error("branch has no status")
      check(currentStatus == BranchStatus.MergeInProgress.value) {
        "Cannot cancel merge: branch status is $currentStatus"
      }
      val initiator = snapshot.mergeRequestInitiatorUid()
      check(initiator == canceller.value) {
        "Only the merge initiator can cancel the merge request"
      }
      txn.update(
        branchRef,
        buildMap {
          put(FirestoreSchema.BRANCH_STATUS, BranchStatus.Active.value)
          put(FirestoreSchema.BRANCH_MERGE_REQUEST, FieldValue.delete())
        }
      )
    }

    transaction.await()
  }

  private fun directConversationIdLive(peerId: Peer.Id): Flow<FirestoreConversation.Id?> {
    return callbackFlow {
      val currentUid = requireUserId()
      listenerGuard.trackOpen("directConversationIdLive:${currentUid.value}:${peerId.value}")

      val directParticipantIds = directParticipantIds(currentUid, peerId)

      val listener = conversationsQuery(
        whereEqualTo = ConversationType.Direct,
        whereArrayContains = currentUid
      ).addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        trySend(snapshot?.findDirectConversation(directParticipantIds)?.id)
      }

      awaitClose { listener.remove() }
    }
  }

  private fun conversationMessagesLive(
    id: FirestoreConversation.Id,
    branchId: FirestoreBranch.Id,
    limit: Long
  ): Flow<List<FirestoreCommit>> {
    return callbackFlow {
      listenerGuard.trackOpen("conversationMessagesLive:${id.value}:${branchId.value}")

      val listener = commitsCollectionRef(id)
        .whereEqualTo(FirestoreSchema.COMMIT_BRANCH_ID, branchId.value)
        .orderBy(FirestoreSchema.COMMIT_CREATED_AT, Query.Direction.DESCENDING)
        .limit(limit)
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }
          val changes = snapshot.mapChanges { extractCommitFB(it.document, id, it.type) }
          trySend(changes)
        }

      awaitClose { listener.remove() }
    }
  }

  private fun directParticipantIds(userId: UserId, peerId: Peer.Id): List<String> {
    return setOf(userId.value, peerId.value).sorted()
  }

  @Suppress("UNCHECKED_CAST")
  private fun DocumentSnapshot.mergeRequestApprovedUids(): List<String>? {
    val raw = get(FirestoreSchema.BRANCH_MERGE_REQUEST) as? Map<String, Any?> ?: return null
    return (raw[FirestoreSchema.BRANCH_MERGE_REQUEST_APPROVED_BY_UIDS] as? List<*>)
      ?.mapNotNull { it as? String }
  }

  @Suppress("UNCHECKED_CAST")
  private fun DocumentSnapshot.mergeRequestInitiatorUid(): String? {
    val raw = get(FirestoreSchema.BRANCH_MERGE_REQUEST) as? Map<String, Any?> ?: return null
    return raw[FirestoreSchema.BRANCH_MERGE_REQUEST_INITIATOR_UID] as? String
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
