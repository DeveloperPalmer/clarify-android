@file:Suppress("IgnoredReturnValue", "RedundantSuspendModifier")

package ru.sla.clarify.lib.google.firestore

import com.google.firebase.Timestamp
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
import ru.sla.clarify.lib.google.firestore.codec.FirestoreFormat
import ru.sla.clarify.lib.google.firestore.codec.decodeFromSnapshot
import ru.sla.clarify.lib.google.firestore.codec.encodeToMap
import ru.sla.clarify.lib.google.firestore.entity.BranchNM
import ru.sla.clarify.lib.google.firestore.entity.BranchNM.Status
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM
import ru.sla.clarify.lib.google.firestore.entity.MergeRequestNM
import ru.sla.clarify.lib.google.firestore.entity.ParticipantNM
import ru.sla.clarify.lib.google.firestore.entity.UserNM
import ru.sla.clarify.lib.google.firestore.entity.write.PatchBranchCancelMergeParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchBranchFinalizeMergeParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchBranchOpenMergeParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchUnreadCountParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchUnreadIncrementParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchUserParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostBranchParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostCommitParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostConversationParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostParticipantParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostUserParams
import ru.sla.clarify.lib.google.firestore.mapper.mapDocumentChanges
import ru.sla.clarify.lib.google.firestore.mapper.toFirestoreDocumentResult
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

  private val codec: FirestoreFormat = FirestoreFormat.Default

  suspend fun postUser(
    id: UserId,
    displayName: String?,
    photoUrl: String?,
    email: String?
  ) {
    val payload = codec.encodeToMap(
      PostUserParams(
        displayName = displayName,
        photoUrl = photoUrl,
        email = email
      )
    )
    userDocumentRef(id)
      .set(payload)
      .await()
  }

  suspend fun patchUser(
    id: UserId,
    displayName: String?,
    photoUrl: String?,
    email: String?
  ) {
    // Если на вход пришли только null'ы, в map'е остался бы один updatedAt —
    // нет смысла дергать сеть ради одного timestamp'а.
    if (displayName == null && photoUrl == null && email == null) return

    val payload = codec.encodeToMap(
      PatchUserParams(
        displayName = displayName,
        photoUrl = photoUrl,
        email = email
      )
    )
    userDocumentRef(id)
      .set(payload, SetOptions.merge())
      .await()
  }

  suspend fun isUserExists(id: UserId): Boolean {
    return userDocumentRef(id)
      .get()
      .await()
      .exists()
  }

  suspend fun getCurrentUser(): UserNM {
    val userId = requireUserId()
    val document = userDocumentRef(userId)
      .get()
      .await()

    if (!document.exists()) {
      error("User by id: ${userId.value} not found in Firestore")
    }

    return codec.decodeFromSnapshot<UserNM>(document)
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

  fun conversationsLive(): Flow<List<FirestoreChange<ConversationNM>>> {
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
        val response = snapshot.mapDocumentChanges { change ->
          FirestoreChange(
            data = codec.decodeFromSnapshot<ConversationNM>(change.document),
            changeType = change.type.toFirestoreDocumentResult()
          )
        }
        trySend(response)
      }

      awaitClose { listener.remove() }
    }
  }

  suspend fun patchUnreadCount(conversationId: String) {
    val userId = requireUserId()
    unreadCommitsDocumentRef(conversationId, userId)
      .set(
        codec.encodeToMap(PatchUnreadCountParams(count = 0L)),
        SetOptions.merge()
      )
      .await()
  }

  fun unreadCountLive(conversationId: String): Flow<Long> {
    return callbackFlow {
      val userId = requireUserId()
      listenerGuard.trackOpen("unreadCountLive:$conversationId:${userId.value}")

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
    val batch = writeBatch()
    val reference = conversationCollectionRef()
    ids.forEach { id -> batch.delete(reference.document(id)) }
    batch.commit().await()
  }

  suspend fun getParticipants(
    conversationId: String
  ): List<ParticipantNM> {
    return participantsCollectionRef(conversationId)
      .get()
      .await()
      .documents
      .map { codec.decodeFromSnapshot<ParticipantNM>(it) }
  }

  fun directCommitsLive(
    peerId: Peer.Id,
    branchId: String,
    limit: Long
  ): Flow<List<FirestoreChange<CommitNM>>> {
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
    conversationId: String,
    branchId: String,
    count: Int,
    before: LocalDateTime?
  ): List<CommitNM> {
    var query = commitsCollectionRef(conversationId)
      .whereEqualTo(FirestoreSchema.COMMIT_BRANCH_ID, branchId)
      .orderBy(FirestoreSchema.COMMIT_CREATED_AT, Query.Direction.DESCENDING)
      .limit(count.toLong())

    if (before != null) {
      query = query.whereLessThan(FirestoreSchema.COMMIT_CREATED_AT, before.toTimestamp())
    }

    return query
      .get()
      .await()
      .documents
      .map { codec.decodeFromSnapshot<CommitNM>(it) }
  }

  suspend fun postCommit(
    peerId: Peer.Id,
    branchId: String?,
    conversationId: String?,
    text: String,
    colorHex: String
  ) {
    val senderId = requireUserId()
    val isNewConversation = conversationId == null
    val conversationId = conversationId ?: randomUuid()
    // null branchId означает «писать в корень conversation» (master-ветка). У master-ветки
    // нет своего документа — её id совпадает с conversationId, — поэтому резолвим лениво
    // здесь, уже после того как conversationId мог быть сгенерирован.
    val resolvedBranchId = branchId ?: conversationId

    val commitId = randomUuid()
    val createdAt = Timestamp.now()
    val participantIds = directParticipantIds(senderId, peerId)
    val isRoot = resolvedBranchId == conversationId

    val batch = writeBatch()
    val conversationRef = conversationDocumentRef(conversationId)

    val messageData = codec.encodeToMap(
      PostCommitParams(
        clientCommitId = commitId,
        senderUid = senderId,
        text = text,
        type = CommitNM.Type.Text,
        createdAt = createdAt,
        readBy = listOf(senderId.value),
        colorHex = colorHex,
        branchId = resolvedBranchId
      )
    )

    val conversationData = codec.encodeToMap(
      PostConversationParams(
        type = ConversationNM.Type.Direct,
        participantUids = participantIds,
        lastCommitText = text.takeIf { isRoot },
        lastCommitSenderUid = senderId.value.takeIf { isRoot },
        lastCommitAt = createdAt.takeIf { isRoot }
      )
    )

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
        val participantData = codec.encodeToMap(
          PostParticipantParams(
            displayName = userSnapshot.getString(FirestoreSchema.USER_DISPLAY_NAME),
            photoUrl = userSnapshot.getString(FirestoreSchema.USER_PHOTO_URL)
          )
        )
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
            codec.encodeToMap(PatchUnreadIncrementParams()),
            SetOptions.merge()
          )
        }
    }

    batch.commit().await()
  }

  fun branchesLive(
    conversationId: String
  ): Flow<List<FirestoreChange<BranchNM>>> {
    return callbackFlow {
      listenerGuard.trackOpen("branchesLive:$conversationId")

      val listener = branchesCollectionRef(
        conversationId = conversationId
      ).addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        val response = snapshot.mapDocumentChanges { change ->
          FirestoreChange(
            changeType = change.type.toFirestoreDocumentResult(),
            data = codec.decodeFromSnapshot<BranchNM>(change.document)
          )
        }
        trySend(response)
      }

      awaitClose { listener.remove() }
    }
  }

  suspend fun postBranch(
    conversationId: String,
    parentBranchId: String,
    branchedFromCommitId: String,
    name: String
  ): BranchNM {
    val createdAt = Timestamp.now()
    val createdByUserId = requireUserId()

    val branchId = randomUuid()

    val branchData = codec.encodeToMap(
      PostBranchParams(
        parentBranchId = parentBranchId,
        branchedFromCommitId = branchedFromCommitId,
        name = name,
        status = Status.Active,
        createdAt = createdAt,
        createdByUid = createdByUserId
      )
    )

    branchDocumentRef(conversationId, branchId)
      .set(branchData)
      .await()

    return BranchNM(
      id = branchId,
      parentBranchId = parentBranchId,
      branchedFromCommitId = branchedFromCommitId,
      name = name,
      status = Status.Active,
      createdAt = createdAt,
      createdByUid = createdByUserId.value,
      mergeRequest = null,
      mergedAt = null,
      mergedIntoBranchId = null
    )
  }

  /**
   * Открывает merge request для ветки. Атомарно: проходит только если ветка сейчас
   * в статусе [Status.Active]. Записывает `mergeRequest = { initiator, requestedAt,
   * approvedByUids: [initiator] }` и переводит статус в [Status.MergeInProgress].
   */
  suspend fun postMergeRequest(
    conversationId: String,
    branchId: String
  ) {
    val initiator = requireUserId()
    val branchRef = branchDocumentRef(conversationId, branchId)
    val requestedAt = Timestamp.now()

    val transaction = runTransaction { txn ->
      val current = codec.decodeFromSnapshot<BranchNM>(txn.get(branchRef))
      check(current.status == Status.Active) {
        "Cannot request merge: branch status is ${current.status}, expected ${Status.Active}"
      }
      val update = codec.encodeToMap(
        PatchBranchOpenMergeParams(
          status = Status.MergeInProgress,
          mergeRequest = MergeRequestNM(
            initiatorUid = initiator.value,
            requestedAt = requestedAt,
            approvedByUids = listOf(initiator.value)
          )
        )
      )
      txn.update(branchRef, update)
    }

    transaction.await()
  }

  /**
   * Добавляет текущего пользователя в список approvers активного merge request'а. Атомарно.
   *
   * Если с учётом текущего пользователя [participantUids] покрыт полностью, ветка финализируется
   * в той же транзакции: status -> [Status.Merged], `mergedAt = now`,
   * `mergedIntoBranchId = parentBranchId`, `mergeRequest` удаляется.
   */
  suspend fun patchMergeApproval(
    conversationId: String,
    branchId: String,
    participantUids: List<String>
  ) {
    val approver = requireUserId()
    val branchRef = branchDocumentRef(conversationId, branchId)
    val now = Timestamp.now()
    val transaction = runTransaction { txn ->
      val current = codec.decodeFromSnapshot<BranchNM>(txn.get(branchRef))
      check(current.status == Status.MergeInProgress) {
        "Cannot approve merge: branch status is ${current.status}"
      }
      val approvedRaw = current.mergeRequest?.approvedByUids.orEmpty()
      val updatedApproved = (approvedRaw + approver.value).distinct()

      val finalize = participantUids
        .toSet()
        .subtract(updatedApproved.toSet())
        .isEmpty() && participantUids.isNotEmpty()

      if (finalize) {
        val update = codec.encodeToMap(
          PatchBranchFinalizeMergeParams(
            status = Status.Merged,
            mergedAt = now,
            mergedIntoBranchId = current.parentBranchId
          )
        )
        txn.update(branchRef, update)
      } else {
        // Partial field-path update: дописываем только approvedByUids внутри mergeRequest,
        // не трогая initiatorUid/requestedAt. NM-payload здесь не годится — нужно
        // именно "dot-path" обращение, которое Firestore разворачивает по месту.
        txn.update(
          branchRef,
          mapOf(
            "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_APPROVED_BY_UIDS}"
              to updatedApproved
          )
        )
      }
    }

    transaction.await()
  }

  /**
   * Убирает текущего пользователя из списка approvers. Если текущий пользователь — инициатор,
   * весь merge request отменяется (status -> [Status.Active]).
   */
  suspend fun deleteMergeApproval(
    conversationId: String,
    branchId: String
  ) {
    val approver = requireUserId()
    val branchRef = branchDocumentRef(conversationId, branchId)
    val transaction = runTransaction { txn ->
      val current = codec.decodeFromSnapshot<BranchNM>(txn.get(branchRef))
      check(current.status == Status.MergeInProgress) {
        "Cannot revoke approval: branch status is ${current.status}"
      }
      val mergeRequest = current.mergeRequest
        ?: error("branch in MergeInProgress has no mergeRequest")
      if (mergeRequest.initiatorUid == approver.value) {
        // Отзыв со стороны инициатора означает отмену всего merge request'а.
        txn.update(branchRef, cancelMergePayload())
      } else {
        val updatedApproved = mergeRequest.approvedByUids.filter { it != approver.value }
        // Partial field-path update — см. комментарий в patchMergeApproval.
        txn.update(
          branchRef,
          mapOf(
            "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_APPROVED_BY_UIDS}"
              to updatedApproved
          )
        )
      }
    }

    transaction.await()
  }

  /**
   * Инициатор отменяет merge request. Ветка возвращается в [Status.Active].
   */
  suspend fun deleteMergeRequest(
    conversationId: String,
    branchId: String
  ) {
    val canceller = requireUserId()
    val branchRef = branchDocumentRef(conversationId, branchId)
    val transaction = runTransaction { txn ->
      val current = codec.decodeFromSnapshot<BranchNM>(txn.get(branchRef))
      check(current.status == Status.MergeInProgress) {
        "Cannot cancel merge: branch status is ${current.status}"
      }
      val initiator = current.mergeRequest?.initiatorUid
        ?: error("branch in MergeInProgress has no mergeRequest")
      check(initiator == canceller.value) {
        "Only the merge initiator can cancel the merge request"
      }
      txn.update(branchRef, cancelMergePayload())
    }

    transaction.await()
  }

  private fun cancelMergePayload(): Map<String, Any?> = codec.encodeToMap(
    PatchBranchCancelMergeParams(status = Status.Active)
  )

  private fun directConversationIdLive(peerId: Peer.Id): Flow<String?> {
    return callbackFlow {
      val currentUid = requireUserId()
      listenerGuard.trackOpen("directConversationIdLive:${currentUid.value}:${peerId.value}")

      val directParticipantIds = directParticipantIds(currentUid, peerId)

      val listener = conversationsQuery(
        whereEqualTo = ConversationNM.Type.Direct,
        whereArrayContains = currentUid
      ).addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        trySend(snapshot?.findDirectConversationId(directParticipantIds))
      }

      awaitClose { listener.remove() }
    }
  }

  private fun conversationMessagesLive(
    id: String,
    branchId: String,
    limit: Long
  ): Flow<List<FirestoreChange<CommitNM>>> {
    return callbackFlow {
      listenerGuard.trackOpen("conversationMessagesLive:$id:$branchId")

      val listener = commitsCollectionRef(id)
        .whereEqualTo(FirestoreSchema.COMMIT_BRANCH_ID, branchId)
        .orderBy(FirestoreSchema.COMMIT_CREATED_AT, Query.Direction.DESCENDING)
        .limit(limit)
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }
          val response = snapshot.mapDocumentChanges { change ->
            FirestoreChange(
              changeType = change.type.toFirestoreDocumentResult(),
              data = codec.decodeFromSnapshot<CommitNM>(change.document)
            )
          }
          trySend(response)
        }

      awaitClose { listener.remove() }
    }
  }

  private fun directParticipantIds(userId: UserId, peerId: Peer.Id): List<String> {
    return setOf(userId.value, peerId.value).sorted()
  }

  private fun QuerySnapshot?.findDirectConversationId(
    participantIds: List<String>
  ): String? {
    return this?.documents
      ?.firstOrNull { document ->
        codec.decodeFromSnapshot<ConversationNM>(document).participantUids == participantIds
      }
      ?.id
  }

  private suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) }) {
      "Current user not found in persistence"
    }
  }
}
