package ru.sla.clarify.feature.chat.branch.data

import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.onFailure
import com.google.firebase.firestore.Source
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
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.branch.domain.BranchRepository
import ru.sla.clarify.feature.chat.branch.domain.di.BranchScope
import ru.sla.clarify.feature.chat.branch.domain.entity.EditTargetNotFoundException
import ru.sla.clarify.feature.chat.branch.domain.entity.TargetParams
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.FirestoreChange
import ru.sla.clarify.lib.google.firestore.entity.BranchNM
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.CommitNotFoundException
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.clarify.lib.google.firestore.entity.write.LastCommitParams
import ru.sla.clarify.lib.google.firestore.toEpochMillis
import ru.sla.clarify.mapper.data.lastCommitWriteAfterDeleting
import ru.sla.clarify.mapper.data.mapToBranch
import ru.sla.clarify.mapper.data.mapToCommit
import ru.sla.clarify.mapper.data.mapToMember
import ru.sla.clarify.mapper.data.mapToUser
import ru.sla.clarify.mapper.data.toDomain
import ru.sla.clarify.mapper.data.toDomainModel
import ru.sla.clarify.mapper.data.toLocalDateTime
import ru.sla.clarify.mapper.data.unreadDelta
import ru.sla.clarify.mapper.data.withReadStatus
import ru.sla.log.log
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import java.time.LocalDateTime

