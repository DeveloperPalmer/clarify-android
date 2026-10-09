package ru.sla.clarify.feature.chat.direct.thread.data

import androidx.room3.withWriteTransaction
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.chat.api.BranchApi
import ru.sla.clarify.chat.api.CommitApi
import ru.sla.clarify.chat.api.MemberApi
import ru.sla.clarify.chat.api.UnreadCountApi
import ru.sla.clarify.chat.api.UserApi
import ru.sla.clarify.core.domain.date.nowEpochNanos
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.ChatDatabase
import ru.sla.clarify.database.entity.ChatCommitEntity
import ru.sla.clarify.database.upsertBranch
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.BranchRecord
import ru.sla.clarify.entity.chat.ChatChange
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.CommitCursor
import ru.sla.clarify.entity.chat.CommitNotFoundException
import ru.sla.clarify.entity.chat.CommitRecord
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.ConversationRecord
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.direct.thread.data.entity.DeleteForEveryoneWrite
import ru.sla.clarify.feature.chat.direct.thread.data.entity.EditState
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.toDomainModel
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.toPeer
import ru.sla.clarify.feature.chat.direct.thread.domain.DirectThreadRepository
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.EditTargetNotFoundException
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.TargetParams
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
@SingleIn(DirectThreadScope::class)
@ContributesBinding(DirectThreadScope::class)
class DirectThreadRepositoryImpl @Inject constructor(
  params: TargetParams,
  private val branchApi: BranchApi,
  private val commitApi: CommitApi,
  private val memberApi: MemberApi,
  private val unreadCountApi: UnreadCountApi,
  private val userApi: UserApi,
  private val chatDatabase: ChatDatabase,
  private val authSessionPersistence: AuthSessionPersistence
) : DirectThreadRepository {

  private val peerId = params.peerId
  private val lastReadWatermarkCache = MutableStateFlow<LocalDateTime?>(null)

  private val fetchCommitHistoryMutex = Mutex()
  private val hasCommitsHistoryCache = MutableStateFlow(true)
  private val fetchLatestCommitsCompletable = CompletableDeferred<Unit>()

  override suspend fun subscribeOnPeerChanges() {
    val peerUserId = UserId(peerId.value)
    userApi.userLive(peerUserId)
      .filterNotNull()
      .collect(::applyPeerChanges)
  }

  override suspend fun subscribeOnCommitChanges() {
    val userId = requireUserId()
    val conversationId = awaitConversationId()
    // Ждём первую страницу, чтобы первое окно tail было [самый старый в кэше, +inf), а не вся
    // история: null-курсор потянул бы всё в кэш и свёл бы пагинацию на нет.
    fetchLatestCommitsCompletable.await()

    oldestCommitCursor(conversationId).flatMapLatest { cursor ->
      commitApi.directCommitsLive(
        peerId = peerId.value,
        branchId = conversationId.value,
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

  override suspend fun subscribeOnBranchesChanges() {
    val conversationId = awaitConversationId()
    branchApi.branchesLive(
      conversationId = conversationId.value
    ).collect { changes ->
      applyBranchesChanges(
        changes = changes
      )
    }
  }

  override suspend fun subscribeOnBranchesUnreadCounts() {
    val conversationId = awaitConversationId()
    chatDatabase.chatBranchDao()
      .observeIds(conversationId)
      .collectLatest(::subscribeOnBranchUnreadCount)
  }

  override suspend fun fetchLatestCommits() {
    val conversationId = findConversationId()
    if (conversationId == null) {
      // Разговора ещё нет (ни разу не писали): страничить нечего, сразу разблокируем tail.
      fetchLatestCommitsCompletable.complete(Unit)
      return
    }
    val latestCommits = commitApi.readCommits(
      conversationId = conversationId.value,
      branchId = conversationId.value,
      limit = LATEST_PAGE_SIZE.toLong(),
      before = null
    )
    applyInsertOrReplaceCommits(
      commits = latestCommits
    )
    fetchLatestCommitsCompletable.complete(Unit)
  }

  override suspend fun fetchCommitHistory() = fetchCommitHistoryMutex.withLock {
    if (!hasCommitsHistoryCache.value) {
      return@withLock
    }
    val conversationId = awaitConversationId()
    val branchId = Branch.Id(conversationId.value)

    val cursor = chatDatabase.chatCommitDao()
      .selectOldestCursor(conversationId, branchId)
      ?.toDomainModel()

    val commitHistory = commitApi.readCommits(
      conversationId = conversationId.value,
      branchId = conversationId.value,
      limit = HISTORY_PAGE_SIZE.toLong(),
      before = cursor,
      fromServerOnly = true
    )

    applyInsertOrReplaceCommits(commits = commitHistory)

    if (commitHistory.size < HISTORY_PAGE_SIZE) {
      hasCommitsHistoryCache.value = false
    }
  }

  override suspend fun sendCommit(text: String, replyCommit: Commit.Message?) {
    commitApi.createDirectCommit(
      conversationId = findConversationId()?.value,
      text = text,
      peerId = peerId.value,
      branchId = null,
      replyCommit = replyCommit?.toReplyRecord()
    )
  }

  override suspend fun markAsRead() {
    val conversationId = findConversationId() ?: return
    unreadCountApi.updateUnreadCount(conversationId.value)
  }

  override suspend fun markReadUpTo(lastReadAt: LocalDateTime) {
    val conversationId = findConversationId() ?: return
    val current = lastReadWatermarkCache.value
    if (current != null && !lastReadAt.isAfter(current)) {
      log { "Direct: lastReadAt ($lastReadAt) is not after current watermark ($current), skipping" }
      return
    }
    lastReadWatermarkCache.value = lastReadAt
    memberApi.updateReadWatermark(
      conversationId = conversationId.value,
      lastReadAt = lastReadAt
    )
    unreadCountApi.updateUnreadCount(
      conversationId = conversationId.value
    )
  }

  override suspend fun createBranch(
    parentId: Branch.Id?,
    from: Commit.Id,
    name: String
  ): Branch.Id {
    val conversationId = requireConversationId()
    val branch = branchApi.createBranch(
      conversationId = conversationId.value,
      parentBranchId = resolveBranchId(parentId).value,
      branchedFromCommitId = from.value,
      name = name
    )
    chatDatabase.upsertBranch(branch)
    return branch.id
  }

  override suspend fun editCommit(id: Commit.Id, text: String) {
    val conversationId = requireConversationId()
    // Оптимистично: сразу показываем новый текст со статусом «в процессе» (часы). Транзакции
    // Firestore не дают latency-компенсированных событий, поэтому кэш ведём сами — прогресс правки
    // виден мгновенно, а не после ответа сервера. Прежнее состояние держим для отката.
    val previous = readEditState(id) ?: return
    applyEditStatus(
      id = id,
      text = text,
      status = Commit.Status.Sending
    )
    runSuspendCatching {
      commitApi.updateDirectCommit(
        conversationId = conversationId.value,
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

  override suspend fun deleteCommits(ids: List<Commit.Id>, forEveryone: Boolean) {
    val conversationId = requireConversationId()
    val removableCommits = readCachedCommits(ids)
    // Для «у всех» денормализованные lastCommit/unreadDelta считаем по ПОЛНОМУ кэшу до удаления —
    // после оптимистичного удаления они уже не увидели бы удаляемые коммиты.
    val forEveryoneWrite = if (forEveryone) buildDeleteForEveryoneWrite(ids) else null
    // Оптимистичное локальное удаление ДО записи: лента и выделение не ждут ответа сервера.
    // Живой слушатель — forward-tail от самого нового коммита, поэтому удаление более старого
    // сообщения обратно через него не «всплывёт».
    applyDeleteCommits(ids = ids)
    runSuspendCatching {
      if (forEveryoneWrite != null) {
        commitApi.deleteDirectCommits(
          conversationId = conversationId.value,
          peerId = peerId.value,
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

  override val peer: Flow<Peer?> = chatDatabase.userDao()
    .observe(UserId(peerId.value))
    .map { it?.toPeer() }

  override val hasCommitsHistory: Flow<Boolean> = hasCommitsHistoryCache

  override val commits: Flow<List<Commit>> = flow {
    val conversationId = awaitConversationId()
    val branchId = Branch.Id(conversationId.value)

    val commitsFlow = chatDatabase.chatCommitDao()
      .observe(conversationId, branchId)
      .map { rows -> rows.map { it.toDomainModel() } }

    val peerReadAtFlow = memberApi.memberLive(
      conversationId = conversationId.value,
      memberId = peerId.value
    ).map { member ->
      member
    }.distinctUntilChanged()

    combine(
      flow = commitsFlow,
      flow2 = peerReadAtFlow
    ) { commits, peerReadAt ->
      commits.map { it.withReadStatus(peerReadAt) }
    }.collect { emit(it) }
  }

  override val members: Flow<List<Member>> = flow {
    val conversationId = awaitConversationId()
    chatDatabase.chatMemberDao()
      .observeDirect(conversationId)
      .map { rows -> rows.map { it.toDomainModel() } }
      .collect { emit(it) }
  }

  override val unreadCount: Flow<Long> = flow {
    val conversationId = awaitConversationId()
    unreadCountApi
      .unreadCountLive(conversationId.value)
      .collect { emit(it) }
  }

  override val branches: Flow<List<Branch>> = flow {
    val conversationId = awaitConversationId()
    chatDatabase.chatBranchDao()
      .observeByConversation(conversationId)
      .map { rows -> rows.map { it.toDomainModel() } }
      .collect { emit(it) }
  }

  private suspend fun subscribeOnBranchUnreadCount(ids: List<Branch.Id>) {
    return coroutineScope {
      val conversationId = awaitConversationId()
      ids.forEach { branchId ->
        launch {
          unreadCountApi.branchUnreadCountLive(
            conversationId = conversationId.value,
            branchId = branchId.value
          ).collect { unreadCount ->
            applyUpdateBranchUnreadCount(
              branchId = branchId,
              unreadCount = unreadCount
            )
          }
        }
      }
    }
  }

  private fun oldestCommitCursor(conversationId: Conversation.Id): Flow<CommitCursor?> {
    return chatDatabase.chatCommitDao()
      .observeOldestCursor(conversationId, Branch.Id(conversationId.value))
      .map { it?.toDomainModel() }
  }

  private suspend fun applyInsertOrReplaceCommits(commits: List<CommitRecord>) {
    val userId = requireUserId()
    val conversationId = awaitConversationId()
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

  private suspend fun applyPeerChanges(user: User) {
    chatDatabase.userDao().insertOrReplace(user.toCacheRow())
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

  private suspend fun applyBranchesChanges(changes: List<ChatChange<BranchRecord>>) {
    awaitConversationId()
    chatDatabase.withWriteTransaction {
      changes.forEach { change ->
        when (change.changeType) {
          ChatChange.Type.Added,
          ChatChange.Type.Modified -> {
            chatDatabase.upsertBranch(change.data)
          }
          ChatChange.Type.Removed -> {
            chatDatabase.chatBranchDao().delete(change.data.id)
          }
        }
      }
    }
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

  private suspend fun buildDeleteForEveryoneWrite(ids: List<Commit.Id>): DeleteForEveryoneWrite {
    val conversationId = awaitConversationId()
    val branchId = Branch.Id(conversationId.value)
    val rootCommits = chatDatabase.chatCommitDao()
      .select(conversationId, branchId)
      .map { it.toDomainModel() }

    val peerLastReadAt = memberApi.readMember(
      conversationId = conversationId.value,
      memberId = peerId.value
    )

    val deletedIds = ids.toSet()
    return DeleteForEveryoneWrite(
      lastCommit = rootCommits.lastCommitWriteAfterDeleting(deletedIds),
      peerUnreadDelta = rootCommits.unreadDelta(deletedIds, peerLastReadAt)
    )
  }

  private suspend fun applyUpdateBranchUnreadCount(branchId: Branch.Id, unreadCount: Long) {
    chatDatabase.chatBranchDao().updateUnreadCount(
      id = branchId,
      unreadCount = unreadCount
    )
  }

  private suspend fun resolveBranchId(branchId: Branch.Id?): Branch.Id {
    val result = (branchId?.value ?: findConversationId()?.value)?.let(Branch::Id)
    return result ?: error("conversationId not found")
  }

  private suspend fun awaitConversationId(): Conversation.Id {
    findConversationId()?.let { return it }

    val memberIds = directMemberIds()
    return chatDatabase.chatConversationDao()
      .observeIdByMembers(
        type = ConversationRecord.Type.Direct.value,
        memberIds = memberIds,
        memberCount = memberIds.size.toLong()
      )
      .filterNotNull()
      .first()
  }

  private suspend fun findConversationId(): Conversation.Id? {
    val memberIds = directMemberIds()
    return chatDatabase.chatConversationDao().selectIdByMembers(
      type = ConversationRecord.Type.Direct.value,
      memberIds = memberIds,
      memberCount = memberIds.size.toLong()
    )
  }

  private suspend fun requireConversationId(): Conversation.Id {
    return requireNotNull(findConversationId()) {
      "conversationId is null. A branch can only be created for an existing conversation."
    }
  }

  private suspend fun directMemberIds(): List<Member.Id> {
    return setOf(requireUserId().value, peerId.value)
      .sorted()
      .map(Member::Id)
  }

  private suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) })
  }
}

private const val LATEST_PAGE_SIZE = 50
private const val HISTORY_PAGE_SIZE = 30
