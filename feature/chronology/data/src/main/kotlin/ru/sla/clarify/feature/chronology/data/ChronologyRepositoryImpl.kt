package ru.sla.clarify.feature.chronology.data

import app.cash.sqldelight.Query
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.CommitRecord
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.ConversationRecord
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.feature.chronology.data.mapper.mapToBranchCommit
import ru.sla.clarify.feature.chronology.domain.ChronologyRepository
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope
import ru.sla.clarify.feature.chronology.domain.entity.BaselineHistory
import ru.sla.clarify.feature.chronology.domain.entity.BranchHistory
import ru.sla.clarify.feature.chronology.domain.entity.ChronologyHistory
import ru.sla.clarify.feature.chronology.domain.entity.TargetParams
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.mapper.data.mapToBranch
import ru.sla.clarify.mapper.data.mapToMember
import ru.sla.clarify.mapper.data.toCacheRow
import ru.sla.log.log
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * История беседы для графа: ветки и их ленты из кэша, недостающие ленты — из сети.
 *
 * **Своих подписок на изменения здесь ровно одна.** Ветки и лента магистрали приезжают в кэш
 * слушателями личного треда — хронология открывается только из него, — и повторять их значило бы
 * платить за тот же трафик дважды и расходиться с чатом на кадрах между двумя источниками. А вот
 * ленты веток в кэш не кладёт никто, пока ветку не открыли: их эта реализация и догружает.
 */
@SingleIn(ChronologyScope::class)
@ContributesBinding(ChronologyScope::class)
class ChronologyRepositoryImpl @Inject constructor(
  params: TargetParams,
  private val firestore: Firestore,
  private val inMemoryDB: InMemoryDB,
  private val authSessionPersistence: AuthSessionPersistence
) : ChronologyRepository {

  private val peerId = params.peerId

  // Ветки, чья лента уже догружена. Читается и пишется единственным сборщиком
  // [subscribeOnBranchCommits], поэтому обычное множество: гонки за него нет по построению.
  private val fetchedBranches = mutableSetOf<Branch.Id>()

  override suspend fun subscribeOnBranchCommits() {
    val conversationId = awaitConversationId()
    // За списком веток, а не по нему один раз: на холодном старте хронология успевает открыться
    // раньше, чем приедет первый снимок веток беседы, и однократное чтение застало бы пустоту.
    inMemoryDB.chatBranchQueries
      .selectIds(conversationId)
      .observeList()
      .collect { branchIds ->
        applyFetchMissingCommits(
          conversationId = conversationId,
          branchIds = branchIds
        )
      }
  }

  override val history: Flow<ChronologyHistory> = flow {
    val conversationId = awaitConversationId()
    val baselineId = Branch.Id(conversationId.value)

    val branchesFlow = inMemoryDB.chatBranchQueries
      .selectByConversation(conversationId, ::mapToBranch)
      .observeList()

    val commitsFlow = inMemoryDB.chatCommitQueries
      .selectByConversation(conversationId, ::mapToBranchCommit)
      .observeList()
      .map { rows -> rows.groupBy({ it.first }, { it.second }) }

    val membersFlow = inMemoryDB.chatMemberQueries
      .selectDirect(conversationId, ::mapToMember)
      .observeList()

    // Непрочитанное магистрали — счётчик беседы, тот же, что показывает чат: граф прочитанность
    // показывает, а не считает (решение владельца, журнал, итерация 39, п. 1).
    //
    // Нулём вперёд, потому что `combine` ждёт первого значения от каждого потока: без этого граф
    // целиком стоял бы до первого снимка Firestore ради бейджа, которого может и не быть.
    val baselineUnreadFlow = firestore.unreadCountLive(conversationId.value).onStart { emit(0L) }

    combine(
      branchesFlow,
      commitsFlow,
      membersFlow,
      baselineUnreadFlow
    ) { branches, commitsByBranch, members, baselineUnreadCount ->
      ChronologyHistory(
        baseline = BaselineHistory(
          id = baselineId,
          commits = commitsByBranch[baselineId].orEmpty(),
          unreadCount = baselineUnreadCount
        ),
        branches = branches.map { branch ->
          BranchHistory(
            branch = branch,
            commits = commitsByBranch[branch.id].orEmpty()
          )
        },
        members = members
      )
    }.collect { emit(it) }
  }

  /**
   * Догружает ленты тех веток, которых ещё нет в кэше, — по одной странице на ветку.
   *
   * Страница, а не вся история: граф строится из того же объёма, что видит чат, а тянуть переписку
   * целиком ради обзорного экрана значило бы платить чтениями Firestore за каждое его открытие.
   *
   * Ветка помечается загруженной **после** успеха: иначе первая же сетевая ошибка вычеркнула бы её
   * из графа до конца сеанса, и молча — на экране это выглядит просто веткой без сообщений.
   */
  private suspend fun applyFetchMissingCommits(
    conversationId: Conversation.Id,
    branchIds: List<Branch.Id>
  ) {
    branchIds.filterNot { it in fetchedBranches }.forEach { branchId ->
      runSuspendCatching {
        firestore.readCommits(
          conversationId = conversationId.value,
          branchId = branchId.value,
          limit = BRANCH_PAGE_SIZE,
          before = null
        )
      }.onSuccess { commits ->
        applyInsertOrReplaceCommits(
          conversationId = conversationId,
          commits = commits
        )
        fetchedBranches += branchId
      }.onFailure { error ->
        log { "Chronology: failed to read commits of branch ${branchId.value}: $error" }
      }
    }
  }

  private suspend fun applyInsertOrReplaceCommits(
    conversationId: Conversation.Id,
    commits: List<CommitRecord>
  ) {
    return withContext(Dispatchers.IO) {
      val userId = requireUserId()
      inMemoryDB.transaction {
        commits.forEach { commit ->
          val row = commit.toCacheRow(
            conversationId = conversationId,
            selfUserId = userId,
            isPending = false
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
      }
    }
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

  private fun selectDirectConversationId(memberIds: List<Member.Id>): Query<Conversation.Id> {
    return inMemoryDB.chatConversationQueries.selectIdByMembers(
      memberIds = memberIds,
      memberCount = memberIds.size.toLong(),
      type = ConversationRecord.Type.Direct.value
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

// Сколько сообщений ветки тянуть в граф: столько же, сколько чат берёт первой страницей ленты.
private const val BRANCH_PAGE_SIZE = 50L
