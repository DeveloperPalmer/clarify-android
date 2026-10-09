package ru.sla.clarify.feature.chat.branch.data

import androidx.room3.withWriteTransaction
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.chat.api.BranchApi
import ru.sla.clarify.chat.api.CommitApi
import ru.sla.clarify.chat.api.MemberApi
import ru.sla.clarify.chat.api.MergeRequestApi
import ru.sla.clarify.chat.api.UnreadCountApi
import ru.sla.clarify.core.domain.date.nowEpochNanos
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.ChatDatabase
import ru.sla.clarify.database.entity.ChatCommitEntity
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.BranchRecord
import ru.sla.clarify.entity.chat.ChatChange
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.CommitCursor
import ru.sla.clarify.entity.chat.CommitNotFoundException
import ru.sla.clarify.entity.chat.CommitRecord
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.branch.data.entity.DeleteForEveryoneWrite
import ru.sla.clarify.feature.chat.branch.data.entity.EditState
import ru.sla.clarify.feature.chat.branch.data.mapper.toDomainModel
import ru.sla.clarify.feature.chat.branch.domain.BranchRepository
import ru.sla.clarify.feature.chat.branch.domain.di.BranchScope
import ru.sla.clarify.feature.chat.branch.domain.entity.EditTargetNotFoundException
import ru.sla.clarify.feature.chat.branch.domain.entity.TargetParams
import ru.sla.clarify.mapper.data.lastCommitWriteAfterDeleting
import ru.sla.clarify.mapper.data.toCacheRow
import ru.sla.clarify.mapper.data.toDomainModel
import ru.sla.clarify.mapper.data.toReplyRecord
import ru.sla.clarify.mapper.data.unreadDelta
import ru.sla.clarify.mapper.data.withReadStatus
import ru.sla.log.log
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import java.time.LocalDateTime

