package ru.sla.clarify.lib.google.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.MetadataChanges
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
import ru.sla.clarify.core.domain.entity.GroupName
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.domain.randomUuid
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.codec.codec
import ru.sla.clarify.lib.google.firestore.codec.decodeFromSnapshot
import ru.sla.clarify.lib.google.firestore.codec.encodeToMap
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ArrayRemove
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ArrayUnion
import ru.sla.clarify.lib.google.firestore.codec.sentinel.Delete
import ru.sla.clarify.lib.google.firestore.codec.sentinel.Increment
import ru.sla.clarify.lib.google.firestore.entity.BranchNM
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM
import ru.sla.clarify.lib.google.firestore.entity.MemberNM
import ru.sla.clarify.lib.google.firestore.entity.MergeRequestNM
import ru.sla.clarify.lib.google.firestore.entity.MergeRequestNM.Status
import ru.sla.clarify.lib.google.firestore.entity.UserNM
import ru.sla.clarify.lib.google.firestore.entity.write.CreateBranchParams
import ru.sla.clarify.lib.google.firestore.entity.write.CreateCommitInviteMemberParams
import ru.sla.clarify.lib.google.firestore.entity.write.CreateCommitParams
import ru.sla.clarify.lib.google.firestore.entity.write.CreateConversationParams
import ru.sla.clarify.lib.google.firestore.entity.write.CreateMergeParams
import ru.sla.clarify.lib.google.firestore.entity.write.CreateUserParams
import ru.sla.clarify.lib.google.firestore.entity.write.DeleteConversationMemberParams
import ru.sla.clarify.lib.google.firestore.entity.write.DeleteMergeRequestParams
import ru.sla.clarify.lib.google.firestore.entity.write.UpdateBranchLastCommitParams
import ru.sla.clarify.lib.google.firestore.entity.write.UpdateConversationMembersParams
import ru.sla.clarify.lib.google.firestore.entity.write.UpdateConversationNameParams
import ru.sla.clarify.lib.google.firestore.entity.write.UpdateGroupLastCommitParams
import ru.sla.clarify.lib.google.firestore.entity.write.UpdateIncrementParams
import ru.sla.clarify.lib.google.firestore.entity.write.UpdateMergeApprovalParams
import ru.sla.clarify.lib.google.firestore.entity.write.UpdateMergeFinalizeParams
import ru.sla.clarify.lib.google.firestore.entity.write.UpdateReadWatermarkParams
import ru.sla.clarify.lib.google.firestore.entity.write.UpdateUnreadCountParams
import ru.sla.clarify.lib.google.firestore.entity.write.UpdateUserParams
import ru.sla.clarify.lib.google.firestore.mapper.mapDocumentChanges
import java.time.LocalDateTime
import javax.inject.Inject

