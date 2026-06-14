@file:Suppress("IgnoredReturnValue", "RedundantSuspendModifier")

package ru.sla.clarify.lib.google.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
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
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM
import ru.sla.clarify.lib.google.firestore.entity.MemberNM
import ru.sla.clarify.lib.google.firestore.entity.MergeRequestNM
import ru.sla.clarify.lib.google.firestore.entity.UserNM
import ru.sla.clarify.lib.google.firestore.entity.write.PatchBranchLastCommitParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchBranchOpenMergeParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchConversationLastCommitParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchConversationNameParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchReadWatermarkParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchUnreadCountParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchUnreadIncrementParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchUserParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostBranchParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostCommitParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostConversationParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostInviteMemberCommitParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostMemberParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostUserParams
import ru.sla.clarify.lib.google.firestore.mapper.mapDocumentChanges
import ru.sla.clarify.lib.google.firestore.mapper.toFirestoreDocumentResult
import java.time.LocalDateTime
import javax.inject.Inject

@Suppress("TooManyFunctions", "LargeClass")
@SingleIn(AppScope::class)
class Firestore @Inject constructor(
  firestoreWrapper: FirestoreWrapper,
  private val authSessionPersistence: AuthSessionPersistence,
  private val listenerGuard: FirestoreListenerGuard
) : FirestoreWrapperProvider by firestoreWrapper {

  private val codec: FirestoreFormat = FirestoreFormat.Default

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

  suspend fun getUser(id: UserId): UserNM? {
    val document = userDocumentRef(id)
      .get()
      .await()
    if (!document.exists()) return null
    return codec.decodeFromSnapshot<UserNM>(document)
  }

  suspend fun getUserExists(id: UserId): Boolean {
    return userDocumentRef(id)
      .get()
      .await()
      .exists()
  }

  suspend fun getUserExistsByEmail(email: Email): Boolean {
    return !usersQuery(whereEqualTo = email.value.lowercase())
      .limit(1)
      .get()
      .await()
      .isEmpty
  }

  suspend fun getUserIdByEmail(email: Email): UserId? {
    val snapshot = usersQuery(whereEqualTo = email.value.lowercase())
      .limit(1)
      .get()
      .await()
    return snapshot.documents.firstOrNull()
      ?.id
      ?.let(::UserId)
  }

  suspend fun getUsersByEmailPrefix(prefix: String, limit: Long): List<UserNM> {
    return usersQueryByEmailPrefix(prefix = prefix, limit = limit)
      .get()
      .await()
      .documents
      .map { codec.decodeFromSnapshot<UserNM>(it) }
  }

  fun observeUser(id: UserId): Flow<UserNM?> {
    return callbackFlow {
      listenerGuard.trackOpen("observeUser:${id.value}")

      val listener = userDocumentRef(id).addSnapshotListener { snapshot, error ->
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
    }.flowOn(Dispatchers.IO)
  }

  suspend fun patchUser(
    id: UserId,
    email: String,
    displayName: String,
    photoUrl: String?
  ) {
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

  suspend fun postUser(
    id: UserId,
    email: String,
    displayName: String,
    photoUrl: String?
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

  suspend fun deleteConversations(ids: List<String>) {
    val batch = writeBatch()
    val reference = conversationCollectionRef()
    ids.forEach { id -> batch.delete(reference.document(id)) }
    batch.commit().await()
  }

  suspend fun deleteGroupConversation(conversationId: String) {
    conversationDocumentRef(conversationId)
      .delete()
      .await()
  }

  fun observeConversations(): Flow<List<FirestoreChange<ConversationNM>>> {
    return callbackFlow {
      val userId = requireUserId()
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

  suspend fun patchGroupName(conversationId: String, name: String) {
    conversationDocumentRef(conversationId)
      .set(
        codec.encodeToMap(PatchConversationNameParams(name = name)),
        SetOptions.merge()
      )
      .await()
  }

  suspend fun postGroupConversation(name: String): String {
    val ownerId = requireUserId()
    val conversationId = randomUuid()

    val batch = writeBatch()
    val conversationRef = conversationDocumentRef(conversationId)

    val postConversationParams = PostConversationParams(
      type = ConversationNM.Type.Group,
      memberUids = listOf(ownerId.value),
      name = name,
      ownerUid = ownerId.value
    )
    val postMemberParams = PostMemberParams(
      id = ownerId.value
    )
    batch.set(
      conversationRef,
      codec.encodeToMap(postConversationParams)
    )
    batch.set(
      memberDocumentRef(conversationId, ownerId),
      codec.encodeToMap(postMemberParams)
    )
    batch.commit().await()
    return conversationId
  }

  suspend fun deleteMember(conversationId: String, userId: UserId) {
    val batch = writeBatch()
    val conversationRef = conversationDocumentRef(conversationId)
    batch.update(
      conversationRef,
      FirestoreSchema.CONVERSATION_MEMBER_UIDS,
      FieldValue.arrayRemove(userId.value)
    )
    batch.delete(memberDocumentRef(conversationId, userId))
    batch.delete(unreadCommitsDocumentRef(conversationId, userId))
    batch.commit().await()
  }

  suspend fun leaveGroup(conversationId: String) {
    deleteMember(conversationId, requireUserId())
  }

  fun observeMember(
    conversationId: String,
    userId: UserId
  ): Flow<MemberNM?> {
    return callbackFlow {
      listenerGuard.trackOpen("observeMember:$conversationId:${userId.value}")

      val listener = memberDocumentRef(conversationId, userId)
        .addSnapshotListener { snapshot, error ->
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
  }

  fun observeMembers(conversationId: String): Flow<List<FirestoreChange<MemberNM>>> {
    return callbackFlow {
      listenerGuard.trackOpen("observeMembers:$conversationId")

      val listener = membersCollectionRef(conversationId)
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }
          val response = snapshot.mapDocumentChanges { change ->
            FirestoreChange(
              changeType = change.type.toFirestoreDocumentResult(),
              data = codec.decodeFromSnapshot<MemberNM>(change.document)
            )
          }
          trySend(response)
        }

      awaitClose { listener.remove() }
    }
  }

  suspend fun patchReadWatermark(
    conversationId: String,
    lastReadAt: LocalDateTime
  ) {
    val userId = requireUserId()
    memberDocumentRef(conversationId, userId)
      .set(
        codec.encodeToMap(PatchReadWatermarkParams(lastReadAt = lastReadAt.toTimestamp())),
        SetOptions.merge()
      )
      .await()
  }

  suspend fun postInviteMember(conversationId: String, invitedUserId: UserId) {
    val senderId = requireUserId()
    val createdAt = Timestamp.now()
    val commitId = randomUuid()

    val batch = writeBatch()
    val conversationRef = conversationDocumentRef(conversationId)
    batch.update(
      conversationRef,
      FirestoreSchema.CONVERSATION_MEMBER_UIDS,
      FieldValue.arrayUnion(invitedUserId.value)
    )
    batch.set(
      memberDocumentRef(conversationId, invitedUserId),
      codec.encodeToMap(PostMemberParams(id = invitedUserId.value))
    )
    batch.set(
      conversationRef
        .collection(FirestoreSchema.COMMITS_COLLECTION)
        .document(commitId),
      codec.encodeToMap(
        PostInviteMemberCommitParams(
          clientCommitId = commitId,
          senderUid = senderId,
          invitedUid = invitedUserId.value,
          branchId = conversationId,
          createdAt = createdAt
        )
      )
    )
    batch.commit().await()
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

  fun observeCommits(
    conversationId: String,
    branchId: String,
    limit: Long
  ): Flow<List<FirestoreChange<CommitNM>>> {
    return conversationMessagesLive(
      id = conversationId,
      branchId = branchId,
      limit = limit
    )
  }

  fun observeDirectCommits(
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
      .flowOn(Dispatchers.IO)
  }

  fun observeGroupCommits(
    conversationId: String,
    limit: Long
  ): Flow<List<FirestoreChange<CommitNM>>> {
    return conversationMessagesLive(
      id = conversationId,
      branchId = conversationId,
      limit = limit
    )
  }

  suspend fun postBranchCommit(
    conversationId: String,
    branchId: String,
    text: String,
    colorHex: String,
    memberUids: List<String>
  ) {
    val senderId = requireUserId()
    val commitId = randomUuid()
    val createdAt = Timestamp.now()

    val batch = writeBatch()
    val conversationRef = conversationDocumentRef(conversationId)

    batch.set(
      conversationRef
        .collection(FirestoreSchema.COMMITS_COLLECTION)
        .document(commitId),
      codec.encodeToMap(
        PostCommitParams(
          clientCommitId = commitId,
          senderUid = senderId,
          text = text,
          type = CommitNM.Type.Text,
          createdAt = createdAt,
          colorHex = colorHex,
          branchId = branchId
        )
      )
    )
    batch.set(
      branchDocumentRef(conversationId, branchId),
      codec.encodeToMap(
        PatchBranchLastCommitParams(
          lastCommitText = text,
          lastCommitAt = createdAt
        )
      ),
      SetOptions.merge()
    )
    memberUids
      .filter { it != senderId.value }
      .forEach { uid ->
        batch.set(
          branchUnreadCommitsDocumentRef(conversationId, branchId, UserId(uid)),
          codec.encodeToMap(PatchUnreadIncrementParams()),
          SetOptions.merge()
        )
      }

    batch.commit().await()
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
    val resolvedBranchId = branchId ?: conversationId

    val commitId = randomUuid()
    val createdAt = Timestamp.now()
    val memberIds = directMemberIds(senderId, peerId)
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
        colorHex = colorHex,
        branchId = resolvedBranchId
      )
    )

    val conversationData = codec.encodeToMap(
      PostConversationParams(
        type = ConversationNM.Type.Direct,
        memberUids = memberIds,
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
      memberIds.forEach { uid ->
        batch.set(
          memberDocumentRef(conversationId, UserId(uid)),
          codec.encodeToMap(PostMemberParams(id = uid))
        )
      }
    }

    if (isRoot) {
      memberIds
        .filter { it != senderId.value }
        .forEach { peerId ->
          batch.set(
            unreadCommitsDocumentRef(conversationId, UserId(peerId)),
            codec.encodeToMap(PatchUnreadIncrementParams()),
            SetOptions.merge()
          )
        }
    } else {
      batch.set(
        branchDocumentRef(conversationId, resolvedBranchId),
        codec.encodeToMap(
          PatchBranchLastCommitParams(
            lastCommitText = text,
            lastCommitAt = createdAt
          )
        ),
        SetOptions.merge()
      )
      memberIds
        .filter { it != senderId.value }
        .forEach { peerId ->
          batch.set(
            branchUnreadCommitsDocumentRef(conversationId, resolvedBranchId, UserId(peerId)),
            codec.encodeToMap(PatchUnreadIncrementParams()),
            SetOptions.merge()
          )
        }
    }

    batch.commit().await()
  }

  suspend fun postGroupCommit(
    conversationId: String,
    text: String,
    colorHex: String,
    memberUids: List<String>
  ) {
    val senderId = requireUserId()
    val commitId = randomUuid()
    val createdAt = Timestamp.now()

    val batch = writeBatch()
    val conversationRef = conversationDocumentRef(conversationId)

    batch.set(
      conversationRef
        .collection(FirestoreSchema.COMMITS_COLLECTION)
        .document(commitId),
      codec.encodeToMap(
        PostCommitParams(
          clientCommitId = commitId,
          senderUid = senderId,
          text = text,
          type = CommitNM.Type.Text,
          createdAt = createdAt,
          colorHex = colorHex,
          branchId = conversationId
        )
      )
    )
    batch.set(
      conversationRef,
      codec.encodeToMap(
        PatchConversationLastCommitParams(
          lastCommitText = text,
          lastCommitSenderUid = senderId.value,
          lastCommitAt = createdAt
        )
      ),
      SetOptions.merge()
    )
    memberUids
      .filter { it != senderId.value }
      .forEach { uid ->
        batch.set(
          unreadCommitsDocumentRef(conversationId, UserId(uid)),
          codec.encodeToMap(PatchUnreadIncrementParams()),
          SetOptions.merge()
        )
      }

    batch.commit().await()
  }

  fun observeBranchUnreadCount(
    conversationId: String,
    branchId: String
  ): Flow<Long> {
    return callbackFlow {
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
  }

  fun observeUnreadCount(conversationId: String): Flow<Long> {
    return callbackFlow {
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
  }

  suspend fun patchBranchClearUnreadCount(
    conversationId: String,
    branchId: String
  ) {
    val userId = requireUserId()
    branchUnreadCommitsDocumentRef(conversationId, branchId, userId)
      .set(
        codec.encodeToMap(PatchUnreadCountParams(count = 0L)),
        SetOptions.merge()
      )
      .await()
  }

  suspend fun patchClearUnreadCount(conversationId: String) {
    val userId = requireUserId()
    unreadCommitsDocumentRef(conversationId, userId)
      .set(
        codec.encodeToMap(PatchUnreadCountParams(count = 0L)),
        SetOptions.merge()
      )
      .await()
  }

  fun observeBranches(
    conversationId: String
  ): Flow<List<FirestoreChange<BranchNM>>> {
    return callbackFlow {
      listenerGuard.trackOpen("observeBranches:$conversationId")

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
    }.flowOn(Dispatchers.IO)
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
      createdAt = createdAt,
      lastCommitAt = null,
      createdByUid = createdByUserId.value,
      mergeRequest = null
    )
  }

  suspend fun deleteMergeApproval(
    conversationId: String,
    branchId: String
  ) {
    val approver = requireUserId()
    val branchRef = branchDocumentRef(conversationId, branchId)
    val transaction = runTransaction { txn ->
      val current = codec.decodeFromSnapshot<BranchNM>(txn.get(branchRef))
      val mergeRequest = current.mergeRequest
        ?: error("Cannot revoke approval: no active merge request")
      check(
        mergeRequest.status == MergeRequestNM.Status.Open ||
          mergeRequest.status == MergeRequestNM.Status.ReadyToMerge
      ) {
        "Cannot revoke approval: status is ${mergeRequest.status}"
      }
      val updatedApproved = mergeRequest.approvedByUids.filter { it != approver.value }
      txn.update(
        branchRef,
        mapOf(
          "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_APPROVED_BY_UIDS}"
            to updatedApproved,
          "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_STATUS}"
            to MergeRequestNM.Status.Open.value
        )
      )
    }

    transaction.await()
  }

  suspend fun deleteMergeRequest(
    conversationId: String,
    branchId: String
  ) {
    val branchRef = branchDocumentRef(conversationId, branchId)
    val transaction = runTransaction { txn ->
      val current = codec.decodeFromSnapshot<BranchNM>(txn.get(branchRef))
      val mergeRequest = current.mergeRequest
        ?: error("Cannot cancel merge: no active merge request")
      check(
        mergeRequest.status == MergeRequestNM.Status.Open ||
          mergeRequest.status == MergeRequestNM.Status.ReadyToMerge
      ) {
        "Cannot cancel merge: status is ${mergeRequest.status}"
      }
      txn.update(
        branchRef,
        mapOf(FirestoreSchema.BRANCH_MERGE_REQUEST to FieldValue.delete())
      )
    }

    transaction.await()
  }

  suspend fun patchMergeApproval(
    conversationId: String,
    branchId: String,
    memberUids: List<String>
  ) {
    val approver = requireUserId()
    val branchRef = branchDocumentRef(conversationId, branchId)
    val transaction = runTransaction { txn ->
      val current = codec.decodeFromSnapshot<BranchNM>(txn.get(branchRef))
      val mergeRequest = current.mergeRequest
        ?: error("Cannot approve merge: no active merge request")
      check(
        mergeRequest.status == MergeRequestNM.Status.Open ||
          mergeRequest.status == MergeRequestNM.Status.ReadyToMerge
      ) {
        "Cannot approve merge: status is ${mergeRequest.status}"
      }

      val updatedApproved = (mergeRequest.approvedByUids + approver.value).distinct()

      val ready = memberUids.isNotEmpty() &&
        memberUids.toSet().subtract(updatedApproved.toSet()).isEmpty()

      txn.update(
        branchRef,
        mapOf(
          "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_APPROVED_BY_UIDS}"
            to updatedApproved,
          "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_STATUS}"
            to (if (ready) MergeRequestNM.Status.ReadyToMerge else MergeRequestNM.Status.Open).value
        )
      )
    }

    transaction.await()
  }

  suspend fun patchMergeFinalize(
    conversationId: String,
    branchId: String
  ) {
    val branchRef = branchDocumentRef(conversationId, branchId)
    val now = Timestamp.now()
    val transaction = runTransaction { txn ->
      val current = codec.decodeFromSnapshot<BranchNM>(txn.get(branchRef))
      val mergeRequest = current.mergeRequest
        ?: error("Cannot finalize merge: no active merge request")
      check(mergeRequest.status == MergeRequestNM.Status.ReadyToMerge) {
        "Cannot finalize merge: status is ${mergeRequest.status}"
      }
      txn.update(
        branchRef,
        mapOf(
          "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_STATUS}"
            to MergeRequestNM.Status.Merged.value,
          "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_MERGED_AT}"
            to now,
          "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_MERGED_INTO_BRANCH_ID}"
            to current.parentBranchId
        )
      )
    }

    transaction.await()
  }

  suspend fun postMergeRequest(
    conversationId: String,
    branchId: String
  ) {
    val initiator = requireUserId()
    val branchRef = branchDocumentRef(conversationId, branchId)
    val requestedAt = Timestamp.now()

    val transaction = runTransaction { txn ->
      val current = codec.decodeFromSnapshot<BranchNM>(txn.get(branchRef))
      check(current.mergeRequest == null) {
        "Cannot open merge request: there is already an active one"
      }
      val update = codec.encodeToMap(
        PatchBranchOpenMergeParams(
          mergeRequest = MergeRequestNM(
            status = MergeRequestNM.Status.Open,
            initiatorUid = initiator.value,
            requestedAt = requestedAt,
            approvedByUids = emptyList()
          )
        )
      )
      txn.update(branchRef, update)
    }

    transaction.await()
  }

  private fun directConversationIdLive(peerId: Peer.Id): Flow<String?> {
    return callbackFlow {
      val currentUid = requireUserId()
      listenerGuard.trackOpen("directConversationIdLive:${currentUid.value}:${peerId.value}")

      val directMemberIds = directMemberIds(currentUid, peerId)

      val listener = conversationsQuery(
        whereEqualTo = ConversationNM.Type.Direct,
        whereArrayContains = currentUid
      ).addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        trySend(snapshot?.findDirectConversationId(directMemberIds))
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
        .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }
          val response = snapshot.mapDocumentChanges(MetadataChanges.INCLUDE) { change ->
            FirestoreChange(
              changeType = change.type.toFirestoreDocumentResult(),
              data = codec.decodeFromSnapshot<CommitNM>(change.document),
              hasPendingWrites = change.document.metadata.hasPendingWrites()
            )
          }
          trySend(response)
        }

      awaitClose { listener.remove() }
    }
  }

  private fun directMemberIds(userId: UserId, peerId: Peer.Id): List<String> {
    return setOf(userId.value, peerId.value).sorted()
  }

  private fun QuerySnapshot?.findDirectConversationId(
    memberIds: List<String>
  ): String? {
    return this?.documents
      ?.firstOrNull { document ->
        codec.decodeFromSnapshot<ConversationNM>(document).memberUids == memberIds
      }
      ?.id
  }

  private suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) }) {
      "Current user not found in persistence"
    }
  }
}
