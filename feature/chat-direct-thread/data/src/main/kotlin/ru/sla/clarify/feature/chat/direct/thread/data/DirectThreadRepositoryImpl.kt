package ru.sla.clarify.feature.chat.direct.thread.data

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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.chat.ChatCommit
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.direct.thread.data.common.ThreadMediator
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
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.clarify.lib.google.firestore.entity.UserNM
import ru.sla.clarify.lib.google.firestore.entity.write.LastCommitParams
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
import ru.sla.clarify.mapper.data.unreadDelta
import ru.sla.clarify.mapper.data.withReadStatus
import ru.sla.log.log
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import java.time.LocalDateTime

@SingleIn(DirectThreadScope::class)
@ContributesBinding(DirectThreadScope::class)
class DirectThreadRepositoryImpl @Inject constructor(
  params: TargetParams,
  private val firestore: Firestore,
  private val inMemoryDB: InMemoryDB,
  private val threadMediator: ThreadMediator
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
    val userId = threadMediator.requireUserId()
    val conversationId = threadMediator.awaitConversationId()
    // Ждём первую страницу, чтобы первое окно tail было [самый старый в кэше, +inf), а не вся
    // история: null-курсор потянул бы всё в кэш и свёл бы пагинацию на нет.
    fetchLatestCommitsCompletable.await()

    oldestCommitCursor(conversationId).flatMapLatest { cursor ->
      firestore.directCommitsLive(
        peerId = peerId.value,
        branchId = conversationId,
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
    val conversationId = threadMediator.awaitConversationId()
    firestore.branchesLive(
      conversationId = conversationId
    ).collect { changes ->
      applyBranchesChanges(
        changes = changes
      )
    }
  }

  override suspend fun subscribeOnBranchesUnreadCounts() {
    val conversationId = threadMediator.awaitConversationId()
    inMemoryDB.chatBranchQueries
      .selectIdsByConversationId(conversationId)
      .observeList()
      .collectLatest(::subscribeOnBranchUnreadCount)
  }

  override suspend fun fetchLatestCommits() {
    val conversationId = threadMediator.conversationId()
    if (conversationId == null) {
      // Разговора ещё нет (ни разу не писали): страничить нечего, сразу разблокируем tail.
      fetchLatestCommitsCompletable.complete(Unit)
      return
    }
    val latestCommits = firestore.readCommits(
      conversationId = conversationId,
      branchId = conversationId,
      limit = LATEST_PAGE_SIZE.toLong(),
      before = null
    )
    applyInsertOrReplaceCommits(
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
    val conversationId = threadMediator.awaitConversationId()

    // Весь кэш уже на экране, поэтому «долистали до верха» означает, что локальный кэш исчерпан:
    // страничим из Firestore, начиная сразу после самого старого закэшированного коммита
    // (точные createdAtNanos + id).
    val cursor = withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries
        .selectOldestCursor(conversationId, conversationId)
        .executeAsOneOrNull()
    }?.let { oldest ->
      CommitCursor(
        id = oldest.id,
        createdAt = oldest.createdAtNanos.epochNanosToTimestamp()
      )
    }

    val commitHistory = firestore.readCommits(
      conversationId = conversationId,
      branchId = conversationId,
      limit = HISTORY_PAGE_SIZE.toLong(),
      before = cursor,
      source = Source.SERVER
    )

    applyInsertOrReplaceCommits(commits = commitHistory)

    if (commitHistory.size < HISTORY_PAGE_SIZE) {
      hasCommitsHistoryCache.value = false
    }
  }

  override suspend fun sendCommit(text: String) {
    firestore.createDirectCommit(
      conversationId = threadMediator.conversationId(),
      text = text,
      peerId = peerId.value,
      branchId = null
    )
  }

  override suspend fun markAsRead() {
    val conversationId = threadMediator.conversationId() ?: return
    firestore.updateUnreadCount(conversationId)
  }

  override suspend fun markReadUpTo(lastReadAt: LocalDateTime) {
    val conversationId = threadMediator.conversationId() ?: return
    val current = lastReadWatermarkCache.value
    if (current != null && !lastReadAt.isAfter(current)) {
      log { "Direct: lastReadAt ($lastReadAt) is not after current watermark ($current), skipping" }
      return
    }
    lastReadWatermarkCache.value = lastReadAt
    firestore.updateReadWatermark(conversationId, lastReadAt)
    firestore.updateUnreadCount(conversationId)
  }

  override suspend fun createBranch(
    parentId: Branch.Id?,
    from: Commit.Id,
    name: String
  ): Branch.Id {
    return withContext(Dispatchers.IO) {
      val conversationId = threadMediator.requireConversationId()
      val remote = firestore.createBranch(
        conversationId = conversationId,
        parentBranchId = resolveBranchId(parentId),
        branchedFromCommitId = from.value,
        name = name
      )
      val branch = remote.toDomain(conversationId)
      applyInsertOrReplaceBranch(branch)
      branch.id
    }
  }

  override suspend fun editCommit(id: Commit.Id, text: String) {
    val conversationId = threadMediator.requireConversationId()
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
        conversationId = conversationId,
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
    val conversationId = threadMediator.requireConversationId()
    // Снимок удаляемых сообщений — вернём их на место, если сервер откажет.
    val removed = readCachedCommits(ids)
    // Для «у всех» денормализованные lastCommit/unreadDelta считаем по ПОЛНОМУ кэшу до удаления —
    // после оптимистичного удаления они уже не увидели бы удаляемые коммиты.
    val forEveryoneWrite = if (forEveryone) buildDeleteForEveryoneWrite(ids) else null
    // Оптимистичное локальное удаление ДО записи: лента и выделение не ждут ответа сервера.
    // Живой слушатель — forward-tail от самого нового коммита, поэтому удаление более старого
    // сообщения обратно через него не «всплывёт».
    applyDeleteCommits(ids = ids)
    // runSuspendCatching, а не try/catch: он пропускает CancellationException мимо, поэтому откат
    // не запускается из уже отменённой корутины, где suspend-вызов всё равно бросит, не доехав
    // до кэша. Расхождение в этом случае поправит live-слушатель.
    runSuspendCatching {
      if (forEveryoneWrite != null) {
        firestore.deleteDirectCommits(
          conversationId = conversationId,
          peerId = peerId.value,
          commitIds = ids.map { it.value },
          lastCommit = forEveryoneWrite.lastCommit,
          peerUnreadDelta = forEveryoneWrite.peerUnreadDelta
        )
      } else {
        firestore.hideCommits(
          conversationId = conversationId,
          commitIds = ids.map { it.value }
        )
      }
    }.onFailure { error ->
      // Сервер отказал — возвращаем сообщения в кэш; ошибка уходит наверх (ui покажет snackbar).
      restoreCommits(removed)
      throw error
    }
  }

  override val peer: Flow<Peer?> = inMemoryDB.userQueries
    .selectById(peerId.value, ::mapToPeer)
    .observeOneOrNull()

  override val hasCommitsHistory: Flow<Boolean> = hasCommitsHistoryCache

  override val commits: Flow<List<Commit>> = flow {
    val conversationId = threadMediator.awaitConversationId()

    val commitsFlow = inMemoryDB.chatCommitQueries
      .selectByBranchId(conversationId, conversationId, ::mapToCommit)
      .observeList()

    val peerReadAtFlow = firestore.memberLive(
      conversationId = conversationId,
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
    val conversationId = threadMediator.awaitConversationId()
    inMemoryDB.chatMemberQueries
      .selectByConversation(conversationId, ::mapToMember)
      .observeList()
      .collect { emit(it) }
  }

  override val unreadCount: Flow<Long> = flow {
    val conversationId = threadMediator.awaitConversationId()
    firestore.unreadCountLive(conversationId)
      .collect { emit(it) }
  }

  override val branches: Flow<List<Branch>> = flow {
    val conversationId = threadMediator.awaitConversationId()
    inMemoryDB.chatBranchQueries
      .selectByConversationId(conversationId, ::mapToBranch)
      .observeList()
      .collect { emit(it) }
  }

  private suspend fun subscribeOnBranchUnreadCount(ids: List<String>) {
    return coroutineScope {
      val conversationId = threadMediator.awaitConversationId()
      ids.forEach { branchId ->
        launch {
          firestore.branchUnreadCountLive(
            conversationId = conversationId,
            branchId = branchId
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

  private fun oldestCommitCursor(conversationId: String): Flow<CommitCursor?> {
    return inMemoryDB.chatCommitQueries
      .selectOldestCursor(conversationId, conversationId)
      .observeOneOrNull()
      .map { oldest ->
        oldest?.let {
          CommitCursor(
            id = it.id,
            createdAt = it.createdAtNanos.epochNanosToTimestamp()
          )
        }
      }
  }

  private suspend fun applyInsertOrReplaceCommits(commits: List<CommitNM>) {
    return withContext(Dispatchers.IO) {
      val userId = threadMediator.requireUserId()
      val conversationId = threadMediator.awaitConversationId()
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
      id = user.id,
      email = user.email,
      displayName = user.displayName,
      photoUrl = user.photoUrl
    )
  }

  private suspend fun applyCommitChanges(
    conversationId: String,
    userId: UserId,
    changes: List<FirestoreChange<CommitNM>>
  ): Unit = withContext(Dispatchers.IO) {
    inMemoryDB.transaction {
      changes.forEach { change ->
        val commit = change.data
        when (change.changeType) {
          FirestoreDocumentResult.Removed -> {
            inMemoryDB.chatCommitQueries.deleteById(commit.id)
          }

          FirestoreDocumentResult.Added,
          FirestoreDocumentResult.Modified -> {
            applyInsertOrReplaceCommit(
              conversationId = conversationId,
              commit = commit,
              userId = userId,
              hasPendingWrites = change.hasPendingWrites
            )
          }
        }
      }
    }
  }

  private fun applyInsertOrReplaceCommit(
    conversationId: String,
    commit: CommitNM,
    userId: UserId,
    hasPendingWrites: Boolean
  ) {
    inMemoryDB.chatCommitQueries.insertOrReplace(
      commit.toDomainModel(
        conversationId = conversationId,
        selfUserId = userId,
        hasPendingWrites = hasPendingWrites
      )
    )
  }

  private suspend fun applyBranchesChanges(changes: List<FirestoreChange<BranchNM>>) {
    return withContext(Dispatchers.IO) {
      val conversationId = threadMediator.awaitConversationId()
      inMemoryDB.transaction {
        changes.forEach { change ->
          when (change.changeType) {
            FirestoreDocumentResult.Removed -> {
              inMemoryDB.chatBranchQueries.deleteById(change.data.id)
            }

            FirestoreDocumentResult.Added,
            FirestoreDocumentResult.Modified -> {
              applyInsertOrReplaceBranch(change.data.toDomain(conversationId))
            }
          }
        }
      }
    }
  }

  private fun applyInsertOrReplaceBranch(branch: Branch) {
    inMemoryDB.chatBranchQueries.insertOrReplace(
      id = branch.id.value,
      conversationId = branch.conversationId.value,
      parentBranchId = branch.parentBranchId.value,
      branchedFromCommitId = branch.branchedFromCommitId.value,
      name = branch.name,
      lastCommit = branch.lastCommit,
      lastCommitTimestamp = branch.lastCommitTimestamp,
      createdAt = branch.createdAt,
      createdByUid = branch.createdById.value
    )
    val mergeRequest = branch.mergeRequest
    if (mergeRequest != null) {
      inMemoryDB.mergeRequestQueries.insertOrReplace(
        branchId = branch.id.value,
        status = mergeRequest.status.value,
        initiatorUid = mergeRequest.initiatorId.value,
        requestedAt = mergeRequest.requestedAt,
        approvedByUids = mergeRequest.approvedByIds.map { it.value },
        mergedAt = mergeRequest.mergedAt,
        mergedIntoBranchId = mergeRequest.mergedIntoBranchId?.value
      )
    } else {
      inMemoryDB.mergeRequestQueries.deleteByBranchId(branch.id.value)
    }
  }

  private suspend fun readEditState(id: Commit.Id): EditState? {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries
        .selectEditStateById(id.value)
        .executeAsOneOrNull()
        ?.let { EditState(text = it.text, editedAtNanos = it.editedAtNanos, status = it.status) }
    }
  }

  private suspend fun applyEditStatus(id: Commit.Id, text: String, status: Commit.Status) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries.updateEdit(
        id = id.value,
        text = text,
        editedAtNanos = Timestamp.now().toEpochNanos(),
        status = status.value
      )
    }
  }

  private suspend fun applyRevertEdit(id: Commit.Id, previous: EditState) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries.updateEdit(
        id = id.value,
        text = previous.text,
        editedAtNanos = previous.editedAtNanos,
        status = previous.status
      )
    }
  }

  private suspend fun applyDeleteCommits(ids: List<Commit.Id>) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.transaction {
        ids.forEach { inMemoryDB.chatCommitQueries.deleteById(it.value) }
      }
    }
  }

  private suspend fun readCachedCommits(ids: List<Commit.Id>): List<ChatCommit> {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries
        .selectByIds(ids.map { it.value })
        .executeAsList()
    }
  }

  private suspend fun restoreCommits(commits: List<ChatCommit>) {
    if (commits.isEmpty()) {
      return
    }
    return withContext(Dispatchers.IO) {
      inMemoryDB.transaction {
        commits.forEach { inMemoryDB.chatCommitQueries.insertOrReplace(it) }
      }
    }
  }

  private suspend fun buildDeleteForEveryoneWrite(ids: List<Commit.Id>): DeleteForEveryoneWrite {
    val conversationId = threadMediator.awaitConversationId()
    val rootCommits = withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries
        .selectByBranchId(conversationId, conversationId, ::mapToCommit)
        .executeAsList()
    }
    val peerMember = firestore.readMember(
      conversationId = conversationId,
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

  private suspend fun applyUpdateBranchUnreadCount(branchId: String, unreadCount: Long) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatBranchQueries.updateUnreadCount(
        id = branchId,
        unreadCount = unreadCount
      )
    }
  }

  private suspend fun resolveBranchId(branchId: Branch.Id?): String {
    return branchId?.value ?: threadMediator.conversationId() ?: error("conversationId not found")
  }
}

/** Снимок редактируемых полей строки кэша для отката оптимистичной правки при ошибке записи. */
private data class EditState(
  val text: String,
  val editedAtNanos: Long?,
  val status: String
)

/** Денормализованные поля для удаления «у всех», посчитанные по полному кэшу до удаления. */
private data class DeleteForEveryoneWrite(
  val lastCommit: LastCommitParams,
  val peerUnreadDelta: Int
)

private const val LATEST_PAGE_SIZE = 50
private const val HISTORY_PAGE_SIZE = 30