@Suppress("TooManyFunctions")
@SingleIn(BranchScope::class)
@ContributesBinding(BranchScope::class)
class BranchRepositoryImpl @Inject constructor(
  params: TargetParams,
  private val branchApi: BranchApi,
  private val commitApi: CommitApi,
  private val memberApi: MemberApi,
  private val mergeRequestApi: MergeRequestApi,
  private val unreadCountApi: UnreadCountApi,
  private val chatDatabase: ChatDatabase,
  private val authSessionPersistence: AuthSessionPersistence
) : BranchRepository {

  private val branchId = params.branchId
  private val lastReadWatermark = MutableStateFlow<LocalDateTime?>(null)

  private val fetchCommitHistoryMutex = Mutex()
  private val hasCommitsHistoryCache = MutableStateFlow(true)
  private val fetchLatestCommitsCompletable = CompletableDeferred<Unit>()

  override suspend fun subscribeOnBranchChanges() {
    val conversationId = requireConversationId()
    branchApi.branchLive(
      conversationId = conversationId.value,
      branchId = branchId.value
    ).collect(::applyBranchChanges)
  }

  override suspend fun subscribeOnBranchCommitsChanges() {
    val userId = requireUserId()
    val conversationId = requireConversationId()
    // Ждём первую страницу, чтобы первое окно tail было [самый старый в кэше, +inf), а не вся
    // история: null-курсор потянул бы всё в кэш и свёл бы пагинацию на нет.
    fetchLatestCommitsCompletable.await()

    oldestCursor(conversationId).flatMapLatest { cursor ->
      commitApi.commitsLive(
        conversationId = conversationId.value,
        branchId = branchId.value,
        from = cursor
      )
    }.collect { changes ->
      applyCommitChanges(
        conversationId = conversationId,
        userId = userId,
        changes = changes
      )
    }
  }

  override suspend fun subscribeOnBranchUnreadCountChanges() {
    val conversationId = requireConversationId()
    unreadCountApi.branchUnreadCountLive(
      conversationId = conversationId.value,
      branchId = branchId.value
    ).collect { unreadCount ->
      applyUpdateUnreadCount(
        branchId = branchId,
        unreadCount = unreadCount
      )
    }
  }

  override suspend fun fetchLatestCommits() {
    val conversationId = requireConversationId()
    val latestCommits = commitApi.readCommits(
      conversationId = conversationId.value,
      branchId = branchId.value,
      limit = LATEST_PAGE_SIZE.toLong(),
      before = null
    )
    applyInsertOrReplaceCommits(
      conversationId = conversationId,
      commits = latestCommits
    )
    // Завершаем только после успешной загрузки: неудачная первичная выборка оставляет tail
    // заблокированным, чтобы повтор пере-вычислил курсор из наполненного кэша, а не тянул всю историю.
    fetchLatestCommitsCompletable.complete(Unit)
  }

  override suspend fun fetchCommitHistory() = fetchCommitHistoryMutex.withLock {
    if (!hasCommitsHistoryCache.value) {
      return@withLock
    }
    val conversationId = requireConversationId()
    val cursor = readOldestCursor(conversationId)

    val commitHistory = commitApi.readCommits(
      conversationId = conversationId.value,
      branchId = branchId.value,
      limit = HISTORY_PAGE_SIZE.toLong(),
      before = cursor,
      fromServerOnly = true
    )

    applyInsertOrReplaceCommits(
      conversationId = conversationId,
      commits = commitHistory
    )

    if (commitHistory.size < HISTORY_PAGE_SIZE) {
      hasCommitsHistoryCache.value = false
    }
  }

  override suspend fun sendCommit(text: String, replyCommit: Commit.Message?) {
    val conversationId = requireConversationId()
    commitApi.createBranchCommit(
      conversationId = conversationId.value,
      branchId = branchId.value,
      text = text,
      memberUids = memberUids(conversationId),
      replyCommit = replyCommit?.toReplyRecord()
    )
  }

  override suspend fun editCommit(id: Commit.Id, text: String) {
    val conversationId = requireConversationId()
    // Оптимистично: сразу показываем новый текст со статусом «в процессе» (часы). Прежнее
    // состояние держим для отката при ошибке записи.
    val previous = readEditState(id) ?: return
    applyEditStatus(
      id = id,
      text = text,
      status = Commit.Status.Sending
    )
    runSuspendCatching {
      commitApi.updateBranchCommit(
        conversationId = conversationId.value,
        branchId = branchId.value,
        commitId = id.value,
        text = text
      )
    }.onFailure { error ->
      if (error is CommitNotFoundException) {
        applyDeleteCommits(listOf(id))
        throw EditTargetNotFoundException(id)
      }
      applyRevertEdit(id, previous)
      throw error
    }
    applyEditStatus(
      id = id,
      text = text,
      status = Commit.Status.Sent
    )
  }

  @Suppress("TooGenericExceptionCaught")
  override suspend fun deleteCommits(
    ids: List<Commit.Id>,
    forEveryone: Boolean
  ) {
    val conversationId = requireConversationId()
    val removableCommits = readCachedCommits(ids)
    // Для «у всех» денормализованные lastCommit/unreadDelta считаем по ПОЛНОМУ кэшу до удаления —
    // после оптимистичного удаления они уже не увидели бы удаляемые коммиты.
    val forEveryoneWrite = if (forEveryone) buildDeleteForEveryoneWrite(ids) else null
    // Оптимистичное локальное удаление ДО записи: лента и выделение не ждут ответа сервера.
    // Живой tail-слушатель идёт вперёд от самого старого коммита, поэтому удаление обратно не «всплывёт».
    applyDeleteCommits(
      ids = ids
    )
    runSuspendCatching {
      if (forEveryoneWrite != null) {
        commitApi.deleteBranchCommits(
          conversationId = conversationId.value,
          branchId = branchId.value,
          peerId = forEveryoneWrite.peerId.value,
          commitIds = ids.map { it.value },
          lastCommit = forEveryoneWrite.lastCommit,
          peerUnreadDelta = forEveryoneWrite.peerUnreadDelta
        )
      } else {
        commitApi.hideCommits(
          conversationId = conversationId.value,
          commitIds = ids.map { it.value }
        )
      }
    }.onFailure { error ->
      restoreCommits(removableCommits)
      throw error
    }
  }

  private suspend fun buildDeleteForEveryoneWrite(ids: List<Commit.Id>): DeleteForEveryoneWrite {
    val peerId = branchPeerId()
    val deletedIds = ids.toSet()
    val conversationId = requireConversationId()

    val branchCommits = chatDatabase.chatCommitDao()
      .select(conversationId, branchId)
      .map { it.toDomainModel() }

    val peerLastReadAt = memberApi.readMember(
      conversationId = conversationId.value,
      memberId = peerId.value
    )

    return DeleteForEveryoneWrite(
      peerId = peerId,
      lastCommit = branchCommits.lastCommitWriteAfterDeleting(deletedIds),
      peerUnreadDelta = branchCommits.unreadDelta(deletedIds, peerLastReadAt)
    )
  }

  private suspend fun branchPeerId(): Peer.Id {
    val userId = requireUserId()
    val conversationId = requireConversationId()

    val memberId = chatDatabase.chatMemberDao()
      .selectDirect(conversationId)
      .firstOrNull { it.id.value != userId.value }
      ?.id

    if (memberId == null) {
      error("memberId is required for branch")
    }

    return Peer.Id(memberId.value)
  }

  override suspend fun markAsRead() {
    unreadCountApi.updateBranchUnreadCount(
      branchId = branchId.value,
      conversationId = requireConversationId().value
    )
  }

  override suspend fun markReadUpTo(lastReadAt: LocalDateTime) {
    val conversationId = requireConversationId()
    val current = lastReadWatermark.value
    if (current != null && !lastReadAt.isAfter(current)) {
      log { "Branch: lastReadAt ($lastReadAt) is not after current watermark ($current), skipping" }
      return
    }
    lastReadWatermark.value = lastReadAt
    memberApi.updateReadWatermark(
      conversationId = conversationId.value,
      lastReadAt = lastReadAt
    )
    unreadCountApi.updateBranchUnreadCount(
      branchId = branchId.value,
      conversationId = conversationId.value
    )
  }

  override suspend fun openMergeRequest() {
    mergeRequestApi.createOpenMergeRequest(
      branchId = branchId.value,
      conversationId = requireConversationId().value
    )
  }

  override suspend fun approveMergeRequest() {
    val conversationId = requireConversationId()
    mergeRequestApi.updateMergeApproval(
      branchId = branchId.value,
      conversationId = conversationId.value,
      memberUids = memberUids(conversationId)
    )
  }

  override suspend fun revokeMergeRequestApproval() {
    mergeRequestApi.deleteMergeRequestApproval(
      branchId = branchId.value,
      conversationId = requireConversationId().value
    )
  }

  override suspend fun cancelMergeRequest() {
    mergeRequestApi.deleteMergeRequest(
      branchId = branchId.value,
      conversationId = requireConversationId().value
    )
  }

  override suspend fun finalizeMergeRequest() {
    mergeRequestApi.updateMergeFinalize(
      branchId = branchId.value,
      conversationId = requireConversationId().value
    )
  }

  override val user: Flow<User?> = flow {
    val userId = authSessionPersistence.withKey { readUserId(it) }
    if (userId == null) return@flow emit(null)
    chatDatabase.userDao()
      .observe(userId)
      .map { it?.toDomainModel() }
      .collect { emit(it) }
  }

  override val branch: Flow<Branch?> = chatDatabase.chatBranchDao()
    .observeById(branchId)
    .map { it?.toDomainModel() }

  override val hasCommitsHistory: Flow<Boolean> = hasCommitsHistoryCache

  override val commits: Flow<List<Commit>> = flow {
    val conversationId = requireConversationId()
    val selfId = requireUserId()

    val commitsFlow = chatDatabase.chatCommitDao()
      .observe(conversationId, branchId)
      .map { rows -> rows.map { it.toDomainModel() } }

    val peerReadAtFlow = peerReadAt(conversationId, selfId)

    val result = combine(
      flow = commitsFlow,
      flow2 = peerReadAtFlow
    ) { commits, peerReadAt ->
      commits.map { it.withReadStatus(peerReadAt) }
    }

    emitAll(result)
  }

  override val unreadCount: Flow<Long> = flow {
    val conversationId = requireConversationId()
    unreadCountApi.branchUnreadCountLive(
      branchId = branchId.value,
      conversationId = conversationId.value
    ).collect { emit(it) }
  }

  override val members: Flow<List<Member>> = flow {
    val conversationId = requireConversationId()
    chatDatabase.chatMemberDao()
      .observeDirect(conversationId)
      .map { rows -> rows.map { it.toDomainModel() } }
      .collect { emit(it) }
  }

  override fun member(id: UserId): Flow<Member?> = flow {
    val memberId = Member.Id(id.value)
    val conversationId = requireConversationId()
    chatDatabase.chatMemberDao()
      .observeDirectById(conversationId, memberId)
      .map { it?.toDomainModel() }
      .collect { emit(it) }
  }

  private fun peerReadAt(conversationId: Conversation.Id, selfId: UserId): Flow<LocalDateTime?> = flow {
    val member = chatDatabase.chatMemberDao()
      .selectDirect(conversationId)
      .firstOrNull { it.id.value != selfId.value }
    if (member == null) {
      emit(null)
    } else {
      memberApi.memberLive(
        conversationId = conversationId.value,
        memberId = member.id.value
      ).collect {
        emit(it)
      }
    }
  }

  private suspend fun memberUids(conversationId: Conversation.Id): List<String> {
    return chatDatabase.chatMemberDao()
      .selectIds(conversationId)
      .map { it.value }
  }

  private suspend fun applyBranchChanges(branch: BranchRecord?) {
    chatDatabase.withWriteTransaction {
      if (branch == null) {
        chatDatabase.chatBranchDao().delete(branchId)
      } else {
        applyInsertOrReplaceBranch(branch)
      }
    }
  }

  private suspend fun applyInsertOrReplaceBranch(branch: BranchRecord) {
    chatDatabase.chatBranchDao().upsert(
      id = branch.id,
      conversationId = branch.conversationId,
      parentBranchId = branch.parentBranchId,
      branchedFromCommitId = branch.branchedFromCommitId,
      name = branch.name,
      lastCommit = branch.lastCommitText,
      lastCommitTimestamp = branch.lastCommitAtSeconds,
      createdAt = branch.createdAtSeconds,
      createdById = branch.createdById
    )
    val mergeRequest = branch.mergeRequest
    if (mergeRequest != null) {
      chatDatabase.mergeRequestDao().insertOrReplace(mergeRequest.toCacheRow(branch.id))
    } else {
      chatDatabase.mergeRequestDao().delete(branch.id)
    }
  }

  private suspend fun applyUpdateUnreadCount(branchId: Branch.Id, unreadCount: Long) {
    chatDatabase.chatBranchDao().updateUnreadCount(
      id = branchId,
      unreadCount = unreadCount
    )
  }

  private suspend fun applyInsertOrReplaceCommits(conversationId: Conversation.Id, commits: List<CommitRecord>) {
    val userId = requireUserId()
    chatDatabase.withWriteTransaction {
      commits.forEach { item ->
        applyInsertOrReplaceCommit(
          conversationId = conversationId,
          commit = item,
          userId = userId,
          isPending = false
        )
      }
    }
  }

  private suspend fun applyCommitChanges(
    conversationId: Conversation.Id,
    userId: UserId,
    changes: List<ChatChange<CommitRecord>>
  ) {
    chatDatabase.withWriteTransaction {
      changes.forEach { change ->
        val commit = change.data
        when (change.changeType) {
          ChatChange.Type.Added,
          ChatChange.Type.Modified -> {
            applyInsertOrReplaceCommit(
              conversationId = conversationId,
              commit = commit,
              userId = userId,
              isPending = change.isPending
            )
          }
          ChatChange.Type.Removed -> {
            chatDatabase.chatCommitDao().delete(commit.id)
          }
        }
      }
    }
  }

  private suspend fun applyInsertOrReplaceCommit(
    conversationId: Conversation.Id,
    commit: CommitRecord,
    userId: UserId,
    isPending: Boolean
  ) {
    val row = commit.toCacheRow(
      conversationId = conversationId,
      selfUserId = userId,
      isPending = isPending
    )
    chatDatabase.chatCommitDao().insertOrReplaceIfConversationExists(row)
  }

  private fun oldestCursor(conversationId: Conversation.Id): Flow<CommitCursor?> {
    return chatDatabase.chatCommitDao()
      .observeOldestCursor(conversationId, branchId)
      .map { it?.toDomainModel() }
  }

  private suspend fun readOldestCursor(conversationId: Conversation.Id): CommitCursor? {
    return chatDatabase.chatCommitDao()
      .selectOldestCursor(conversationId, branchId)
      ?.toDomainModel()
  }

  private suspend fun readEditState(id: Commit.Id): EditState? {
    return chatDatabase.chatCommitDao()
      .selectEditState(id)
      ?.toDomainModel()
  }

  private suspend fun applyEditStatus(id: Commit.Id, text: String, status: Commit.Status) {
    chatDatabase.chatCommitDao().updateEdit(
      id = id,
      text = text,
      editedAtNanos = nowEpochNanos(),
      status = status.value
    )
  }

  private suspend fun applyRevertEdit(id: Commit.Id, previous: EditState) {
    chatDatabase.chatCommitDao().updateEdit(
      id = id,
      text = previous.text,
      editedAtNanos = previous.editedAtNanos,
      status = previous.status
    )
  }

  private suspend fun applyDeleteCommits(ids: List<Commit.Id>) {
    chatDatabase.withWriteTransaction {
      ids.forEach { chatDatabase.chatCommitDao().delete(it) }
    }
  }

  private suspend fun readCachedCommits(ids: List<Commit.Id>): List<ChatCommitEntity> {
    return chatDatabase.chatCommitDao().selectByIds(ids)
  }

  private suspend fun restoreCommits(commits: List<ChatCommitEntity>) {
    if (commits.isEmpty()) {
      return
    }
    chatDatabase.withWriteTransaction {
      commits.forEach { chatDatabase.chatCommitDao().insertOrReplaceIfConversationExists(it) }
    }
  }

  private suspend fun requireConversationId(): Conversation.Id {
    val conversationId = chatDatabase.chatBranchDao()
      .selectById(branchId)
      ?.conversationId
    return requireNotNull(conversationId) {
      "conversationId not found for branch ${branchId.value}"
    }
  }

  private suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) })
  }
}

private const val LATEST_PAGE_SIZE = 50
private const val HISTORY_PAGE_SIZE = 30
