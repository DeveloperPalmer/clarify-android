package ru.sla.clarify.feature.chat.branch.data

import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.chat.ChatCommit
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
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
import ru.sla.clarify.feature.chat.branch.data.mapper.toCursor
import ru.sla.clarify.feature.chat.branch.data.mapper.toDomainModel
import ru.sla.clarify.feature.chat.branch.domain.BranchRepository
import ru.sla.clarify.feature.chat.branch.domain.di.BranchScope
import ru.sla.clarify.feature.chat.branch.domain.entity.EditTargetNotFoundException
import ru.sla.clarify.feature.chat.branch.domain.entity.TargetParams
import ru.sla.clarify.mapper.data.lastCommitWriteAfterDeleting
import ru.sla.clarify.mapper.data.mapToBranch
import ru.sla.clarify.mapper.data.mapToCommit
import ru.sla.clarify.mapper.data.mapToMember
import ru.sla.clarify.mapper.data.mapToUser
import ru.sla.clarify.mapper.data.toCacheRow
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
  private val inMemoryDB: InMemoryDB,
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
    return withContext(Dispatchers.IO) {
      val conversationId = requireConversationId()
      commitApi.createBranchCommit(
        conversationId = conversationId.value,
        branchId = branchId.value,
        text = text,
        memberUids = memberUids(conversationId),
        replyCommit = replyCommit?.toReplyRecord()
      )
    }
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
    return withContext(Dispatchers.IO) {
      val peerId = branchPeerId()
      val deletedIds = ids.toSet()
      val conversationId = requireConversationId()

      val branchCommits = inMemoryDB.chatCommitQueries
        .select(conversationId, branchId, ::mapToCommit)
        .executeAsList()

      val peerLastReadAt = memberApi.readMember(
        conversationId = conversationId.value,
        memberId = peerId.value
      )

      DeleteForEveryoneWrite(
        peerId = peerId,
        lastCommit = branchCommits.lastCommitWriteAfterDeleting(deletedIds),
        peerUnreadDelta = branchCommits.unreadDelta(deletedIds, peerLastReadAt)
      )
    }
  }

  private suspend fun branchPeerId(): Peer.Id {
    val userId = requireUserId()
    val conversationId = requireConversationId()

    val memberId = inMemoryDB.chatMemberQueries
      .selectDirect(conversationId, ::mapToMember)
      .executeAsList()
      .firstOrNull { it.id.value != userId.value }
      ?.id

    if (memberId == null) {
      error("memberId is required for branch")
    }

    return Peer.Id(memberId.value)
  }

  override suspend fun markAsRead() {
    return withContext(Dispatchers.IO) {
      unreadCountApi.updateBranchUnreadCount(
        branchId = branchId.value,
        conversationId = requireConversationId().value
      )
    }
  }

  override suspend fun markReadUpTo(lastReadAt: LocalDateTime) {
    return withContext(Dispatchers.IO) {
      val conversationId = requireConversationId()
      val current = lastReadWatermark.value
      if (current != null && !lastReadAt.isAfter(current)) {
        log { "Branch: lastReadAt ($lastReadAt) is not after current watermark ($current), skipping" }
        return@withContext
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
  }

  override suspend fun openMergeRequest() {
    return withContext(Dispatchers.IO) {
      mergeRequestApi.createOpenMergeRequest(
        branchId = branchId.value,
        conversationId = requireConversationId().value
      )
    }
  }

  override suspend fun approveMergeRequest() {
    return withContext(Dispatchers.IO) {
      val conversationId = requireConversationId()
      mergeRequestApi.updateMergeApproval(
        branchId = branchId.value,
        conversationId = conversationId.value,
        memberUids = memberUids(conversationId)
      )
    }
  }

  override suspend fun revokeMergeRequestApproval() {
    return withContext(Dispatchers.IO) {
      mergeRequestApi.deleteMergeRequestApproval(
        branchId = branchId.value,
        conversationId = requireConversationId().value
      )
    }
  }

  override suspend fun cancelMergeRequest() {
    return withContext(Dispatchers.IO) {
      mergeRequestApi.deleteMergeRequest(
        branchId = branchId.value,
        conversationId = requireConversationId().value
      )
    }
  }

  override suspend fun finalizeMergeRequest() {
    return withContext(Dispatchers.IO) {
      mergeRequestApi.updateMergeFinalize(
        branchId = branchId.value,
        conversationId = requireConversationId().value
      )
    }
  }

  override val user: Flow<User?> = flow {
    val userId = authSessionPersistence.withKey { readUserId(it) }
    if (userId == null) return@flow emit(null)
    inMemoryDB.userQueries
      .select(userId, ::mapToUser)
      .observeOneOrNull()
      .collect { emit(it) }
  }

  override val branch: Flow<Branch?> = inMemoryDB.chatBranchQueries
    .selectById(branchId, ::mapToBranch)
    .observeOneOrNull()

  override val hasCommitsHistory: Flow<Boolean> = hasCommitsHistoryCache

  override val commits: Flow<List<Commit>> = flow {
    val conversationId = requireConversationId()
    val selfId = requireUserId()

    val commitsFlow = inMemoryDB.chatCommitQueries
      .select(conversationId, branchId, ::mapToCommit)
      .observeList()

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
    inMemoryDB.chatMemberQueries
      .selectDirect(conversationId, ::mapToMember)
      .observeList()
      .collect { emit(it) }
  }

  override fun member(id: UserId): Flow<Member?> = flow {
    val memberId = Member.Id(id.value)
    val conversationId = requireConversationId()
    inMemoryDB.chatMemberQueries
      .selectDirectById(conversationId, memberId, ::mapToMember)
      .observeOneOrNull()
      .collect { emit(it) }
  }

  private fun peerReadAt(conversationId: Conversation.Id, selfId: UserId): Flow<LocalDateTime?> = flow {
    val member = withContext(Dispatchers.IO) {
      inMemoryDB.chatMemberQueries
        .selectDirect(conversationId, ::mapToMember)
        .executeAsList()
        .firstOrNull { it.id.value != selfId.value }
    }
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

  private fun memberUids(conversationId: Conversation.Id): List<String> {
    return inMemoryDB.chatMemberQueries
      .selectIds(conversationId)
      .executeAsList()
      .map { it.value }
  }

  private suspend fun applyBranchChanges(branch: BranchRecord?) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.transaction {
        if (branch == null) {
          inMemoryDB.chatBranchQueries.delete(branchId)
        } else {
          applyInsertOrReplaceBranch(branch)
        }
      }
    }
  }

  private fun applyInsertOrReplaceBranch(branch: BranchRecord) {
    inMemoryDB.chatBranchQueries.insertOrReplace(
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
      inMemoryDB.mergeRequestQueries.insertOrReplace(
        branchId = branch.id,
        initiatorId = mergeRequest.initiatorId,
        approvedByIds = mergeRequest.approvedByIds,
        status = mergeRequest.status.value,
        requestedAt = mergeRequest.requestedAtSeconds,
        mergedAt = mergeRequest.mergedAtSeconds,
        mergedIntoBranchId = mergeRequest.mergedIntoBranchId
      )
    } else {
      inMemoryDB.mergeRequestQueries.delete(branch.id)
    }
  }

  private suspend fun applyUpdateUnreadCount(branchId: Branch.Id, unreadCount: Long) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatBranchQueries.updateUnreadCount(
        id = branchId,
        unreadCount = unreadCount
      )
    }
  }

  private suspend fun applyInsertOrReplaceCommits(conversationId: Conversation.Id, commits: List<CommitRecord>) {
    return withContext(Dispatchers.IO) {
      val userId = requireUserId()
      inMemoryDB.transaction {
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
  }

  private suspend fun applyCommitChanges(
    conversationId: Conversation.Id,
    userId: UserId,
    changes: List<ChatChange<CommitRecord>>
  ) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.transaction {
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
              inMemoryDB.chatCommitQueries.delete(commit.id)
            }
          }
        }
      }
    }
  }

  private fun applyInsertOrReplaceCommit(
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
    inMemoryDB.chatCommitQueries.insertOrReplace(
      id = row.id,
      conversationId = row.conversationId,
      branchId = row.branchId,
      senderId = row.senderId,
      type = row.type,
      text = row.text,
      replyCommit = row.replyCommit,
      invitedId = row.invitedId,
      createdAtNanos = row.createdAtNanos,
      isSelf = row.isSelf,
      status = row.status,
      editedAtNanos = row.editedAtNanos
    )
  }

  private fun oldestCursor(conversationId: Conversation.Id): Flow<CommitCursor?> {
    return inMemoryDB.chatCommitQueries
      .selectOldestCursor(conversationId, branchId)
      .observeOneOrNull()
      .map { oldest -> oldest?.toCursor() }
  }

  private suspend fun readOldestCursor(conversationId: Conversation.Id): CommitCursor? {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries
        .selectOldestCursor(conversationId, branchId)
        .executeAsOneOrNull()
        ?.toCursor()
    }
  }

  private suspend fun readEditState(id: Commit.Id): EditState? {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries
        .selectEditState(id)
        .executeAsOneOrNull()
        ?.toDomainModel()
    }
  }

  private suspend fun applyEditStatus(id: Commit.Id, text: String, status: Commit.Status) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries.updateEdit(
        id = id,
        text = text,
        status = status.value,
        editedAtNanos = nowEpochNanos()
      )
    }
  }

  private suspend fun applyRevertEdit(id: Commit.Id, previous: EditState) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries.updateEdit(
        id = id,
        text = previous.text,
        status = previous.status,
        editedAtNanos = previous.editedAtNanos
      )
    }
  }

  private suspend fun applyDeleteCommits(ids: List<Commit.Id>) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.transaction { ids.forEach { inMemoryDB.chatCommitQueries.delete(it) } }
    }
  }

  private suspend fun readCachedCommits(ids: List<Commit.Id>): List<ChatCommit> {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries
        .selectByIds(ids)
        .executeAsList()
    }
  }

  private suspend fun restoreCommits(commits: List<ChatCommit>) {
    if (commits.isEmpty()) {
      return
    }
    return withContext(Dispatchers.IO) {
      inMemoryDB.transaction {
        commits.forEach { commit ->
          inMemoryDB.chatCommitQueries.insertOrReplace(
            id = commit.id,
            conversationId = commit.conversationId,
            branchId = commit.branchId,
            senderId = commit.senderId,
            invitedId = commit.invitedId,
            type = commit.type,
            status = commit.status,
            text = commit.text,
            replyCommit = commit.replyCommit,
            isSelf = commit.isSelf,
            editedAtNanos = commit.editedAtNanos,
            createdAtNanos = commit.createdAtNanos
          )
        }
      }
    }
  }

  private fun requireConversationId(): Conversation.Id {
    val conversationId = inMemoryDB.chatBranchQueries
      .selectById(branchId, ::mapToBranch)
      .executeAsOneOrNull()
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
