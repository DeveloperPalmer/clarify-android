package ru.sla.clarify.feature.chat.direct.thread.data

import app.cash.sqldelight.Query
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.onFailure
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.chat.ChatCommit
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.direct.thread.data.entity.DeleteForEveryoneWrite
import ru.sla.clarify.feature.chat.direct.thread.data.entity.EditState
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.mapToPeer
import ru.sla.clarify.feature.chat.direct.thread.domain.DirectThreadRepository
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.EditTargetNotFoundException
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.TargetParams
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.FirestoreChange
import ru.sla.clarify.lib.google.firestore.entity.BranchNM
import ru.sla.clarify.lib.google.firestore.entity.CommitCursor
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.CommitNotFoundException
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.clarify.lib.google.firestore.entity.UserNM
import ru.sla.clarify.lib.google.firestore.epochNanosToTimestamp
import ru.sla.clarify.lib.google.firestore.toEpochMillis
import ru.sla.clarify.lib.google.firestore.toEpochNanos
import ru.sla.clarify.mapper.data.lastCommitWriteAfterDeleting
import ru.sla.clarify.mapper.data.mapToBranch
import ru.sla.clarify.mapper.data.mapToCommit
import ru.sla.clarify.mapper.data.mapToMember
import ru.sla.clarify.mapper.data.toDomain
import ru.sla.clarify.mapper.data.toDomainModel
import ru.sla.clarify.mapper.data.toLocalDateTime
import ru.sla.clarify.mapper.data.toNetworkModel
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
  private val firestore: Firestore,
  private val inMemoryDB: InMemoryDB,
  private val authSessionPersistence: AuthSessionPersistence
) : DirectThreadRepository {

  private val peerId = params.peerId
  private val lastReadWatermarkCache = MutableStateFlow<LocalDateTime?>(null)

  private val fetchCommitHistoryMutex = Mutex()
  private val hasCommitsHistoryCache = MutableStateFlow(true)
  private val fetchLatestCommitsCompletable = CompletableDeferred<Unit>()

  override suspend fun subscribeOnPeerChanges() {
    val peerUserId = UserId(peerId.value)
    firestore.userLive(peerUserId)
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
      firestore.directCommitsLive(
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
    firestore.branchesLive(
      conversationId = conversationId.value
    ).collect { changes ->
      applyBranchesChanges(
        changes = changes
      )
    }
  }

  override suspend fun subscribeOnBranchesUnreadCounts() {
    val conversationId = awaitConversationId()
    inMemoryDB.chatBranchQueries
      .selectIds(conversationId)
      .observeList()
      .collectLatest(::subscribeOnBranchUnreadCount)
  }

  override suspend fun fetchLatestCommits() {
    val conversationId = findConversationId()
    if (conversationId == null) {
      // Разговора ещё нет (ни разу не писали): страничить нечего, сразу разблокируем tail.
      fetchLatestCommitsCompletable.complete(Unit)
      return
    }
    val latestCommits = firestore.readCommits(
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

    val cursor = withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries
        .selectOldestCursor(conversationId, branchId)
        .executeAsOneOrNull()
    }?.let { oldest ->
      CommitCursor(
        id = oldest.id.value,
        createdAt = oldest.createdAtNanos.epochNanosToTimestamp()
      )
    }

    val commitHistory = firestore.readCommits(
      conversationId = conversationId.value,
      branchId = conversationId.value,
      limit = HISTORY_PAGE_SIZE.toLong(),
      before = cursor,
      source = Source.SERVER
    )

    applyInsertOrReplaceCommits(commits = commitHistory)

    if (commitHistory.size < HISTORY_PAGE_SIZE) {
      hasCommitsHistoryCache.value = false
    }
  }

  override suspend fun sendCommit(text: String, replyCommit: Commit.Message?) {
    firestore.createDirectCommit(
      conversationId = findConversationId()?.value,
      text = text,
      peerId = peerId.value,
      branchId = null,
      replyCommit = replyCommit?.toNetworkModel()
    )
  }

  override suspend fun markAsRead() {
    val conversationId = findConversationId() ?: return
    firestore.updateUnreadCount(conversationId.value)
  }

  override suspend fun markReadUpTo(lastReadAt: LocalDateTime) {
    val conversationId = findConversationId() ?: return
    val current = lastReadWatermarkCache.value
    if (current != null && !lastReadAt.isAfter(current)) {
      log { "Direct: lastReadAt ($lastReadAt) is not after current watermark ($current), skipping" }
      return
    }
    lastReadWatermarkCache.value = lastReadAt
    firestore.updateReadWatermark(
      conversationId = conversationId.value,
      lastReadAt = lastReadAt
    )
    firestore.updateUnreadCount(
      conversationId = conversationId.value
    )
  }

  override suspend fun createBranch(
    parentId: Branch.Id?,
    from: Commit.Id,
    name: String
  ): Branch.Id {
    return withContext(Dispatchers.IO) {
      val conversationId = requireConversationId()
      val remote = firestore.createBranch(
        conversationId = conversationId.value,
        parentBranchId = resolveBranchId(parentId).value,
        branchedFromCommitId = from.value,
        name = name
      )
      val branch = remote.toDomain(conversationId)
      applyInsertOrReplaceBranch(branch)
      branch.id
    }
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
      firestore.updateDirectCommit(
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
        firestore.deleteDirectCommits(
          conversationId = conversationId.value,
          peerId = peerId.value,
          commitIds = ids.map { it.value },
          lastCommit = forEveryoneWrite.lastCommit,
          peerUnreadDelta = forEveryoneWrite.peerUnreadDelta
        )
      } else {
        firestore.hideCommits(
          conversationId = conversationId.value,
          commitIds = ids.map { it.value }
        )
      }
    }.onFailure { error ->
      restoreCommits(removableCommits)
      throw error
    }
  }

  override val peer: Flow<Peer?> = inMemoryDB.userQueries
    .select(UserId(peerId.value), ::mapToPeer)
    .observeOneOrNull()

  override val hasCommitsHistory: Flow<Boolean> = hasCommitsHistoryCache

  override val commits: Flow<List<Commit>> = flow {
    val conversationId = awaitConversationId()
    val branchId = Branch.Id(conversationId.value)

    val commitsFlow = inMemoryDB.chatCommitQueries
      .select(conversationId, branchId, ::mapToCommit)
      .observeList()

    val peerReadAtFlow = firestore.memberLive(
      conversationId = conversationId.value,
      memberId = peerId.value
    ).map { member ->
      member
        ?.lastReadAt
        ?.toEpochMillis()
        ?.toLocalDateTime()
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
    inMemoryDB.chatMemberQueries
      .selectDirect(conversationId, ::mapToMember)
      .observeList()
      .collect { emit(it) }
  }

  override val unreadCount: Flow<Long> = flow {
    val conversationId = awaitConversationId()
    firestore
      .unreadCountLive(conversationId.value)
      .collect { emit(it) }
  }

  override val branches: Flow<List<Branch>> = flow {
    val conversationId = awaitConversationId()
    inMemoryDB.chatBranchQueries
      .selectByConversation(conversationId, ::mapToBranch)
      .observeList()
      .collect { emit(it) }
  }

  private suspend fun subscribeOnBranchUnreadCount(ids: List<Branch.Id>) {
    return coroutineScope {
      val conversationId = awaitConversationId()
      ids.forEach { branchId ->
        launch {
          firestore.branchUnreadCountLive(
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
    return inMemoryDB.chatCommitQueries
      .selectOldestCursor(conversationId, Branch.Id(conversationId.value))
      .observeOneOrNull()
      .map { oldest ->
        oldest?.let {
          CommitCursor(
            id = it.id.value,
            createdAt = it.createdAtNanos.epochNanosToTimestamp()
          )
        }
      }
  }

  private suspend fun applyInsertOrReplaceCommits(commits: List<CommitNM>) {
    return withContext(Dispatchers.IO) {
      val userId = requireUserId()
      val conversationId = awaitConversationId()
      inMemoryDB.transaction {
        commits.forEach { item ->
          applyInsertOrReplaceCommit(
            conversationId = conversationId,
            commit = item,
            userId = userId,
            hasPendingWrites = false
          )
        }
      }
    }
  }

  private suspend fun applyPeerChanges(user: UserNM): Unit = withContext(Dispatchers.IO) {
    inMemoryDB.userQueries.insertOrReplace(
      id = UserId(user.id),
      email = user.email,
      displayName = user.displayName,
      photoUrl = user.photoUrl
    )
  }

  private suspend fun applyCommitChanges(
    conversationId: Conversation.Id,
    userId: UserId,
    changes: List<FirestoreChange<CommitNM>>
  ): Unit = withContext(Dispatchers.IO) {
    inMemoryDB.transaction {
      changes.forEach { change ->
        val commit = change.data
        when (change.changeType) {
          FirestoreDocumentResult.Added,
          FirestoreDocumentResult.Modified -> {
            applyInsertOrReplaceCommit(
              conversationId = conversationId,
              commit = commit,
              userId = userId,
              hasPendingWrites = change.hasPendingWrites
            )
          }
          FirestoreDocumentResult.Removed -> {
            inMemoryDB.chatCommitQueries.delete(Commit.Id(commit.id))
          }
        }
      }
    }
  }

  private fun applyInsertOrReplaceCommit(
    conversationId: Conversation.Id,
    commit: CommitNM,
    userId: UserId,
    hasPendingWrites: Boolean
  ) {
    val row = commit.toDomainModel(
      conversationId = conversationId,
      selfUserId = userId,
      hasPendingWrites = hasPendingWrites
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

  private suspend fun applyBranchesChanges(changes: List<FirestoreChange<BranchNM>>) {
    return withContext(Dispatchers.IO) {
      val conversationId = awaitConversationId()
      inMemoryDB.transaction {
        changes.forEach { change ->
          when (change.changeType) {
            FirestoreDocumentResult.Added,
            FirestoreDocumentResult.Modified -> {
              applyInsertOrReplaceBranch(change.data.toDomain(conversationId))
            }
            FirestoreDocumentResult.Removed -> {
              inMemoryDB.chatBranchQueries.delete(Branch.Id(change.data.id))
            }
          }
        }
      }
    }
  }

  private fun applyInsertOrReplaceBranch(branch: Branch) {
    inMemoryDB.chatBranchQueries.insertOrReplace(
      id = branch.id,
      conversationId = branch.conversationId,
      parentBranchId = branch.parentBranchId,
      branchedFromCommitId = branch.branchedFromCommitId,
      name = branch.name,
      lastCommit = branch.lastCommit,
      lastCommitTimestamp = branch.lastCommitTimestamp,
      createdAt = branch.createdAt,
      createdById = branch.createdById
    )
    val mergeRequest = branch.mergeRequest
    if (mergeRequest != null) {
      inMemoryDB.mergeRequestQueries.insertOrReplace(
        branchId = branch.id,
        status = mergeRequest.status.value,
        initiatorId = mergeRequest.initiatorId,
        requestedAt = mergeRequest.requestedAt,
        approvedByIds = mergeRequest.approvedByIds,
        mergedAt = mergeRequest.mergedAt,
        mergedIntoBranchId = mergeRequest.mergedIntoBranchId
      )
    } else {
      inMemoryDB.mergeRequestQueries.delete(branch.id)
    }
  }

  private suspend fun readEditState(id: Commit.Id): EditState? {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries
        .selectEditState(id)
        .executeAsOneOrNull()
        ?.let { EditState(text = it.text, editedAtNanos = it.editedAtNanos, status = it.status) }
    }
  }

  private suspend fun applyEditStatus(id: Commit.Id, text: String, status: Commit.Status) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries.updateEdit(
        id = id,
        text = text,
        editedAtNanos = Timestamp.now().toEpochNanos(),
        status = status.value
      )
    }
  }

  private suspend fun applyRevertEdit(id: Commit.Id, previous: EditState) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries.updateEdit(
        id = id,
        text = previous.text,
        editedAtNanos = previous.editedAtNanos,
        status = previous.status
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
            type = commit.type,
            text = commit.text,
            replyCommit = commit.replyCommit,
            invitedId = commit.invitedId,
            createdAtNanos = commit.createdAtNanos,
            isSelf = commit.isSelf,
            status = commit.status,
            editedAtNanos = commit.editedAtNanos
          )
        }
      }
    }
  }

  private suspend fun buildDeleteForEveryoneWrite(ids: List<Commit.Id>): DeleteForEveryoneWrite {
    val conversationId = awaitConversationId()
    val branchId = Branch.Id(conversationId.value)
    val rootCommits = withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries
        .select(conversationId, branchId, ::mapToCommit)
        .executeAsList()
    }

    val peerMember = firestore.readMember(
      conversationId = conversationId.value,
      memberId = peerId.value
    )

    val peerLastReadAt = peerMember?.lastReadAt
      ?.toEpochMillis()
      ?.toLocalDateTime()

    val deletedIds = ids.toSet()
    return DeleteForEveryoneWrite(
      lastCommit = rootCommits.lastCommitWriteAfterDeleting(deletedIds),
      peerUnreadDelta = rootCommits.unreadDelta(deletedIds, peerLastReadAt)
    )
  }

  private suspend fun applyUpdateBranchUnreadCount(branchId: Branch.Id, unreadCount: Long) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatBranchQueries.updateUnreadCount(
        id = branchId,
        unreadCount = unreadCount
      )
    }
  }

  private suspend fun resolveBranchId(branchId: Branch.Id?): Branch.Id {
    val result = (branchId?.value ?: findConversationId()?.value)?.let(Branch::Id)
    return result ?: error("conversationId not found")
  }

  private suspend fun awaitConversationId(): Conversation.Id {
    findConversationId()?.let { return it }

    return selectDirectConversationId(directMemberIds())
      .observeOneOrNull()
      .filterNotNull()
      .first()
  }

  private suspend fun findConversationId(): Conversation.Id? {
    val memberIds = directMemberIds()
    return withContext(Dispatchers.IO) {
      selectDirectConversationId(memberIds).executeAsOneOrNull()
    }
  }

  private suspend fun requireConversationId(): Conversation.Id {
    return requireNotNull(findConversationId()) {
      "conversationId is null. A branch can only be created for an existing conversation."
    }
  }

  private fun selectDirectConversationId(memberIds: List<Member.Id>): Query<Conversation.Id> {
    return inMemoryDB.chatConversationQueries.selectIdByMembers(
      memberIds = memberIds,
      memberCount = memberIds.size.toLong(),
      type = ConversationNM.Type.Direct.value
    )
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