@Suppress("TooManyFunctions", "LargeClass")
@SingleIn(AppScope::class)
class Firestore @Inject constructor(
  firestoreWrapper: FirestoreWrapper,
  private val authSessionPersistence: AuthSessionPersistence,
  private val listenerGuard: FirestoreListenerGuard
) : FirestoreWrapperProvider by firestoreWrapper {

  suspend fun readCurrentUser(): UserNM {
    val userId = requireUserId()
    val document = userDocumentRef(userId)
      .get()
      .await()
    if (!document.exists()) {
      error("User by id: ${userId.value} not found in Firestore")
    }
    return codec.decodeFromSnapshot(document)
  }

  suspend fun readUser(id: UserId): UserNM? {
    val document = userDocumentRef(id)
      .get()
      .await()
    if (!document.exists()) {
      return null
    }
    return codec.decodeFromSnapshot<UserNM>(document)
  }

  suspend fun readUserExists(id: UserId): Boolean {
    return userDocumentRef(id)
      .get()
      .await()
      .exists()
  }

  suspend fun readUserExistsByEmail(email: Email): Boolean {
    return !usersQuery(whereEqualTo = email.value.lowercase())
      .limit(1)
      .get()
      .await()
      .isEmpty
  }

  suspend fun readUserIdByEmail(email: Email): UserId? {
    val querySnapshot = usersQuery(whereEqualTo = email.value.lowercase())
      .limit(1)
      .get()
      .await()
    return querySnapshot.documents.firstOrNull()
      ?.id
      ?.let(::UserId)
  }

  suspend fun readUsersByEmailPrefix(prefix: String, limit: Long): List<UserNM> {
    return usersQueryByEmailPrefix(prefix = prefix, limit = limit)
      .get()
      .await()
      .documents
      .map { codec.decodeFromSnapshot<UserNM>(it) }
  }

  fun userLive(id: UserId): Flow<UserNM?> = callbackFlow {
    listenerGuard.trackOpen("userLive:${id.value}")

    val listener = userDocumentRef(
      userId = id
    ).addSnapshotListener { snapshot, error ->
      if (error != null) {
        close(error)
        return@addSnapshotListener
      }
      val user = snapshot
        ?.takeIf { it.exists() }
        ?.let { codec.decodeFromSnapshot<UserNM>(it) }
      trySend(user)
    }
    awaitClose { listener.remove() }
  }

  suspend fun updateUser(
    id: UserId,
    email: String,
    displayName: String,
    photoUrl: String?
  ) {
    val userDocument = userDocumentRef(
      userId = id
    )

    val updateUserParams = UpdateUserParams(
      displayName = displayName,
      photoUrl = photoUrl,
      email = email
    )

    userDocument
      .set(codec.encodeToMap(updateUserParams), SetOptions.merge())
      .await()
  }

  suspend fun createUser(
    id: UserId,
    email: String,
    displayName: String,
    photoUrl: String?
  ) {
    val createUserParams = CreateUserParams(
      displayName = displayName,
      photoUrl = photoUrl,
      email = email
    )

    userDocumentRef(id)
      .set(codec.encodeToMap(createUserParams))
      .await()
  }

  suspend fun deleteConversations(ids: List<String>) {
    val conversationCollections = conversationCollectionRef()

    val batch = writeBatch()

    ids.forEach { id -> batch.delete(conversationCollections.document(id)) }
    batch.commit().await()
  }

  suspend fun deleteConversation(conversationId: String) {
    conversationDocumentRef(conversationId)
      .delete()
      .await()
  }

  fun conversationsLive(): Flow<List<FirestoreChange<ConversationNM>>> = callbackFlow {
    val userId = requireUserId()

    val listener = conversationsQuery(
      whereArrayContains = userId
    ).addSnapshotListener { snapshot, error ->
      if (error != null) {
        close(error)
        return@addSnapshotListener
      }
      trySend(snapshot.mapDocumentChanges<ConversationNM>())
    }
    awaitClose { listener.remove() }
  }

  suspend fun createGroupConversation(name: GroupName): String {
    val ownerId = requireUserId()
    val conversationId = randomUuid()

    val conversationRef = conversationDocumentRef(conversationId)

    val createConversationParams = CreateConversationParams(
      type = ConversationNM.Type.Group,
      memberUids = listOf(ownerId.value),
      name = name.value,
      ownerUid = ownerId.value
    )

    val batch = writeBatch()

    batch.set(
      conversationRef,
      codec.encodeToMap(createConversationParams)
    )
    batch.set(
      memberDocumentRef(conversationId, ownerId),
      emptyMap
    )
    batch.commit().await()

    return conversationId
  }

  suspend fun updateConversationName(conversationId: String, name: String) {
    val conversationDocument = conversationDocumentRef(conversationId)

    val updateConversationNameParams = UpdateConversationNameParams(
      name = name
    )

    val batch = writeBatch()

    batch.set(
      conversationDocument,
      codec.encodeToMap(updateConversationNameParams),
      SetOptions.merge()
    )
    batch.commit().await()
  }

  suspend fun deleteConversationMember(conversationId: String) {
    deleteConversationMember(
      conversationId = conversationId,
      userId = requireUserId()
    )
  }

  suspend fun deleteConversationMember(conversationId: String, userId: UserId) {
    val conversationDocument = conversationDocumentRef(conversationId)
    val memberDocument = memberDocumentRef(conversationId, userId)
    val unreadCommitsDocument = unreadCommitsDocumentRef(conversationId, userId)

    val batch = writeBatch()

    val deleteConversationMemberParams = DeleteConversationMemberParams(
      memberUids = ArrayRemove(listOf(userId.value))
    )

    batch.update(
      conversationDocument,
      codec.encodeToMap(deleteConversationMemberParams)
    )
    batch.delete(memberDocument)
    batch.delete(unreadCommitsDocument)
    batch.commit().await()
  }

  fun membersLive(
    conversationId: String
  ): Flow<List<FirestoreChange<MemberNM>>> = callbackFlow {
    listenerGuard.trackOpen("membersLive:$conversationId")

    val listener = membersCollectionRef(
      conversationId = conversationId
    ).addSnapshotListener { snapshot, error ->
      if (error != null) {
        close(error)
        return@addSnapshotListener
      }
      trySend(snapshot.mapDocumentChanges<MemberNM>())
    }
    awaitClose { listener.remove() }
  }

  fun memberLive(
    conversationId: String,
    userId: UserId
  ): Flow<MemberNM?> = callbackFlow {
    listenerGuard.trackOpen("memberLive:$conversationId:${userId.value}")

    val listener = memberDocumentRef(
      conversationId = conversationId,
      userId = userId
    ).addSnapshotListener { snapshot, error ->
      if (error != null) {
        close(error)
        return@addSnapshotListener
      }
      val member = snapshot
        ?.takeIf { it.exists() }
        ?.let { codec.decodeFromSnapshot<MemberNM>(it) }
      trySend(member)
    }
    awaitClose { listener.remove() }
  }

  suspend fun updateReadWatermark(
    conversationId: String,
    lastReadAt: LocalDateTime
  ) {
    val currentUserId = requireUserId()

    val memberDocument = memberDocumentRef(
      conversationId = conversationId,
      userId = currentUserId
    )

    val updateReadWatermarkParams = UpdateReadWatermarkParams(
      lastReadAt = lastReadAt.toTimestamp()
    )

    val batch = writeBatch()

    batch.set(
      memberDocument,
      codec.encodeToMap(updateReadWatermarkParams),
      SetOptions.merge()
    )
    batch.commit().await()
  }

  suspend fun createCommitInviteMember(conversationId: String, invitedUserId: UserId) {
    val commitId = randomUuid()
    val createdAt = Timestamp.now()
    val currentUserId = requireUserId()

    val conversationDocument = conversationDocumentRef(conversationId)
    val commitDocument = conversationDocument
      .collection(FirestoreSchema.COMMITS_COLLECTION)
      .document(commitId)

    val createCommitInviteMemberParams = CreateCommitInviteMemberParams(
      senderUid = currentUserId,
      invitedUid = invitedUserId.value,
      branchId = conversationId,
      createdAt = createdAt
    )
    val updateConversationMembersParams = UpdateConversationMembersParams(
      memberUids = ArrayUnion(listOf(invitedUserId.value))
    )

    val batch = writeBatch()

    batch.update(
      conversationDocument,
      codec.encodeToMap(updateConversationMembersParams)
    )
    batch.set(
      memberDocumentRef(conversationId, invitedUserId),
      emptyMap
    )
    batch.set(
      commitDocument,
      codec.encodeToMap(createCommitInviteMemberParams)
    )
    batch.commit().await()
  }

  fun commitsLive(
    conversationId: String,
    branchId: String,
    limit: Long
  ): Flow<List<FirestoreChange<CommitNM>>> = callbackFlow {
    listenerGuard.trackOpen("commitsLive:$conversationId:$branchId")

    val listener = commitQuery(
      conversationId = conversationId,
      whereEqualTo = Branch.Id(branchId),
      before = null,
      limit = limit
    ).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
      if (error != null) {
        close(error)
        return@addSnapshotListener
      }
      val result = snapshot.mapDocumentChanges<CommitNM>(
        metadataChanges = MetadataChanges.INCLUDE,
        trackPendingWrites = true
      )
      trySend(result)
    }
    awaitClose { listener.remove() }
  }

  suspend fun readCommits(
    conversationId: String,
    branchId: String,
    limit: Long,
    before: LocalDateTime?
  ): List<CommitNM> {
    val query = commitQuery(
      conversationId = conversationId,
      whereEqualTo = Branch.Id(branchId),
      before = before?.toTimestamp(),
      limit = limit
    )
    return query
      .get()
      .await()
      .documents
      .map { codec.decodeFromSnapshot<CommitNM>(it) }
  }

  fun directCommitsLive(
    branchId: String,
    peerId: Peer.Id,
    limit: Long
  ): Flow<List<FirestoreChange<CommitNM>>> {
    return directConversationIdLive(peerId)
      .distinctUntilChanged()
      .flatMapLatest { conversationId ->
        if (conversationId == null) {
          flowOf(emptyList())
        } else {
          commitsLive(
            conversationId = conversationId,
            branchId = branchId,
            limit = limit
          )
        }
      }
  }

  fun groupCommitsLive(
    conversationId: String,
    limit: Long
  ): Flow<List<FirestoreChange<CommitNM>>> {
    return commitsLive(
      conversationId = conversationId,
      branchId = conversationId,
      limit = limit
    )
  }

  suspend fun createBranchCommit(
    conversationId: String,
    branchId: String,
    text: String,
    memberUids: List<String>
  ) {
    val commitId = randomUuid()
    val createdAt = Timestamp.now()
    val currentUserId = requireUserId()

    val conversationDocument = conversationDocumentRef(conversationId)
    val commitDocument = conversationDocument
      .collection(FirestoreSchema.COMMITS_COLLECTION)
      .document(commitId)

    val createCommitParams = CreateCommitParams(
      senderUid = currentUserId,
      text = text,
      type = CommitNM.Type.Text,
      createdAt = createdAt,
      branchId = branchId
    )
    val updateBranchLastCommitParams = UpdateBranchLastCommitParams(
      lastCommitText = text,
      lastCommitAt = createdAt
    )
    val updateIncrementParams = UpdateIncrementParams(
      count = Increment(1)
    )

    val batch = writeBatch()

    batch.set(
      commitDocument,
      codec.encodeToMap(createCommitParams)
    )
    batch.set(
      branchDocumentRef(conversationId, branchId),
      codec.encodeToMap(updateBranchLastCommitParams),
      SetOptions.merge()
    )
    memberUids
      .filter { it != currentUserId.value }
      .forEach { uid ->
        batch.set(
          branchUnreadCommitsDocumentRef(conversationId, branchId, UserId(uid)),
          codec.encodeToMap(updateIncrementParams),
          SetOptions.merge()
        )
      }
    batch.commit().await()
  }

  suspend fun createDirectCommit(
    peerId: Peer.Id,
    branchId: String?,
    conversationId: String?,
    text: String
  ) {
    val commitId = randomUuid()
    val createdAt = Timestamp.now()
    val currentUserId = requireUserId()
    val directMemberIds = directMemberIds(currentUserId, peerId)

    val resolvedConversationId = conversationId ?: randomUuid()
    val resolvedBranchId = branchId ?: resolvedConversationId
    val isRoot = resolvedBranchId == resolvedConversationId

    val conversationDocument = conversationDocumentRef(resolvedConversationId)
    val commitDocument = conversationDocument
      .collection(FirestoreSchema.COMMITS_COLLECTION)
      .document(commitId)

    val createCommitParams = CreateCommitParams(
      senderUid = currentUserId,
      text = text,
      type = CommitNM.Type.Text,
      createdAt = createdAt,
      branchId = resolvedBranchId
    )
    val createConversationParams = CreateConversationParams(
      type = ConversationNM.Type.Direct,
      memberUids = directMemberIds,
      lastCommitText = text.takeIf { isRoot },
      lastCommitSenderUid = currentUserId.value.takeIf { isRoot },
      lastCommitAt = createdAt.takeIf { isRoot }
    )
    val updateBranchLastCommitParams = UpdateBranchLastCommitParams(
      lastCommitText = text,
      lastCommitAt = createdAt
    )
    val updateIncrementParams = UpdateIncrementParams(
      count = Increment(1)
    )

    val batch = writeBatch()

    batch.set(
      conversationDocument,
      codec.encodeToMap(createConversationParams),
      SetOptions.merge()
    )
    batch.set(
      commitDocument,
      codec.encodeToMap(createCommitParams)
    )
    if (conversationId == null) {
      directMemberIds.forEach { uid ->
        batch.set(
          memberDocumentRef(resolvedConversationId, UserId(uid)),
          emptyMap
        )
      }
    }
    if (isRoot) {
      directMemberIds
        .filter { it != currentUserId.value }
        .forEach { peerId ->
          batch.set(
            unreadCommitsDocumentRef(resolvedConversationId, UserId(peerId)),
            codec.encodeToMap(updateIncrementParams),
            SetOptions.merge()
          )
        }
    } else {
      batch.set(
        branchDocumentRef(resolvedConversationId, resolvedBranchId),
        codec.encodeToMap(updateBranchLastCommitParams),
        SetOptions.merge()
      )
      directMemberIds
        .filter { it != currentUserId.value }
        .forEach { peerId ->
          batch.set(
            branchUnreadCommitsDocumentRef(
              resolvedConversationId,
              resolvedBranchId,
              UserId(peerId)
            ),
            codec.encodeToMap(updateIncrementParams),
            SetOptions.merge()
          )
        }
    }
    batch.commit().await()
  }

  suspend fun createGroupCommit(
    conversationId: String,
    text: String,
    memberUids: List<String>
  ) {
    val commitId = randomUuid()
    val createdAt = Timestamp.now()
    val currentUserId = requireUserId()

    val conversationDocument = conversationDocumentRef(conversationId)
    val commitDocument = conversationDocument
      .collection(FirestoreSchema.COMMITS_COLLECTION)
      .document(commitId)

    val createCommitParams = CreateCommitParams(
      branchId = conversationId,
      senderUid = currentUserId,
      text = text,
      type = CommitNM.Type.Text,
      createdAt = createdAt
    )
    val updateGroupLastCommitParams = UpdateGroupLastCommitParams(
      lastCommitText = text,
      lastCommitSenderUid = currentUserId.value,
      lastCommitAt = createdAt
    )
    val updateIncrementParams = UpdateIncrementParams(
      count = Increment(1)
    )

    val batch = writeBatch()

    batch.set(
      commitDocument,
      codec.encodeToMap(createCommitParams)
    )
    batch.set(
      conversationDocument,
      codec.encodeToMap(updateGroupLastCommitParams),
      SetOptions.merge()
    )
    memberUids
      .filter { it != currentUserId.value }
      .forEach { uid ->
        batch.set(
          unreadCommitsDocumentRef(conversationId, UserId(uid)),
          codec.encodeToMap(updateIncrementParams),
          SetOptions.merge()
        )
      }

    batch.commit().await()
  }

  fun branchUnreadCountLive(
    conversationId: String,
    branchId: String
  ): Flow<Long> = callbackFlow {
    val userId = requireUserId()

    val listener = branchUnreadCommitsDocumentRef(
      conversationId = conversationId,
      branchId = branchId,
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

  fun unreadCountLive(conversationId: String): Flow<Long> = callbackFlow {
    val userId = requireUserId()

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

  suspend fun updateBranchUnreadCount(conversationId: String, branchId: String) {
    val userId = requireUserId()

    val branchUnreadCommitsDocument = branchUnreadCommitsDocumentRef(
      conversationId = conversationId,
      branchId = branchId,
      userId = userId
    )

    val updateUnreadCountParams = UpdateUnreadCountParams(
      count = 0L
    )
    branchUnreadCommitsDocument
      .set(codec.encodeToMap(updateUnreadCountParams), SetOptions.merge())
      .await()
  }

  suspend fun updateUnreadCount(conversationId: String) {
    val userId = requireUserId()

    val unreadCommitsDocument = unreadCommitsDocumentRef(
      conversationId = conversationId,
      userId = userId
    )

    val updateUnreadCountParams = UpdateUnreadCountParams(
      count = 0L
    )
    unreadCommitsDocument
      .set(codec.encodeToMap(updateUnreadCountParams), SetOptions.merge())
      .await()
  }

  fun branchesLive(
    conversationId: String
  ): Flow<List<FirestoreChange<BranchNM>>> = callbackFlow {
    listenerGuard.trackOpen("branchesLive:$conversationId")

    val listener = branchesCollectionRef(
      conversationId = conversationId
    ).addSnapshotListener { snapshot, error ->
      if (error != null) {
        close(error)
        return@addSnapshotListener
      }
      trySend(snapshot.mapDocumentChanges<BranchNM>())
    }
    awaitClose { listener.remove() }
  }

  suspend fun createBranch(
    conversationId: String,
    parentBranchId: String,
    branchedFromCommitId: String,
    name: String
  ): BranchNM {
    val branchId = randomUuid()
    val createdAt = Timestamp.now()
    val currentUserId = requireUserId()

    val branchDocument = branchDocumentRef(
      branchId = branchId,
      conversationId = conversationId
    )

    val createBranchParams = CreateBranchParams(
      parentBranchId = parentBranchId,
      branchedFromCommitId = branchedFromCommitId,
      name = name,
      createdAt = createdAt,
      createdByUid = currentUserId
    )
    branchDocument
      .set(codec.encodeToMap(createBranchParams))
      .await()

    return BranchNM(
      id = branchId,
      parentBranchId = parentBranchId,
      branchedFromCommitId = branchedFromCommitId,
      name = name,
      createdAt = createdAt,
      lastCommitAt = null,
      createdByUid = currentUserId.value,
      mergeRequest = null
    )
  }

  suspend fun createOpenMergeRequest(conversationId: String, branchId: String) {
    val initiatorId = requireUserId()
    val requestedAt = Timestamp.now()

    val branchDocument = branchDocumentRef(
      branchId = branchId,
      conversationId = conversationId
    )

    val transaction = runTransaction { transaction ->
      val branch = codec.decodeFromSnapshot<BranchNM>(transaction[branchDocument])
      val mergeRequest = branch.mergeRequest

      check(mergeRequest == null) {
        "Cannot open merge request: there is already an active one"
      }

      val createMergeParams = CreateMergeParams(
        mergeRequest = MergeRequestNM(
          status = Status.Open,
          initiatorUid = initiatorId.value,
          requestedAt = requestedAt,
          approvedByUids = emptyList()
        )
      )
      transaction.update(
        branchDocument,
        codec.encodeToMap(createMergeParams)
      )
    }
    transaction.await()
  }

  suspend fun updateMergeApproval(
    conversationId: String,
    branchId: String,
    memberUids: List<String>
  ) {
    val approver = requireUserId()

    val branchDocument = branchDocumentRef(
      branchId = branchId,
      conversationId = conversationId
    )

    val transaction = runTransaction { transaction ->
      val branch = codec.decodeFromSnapshot<BranchNM>(transaction[branchDocument])
      val mergeRequest = branch.mergeRequest

      checkNotNull(mergeRequest) {
        "Cannot approve merge: no active merge request"
      }
      check(mergeRequest.status == Status.Open || mergeRequest.status == Status.ReadyToMerge) {
        "Cannot approve merge: status is ${mergeRequest.status}"
      }

      val updatedApproved = (mergeRequest.approvedByUids + approver.value).distinct()

      val ready = memberUids.isNotEmpty() && memberUids.toSet()
        .subtract(updatedApproved.toSet())
        .isEmpty()

      val updateMergeApprovalParams = UpdateMergeApprovalParams(
        approvedByUids = updatedApproved,
        status = if (ready) Status.ReadyToMerge else Status.Open
      )
      transaction.update(
        branchDocument,
        codec.encodeToMap(updateMergeApprovalParams)
      )
    }

    transaction.await()
  }

  suspend fun updateMergeFinalize(conversationId: String, branchId: String) {
    val now = Timestamp.now()

    val branchDocument = branchDocumentRef(
      branchId = branchId,
      conversationId = conversationId
    )

    val transaction = runTransaction { transaction ->
      val branch = codec.decodeFromSnapshot<BranchNM>(transaction[branchDocument])
      val mergeRequest = branch.mergeRequest

      checkNotNull(mergeRequest) {
        "Cannot finalize merge: no active merge request"
      }
      check(mergeRequest.status == Status.ReadyToMerge) {
        "Cannot finalize merge: status is ${mergeRequest.status}"
      }

      val updateMergeFinalizeParams = UpdateMergeFinalizeParams(
        mergedAt = now,
        mergedIntoBranchId = branch.parentBranchId,
        status = Status.Merged
      )
      transaction.update(
        branchDocument,
        codec.encodeToMap(updateMergeFinalizeParams)
      )
    }
    transaction.await()
  }

  suspend fun deleteMergeRequest(conversationId: String, branchId: String) {
    val branchDocument = branchDocumentRef(
      branchId = branchId,
      conversationId = conversationId
    )

    val transaction = runTransaction { transaction ->
      val branch = codec.decodeFromSnapshot<BranchNM>(transaction[branchDocument])
      val mergeRequest = branch.mergeRequest

      checkNotNull(mergeRequest) {
        "Cannot cancel merge: no active merge request"
      }
      check(mergeRequest.status == Status.Open || mergeRequest.status == Status.ReadyToMerge) {
        "Cannot cancel merge: status is ${mergeRequest.status}"
      }

      val deleteMergeRequestParams = DeleteMergeRequestParams(
        mergeRequest = Delete
      )
      transaction.update(
        branchDocument,
        codec.encodeToMap(deleteMergeRequestParams)
      )
    }
    transaction.await()
  }

  suspend fun deleteMergeRequestApproval(conversationId: String, branchId: String) {
    val approver = requireUserId()

    val branchDocument = branchDocumentRef(
      branchId = branchId,
      conversationId = conversationId
    )

    val transaction = runTransaction { transaction ->
      val branch = codec.decodeFromSnapshot<BranchNM>(transaction[branchDocument])
      val mergeRequest = branch.mergeRequest

      checkNotNull(mergeRequest) {
        "Cannot revoke approval: no active merge request"
      }
      check(mergeRequest.status == Status.Open || mergeRequest.status == Status.ReadyToMerge) {
        "Cannot revoke approval: status is ${mergeRequest.status}"
      }

      val updateMergeApprovalParams = UpdateMergeApprovalParams(
        status = Status.Open,
        approvedByUids = mergeRequest.approvedByUids.filter { it != approver.value }
      )
      transaction.update(
        branchDocument,
        codec.encodeToMap(updateMergeApprovalParams)
      )
    }
    transaction.await()
  }

  private fun directConversationIdLive(peerId: Peer.Id): Flow<String?> = callbackFlow {
    val currentUserId = requireUserId()
    listenerGuard.trackOpen("directConversationIdLive:${currentUserId.value}:${peerId.value}")

    val directMemberIds = directMemberIds(currentUserId, peerId)

    val listener = conversationsQuery(
      whereEqualTo = ConversationNM.Type.Direct,
      whereArrayContains = currentUserId
    ).addSnapshotListener { snapshot, error ->
      if (error != null) {
        close(error)
        return@addSnapshotListener
      }
      trySend(snapshot?.findDirectConversationId(directMemberIds))
    }
    awaitClose { listener.remove() }
  }

  private fun directMemberIds(currentId: UserId, peerId: Peer.Id): List<String> {
    return setOf(currentId.value, peerId.value).sorted()
  }

  private fun QuerySnapshot?.findDirectConversationId(memberIds: List<String>): String? {
    return this
      ?.documents
      ?.firstOrNull { codec.decodeFromSnapshot<ConversationNM>(it).memberUids == memberIds }
      ?.id
  }

  private suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) }) {
      "Current user not found in persistence"
    }
  }
}