@SingleIn(BranchScope::class)
@ContributesBinding(BranchScope::class)
class BranchRepositoryImpl @Inject constructor(
  params: TargetParams,
  private val firestore: Firestore,
  private val inMemoryDB: InMemoryDB,
  private val authSessionPersistence: AuthSessionPersistence
) : BranchRepository {

  private val branchId = params.branchId
  private val lastReadWatermark = MutableStateFlow<LocalDateTime?>(null)

  private val commitCache = BranchCommitCache(inMemoryDB, branchId)
  private val fetchCommitHistoryMutex = Mutex()
  private val hasCommitsHistoryCache = MutableStateFlow(true)
  private val fetchLatestCommitsCompletable = CompletableDeferred<Unit>()

  override suspend fun subscribeOnBranchChanges() {
    val conversationId = requireConversationId()
    firestore.branchLive(
      conversationId = conversationId,
      branchId = branchId.value
    ).collect { branch ->
      applyBranchChanges(
        conversationId = conversationId,
        branch = branch
      )
    }
  }

  override suspend fun subscribeOnBranchCommitsChanges() {
    val userId = requireUserId()
    val conversationId = requireConversationId()
    // Ждём первую страницу, чтобы первое окно tail было [самый старый в кэше, +inf), а не вся
    // история: null-курсор потянул бы всё в кэш и свёл бы пагинацию на нет.
    fetchLatestCommitsCompletable.await()

    commitCache.oldestCursor(conversationId).flatMapLatest { cursor ->
      firestore.commitsLive(
        conversationId = conversationId,
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
    firestore.branchUnreadCountLive(
      conversationId = conversationId,
      branchId = branchId.value
    ).collect { unreadCount ->
      applyUpdateUnreadCount(
        branchId = branchId.value,
        unreadCount = unreadCount
      )
    }
  }

  override suspend fun fetchLatestCommits() {
    val conversationId = requireConversationId()
    val latestCommits = firestore.readCommits(
      conversationId = conversationId,
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

    // Весь кэш уже на экране, поэтому «долистали до верха» означает, что локальный кэш исчерпан:
    // страничим из Firestore, начиная сразу после самого старого закэшированного коммита ветки.
    val cursor = commitCache.readOldestCursor(conversationId)

    val commitHistory = firestore.readCommits(
      conversationId = conversationId,
      branchId = branchId.value,
      limit = HISTORY_PAGE_SIZE.toLong(),
      before = cursor,
      source = Source.SERVER
    )

    applyInsertOrReplaceCommits(
      conversationId = conversationId,
      commits = commitHistory
    )

    if (commitHistory.size < HISTORY_PAGE_SIZE) {
      hasCommitsHistoryCache.value = false
    }
  }

  override suspend fun sendCommit(text: String) {
    return withContext(Dispatchers.IO) {
      val conversationId = requireConversationId()
      firestore.createBranchCommit(
        conversationId = conversationId,
        branchId = branchId.value,
        text = text,
        memberUids = memberUids(conversationId)
      )
    }
  }

  override suspend fun editCommit(id: Commit.Id, text: String) {
    val conversationId = requireConversationId()
    // Оптимистично: сразу показываем новый текст со статусом «в процессе» (часы). Прежнее
    // состояние держим для отката при ошибке записи.
    val previous = commitCache.readEditState(id) ?: return
    commitCache.applyEdit(
      id = id,
      text = text,
      status = Commit.Status.Sending
    )
    runSuspendCatching {
      firestore.updateBranchCommit(
        conversationId = conversationId,
        branchId = branchId.value,
        commitId = id.value,
        text = text
      )
    }.onFailure { error ->
      if (error is CommitNotFoundException) {
        commitCache.deleteCommits(listOf(id))
        throw EditTargetNotFoundException(id)
      }
      commitCache.revertEdit(id, previous)
      throw error
    }
    commitCache.applyEdit(
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
    // Снимок удаляемых сообщений — вернём их на место, если сервер откажет.
    val removed = commitCache.readCommits(ids)
    // Для «у всех» денормализованные lastCommit/unreadDelta считаем по ПОЛНОМУ кэшу до удаления —
    // после оптимистичного удаления они уже не увидели бы удаляемые коммиты.
    val forEveryoneWrite = if (forEveryone) buildDeleteForEveryoneWrite(ids) else null
    // Оптимистичное локальное удаление ДО записи: лента и выделение не ждут ответа сервера.
    // Живой tail-слушатель идёт вперёд от самого старого коммита, поэтому удаление обратно не «всплывёт».
    commitCache.deleteCommits(ids = ids)
    // runSuspendCatching, а не try/catch: он пропускает CancellationException мимо, поэтому откат
    // не запускается из уже отменённой корутины, где suspend-вызов всё равно бросит, не доехав
    // до кэша. Расхождение в этом случае поправит live-слушатель.
    runSuspendCatching {
      if (forEveryoneWrite != null) {
        firestore.deleteBranchCommits(
          conversationId = conversationId,
          branchId = branchId.value,
          peerId = forEveryoneWrite.peerId.value,
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
      commitCache.restoreCommits(removed)
      throw error
    }
  }

  private suspend fun buildDeleteForEveryoneWrite(ids: List<Commit.Id>): DeleteForEveryoneWrite {
    return withContext(Dispatchers.IO) {
      val peerId = branchPeerId()
      val deletedIds = ids.toSet()
      val conversationId = requireConversationId()

      val branchCommits = inMemoryDB.chatCommitQueries
        .selectByBranchId(conversationId, branchId.value, ::mapToCommit)
        .executeAsList()

      val peerLastReadAt = firestore.readMember(
        conversationId = conversationId,
        memberId = peerId.value
      )
        ?.lastReadAt
        ?.toEpochMillis()
        ?.toLocalDateTime()

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

    val memberId = inMemoryDB.chatConversationMemberQueries
      .selectByConversation(conversationId, ::mapToMember)
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
      firestore.updateBranchUnreadCount(
        branchId = branchId.value,
        conversationId = requireConversationId()
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
      firestore.updateReadWatermark(
        conversationId = conversationId,
        lastReadAt = lastReadAt
      )
      firestore.updateBranchUnreadCount(
        branchId = branchId.value,
        conversationId = conversationId
      )
    }
  }

  override suspend fun openMergeRequest() {
    return withContext(Dispatchers.IO) {
      firestore.createOpenMergeRequest(
        branchId = branchId.value,
        conversationId = requireConversationId()
      )
    }
  }

  override suspend fun approveMergeRequest() {
    return withContext(Dispatchers.IO) {
      val conversationId = requireConversationId()
      firestore.updateMergeApproval(
        branchId = branchId.value,
        conversationId = conversationId,
        memberUids = memberUids(conversationId)
      )
    }
  }

  override suspend fun revokeMergeRequestApproval() {
    return withContext(Dispatchers.IO) {
      firestore.deleteMergeRequestApproval(
        branchId = branchId.value,
        conversationId = requireConversationId()
      )
    }
  }

  override suspend fun cancelMergeRequest() {
    return withContext(Dispatchers.IO) {
      firestore.deleteMergeRequest(
        branchId = branchId.value,
        conversationId = requireConversationId()
      )
    }
  }

  override suspend fun finalizeMergeRequest() {
    return withContext(Dispatchers.IO) {
      firestore.updateMergeFinalize(
        branchId = branchId.value,
        conversationId = requireConversationId()
      )
    }
  }

  override val user: Flow<User?> = flow {
    val userId = authSessionPersistence.withKey { readUserId(it) }
    if (userId == null) return@flow emit(null)
    inMemoryDB.userQueries
      .selectById(userId.value, ::mapToUser)
      .observeOneOrNull()
      .collect { emit(it) }
  }

  override val branch: Flow<Branch?> = inMemoryDB.chatBranchQueries
    .selectById(branchId.value, ::mapToBranch)
    .observeOneOrNull()

  override val hasCommitsHistory: Flow<Boolean> = hasCommitsHistoryCache

  override val commits: Flow<List<Commit>> = flow {
    val conversationId = requireConversationId()
    val selfId = requireUserId()

    val commitsFlow = inMemoryDB.chatCommitQueries
      .selectByBranchId(conversationId, branchId.value, ::mapToCommit)
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
    firestore.branchUnreadCountLive(
      branchId = branchId.value,
      conversationId = conversationId
    ).collect { emit(it) }
  }

  override val members: Flow<List<Member>> = flow {
    val conversationId = requireConversationId()
    inMemoryDB.chatConversationMemberQueries
      .selectByConversation(conversationId, ::mapToMember)
      .observeList()
      .collect { emit(it) }
  }

  override fun member(id: UserId): Flow<Member?> = flow {
    val conversationId = requireConversationId()
    inMemoryDB.chatConversationMemberQueries
      .selectByConversationAndId(conversationId, id.value, ::mapToMember)
      .observeOneOrNull()
      .collect { emit(it) }
  }

  /**
   * Read-watermark пира на уровне conversation (read-receipts общие для всей переписки,
   * включая ветки). Branch direct-only — участников ровно двое, пир тот, чей id != self.
   */
  private fun peerReadAt(conversationId: String, selfId: UserId): Flow<LocalDateTime?> = flow {
    val peerId = withContext(Dispatchers.IO) {
      inMemoryDB.chatConversationMemberQueries
        .selectByConversation(conversationId, ::mapToMember)
        .executeAsList()
        .firstOrNull { it.id.value != selfId.value }
        ?.id
        ?.value
    }
    if (peerId != null) {
      firestore.memberLive(
        conversationId = conversationId,
        memberId = peerId
      ).map { member ->
        member?.lastReadAt
          ?.toEpochMillis()
          ?.toLocalDateTime()
      }.collect {
        emit(it)
      }
    } else {
      emit(null)
    }
  }

  private fun memberUids(conversationId: String): List<String> {
    return inMemoryDB.chatConversationMemberQueries
      .selectByConversation(conversationId, ::mapToMember)
      .executeAsList()
      .map { it.id.value }
  }

  private suspend fun applyBranchChanges(conversationId: String, branch: BranchNM?) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.transaction {
        if (branch == null) {
          inMemoryDB.chatBranchQueries.deleteById(branchId.value)
        } else {
          applyInsertOrReplaceBranch(branch.toDomain(conversationId))
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

  private suspend fun applyUpdateUnreadCount(branchId: String, unreadCount: Long) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatBranchQueries.updateUnreadCount(
        id = branchId,
        unreadCount = unreadCount
      )
    }
  }

  private suspend fun applyInsertOrReplaceCommits(conversationId: String, commits: List<CommitNM>) {
    return withContext(Dispatchers.IO) {
      val userId = requireUserId()
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

  private suspend fun applyCommitChanges(
    conversationId: String,
    userId: UserId,
    changes: List<FirestoreChange<CommitNM>>
  ) {
    return withContext(Dispatchers.IO) {
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

  private fun requireConversationId(): String {
    return requireNotNull(
      inMemoryDB.chatBranchQueries
        .selectById(branchId.value, ::mapToBranch)
        .executeAsOneOrNull()
        ?.conversationId
        ?.value
    ) {
      "conversationId not found for branch ${branchId.value}"
    }
  }

  private suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) })
  }
}

/** Денормализованные поля для удаления «у всех», посчитанные по полному кэшу до удаления. */
private data class DeleteForEveryoneWrite(
  val peerId: Peer.Id,
  val lastCommit: LastCommitParams,
  val peerUnreadDelta: Int
)

private const val LATEST_PAGE_SIZE = 50
private const val HISTORY_PAGE_SIZE = 30
