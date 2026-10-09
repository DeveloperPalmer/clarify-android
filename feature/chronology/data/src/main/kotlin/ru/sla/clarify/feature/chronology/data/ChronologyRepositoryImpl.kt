package ru.sla.clarify.feature.chronology.data

import androidx.room3.withWriteTransaction
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.chat.api.CommitApi
import ru.sla.clarify.chat.api.UnreadCountApi
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.ChatDatabase
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.CommitRecord
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.ConversationRecord
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.feature.chronology.data.mapper.toBranchCommit
import ru.sla.clarify.feature.chronology.domain.ChronologyRepository
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope
import ru.sla.clarify.feature.chronology.domain.entity.BaselineHistory
import ru.sla.clarify.feature.chronology.domain.entity.BranchHistory
import ru.sla.clarify.feature.chronology.domain.entity.ChronologyHistory
import ru.sla.clarify.feature.chronology.domain.entity.TargetParams
import ru.sla.clarify.mapper.data.toCacheRow
import ru.sla.clarify.mapper.data.toDomainModel
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
  private val commitApi: CommitApi,
  private val unreadCountApi: UnreadCountApi,
  private val chatDatabase: ChatDatabase,
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
    chatDatabase.chatBranchDao()
      .observeIds(conversationId)
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

    val branchesFlow = chatDatabase.chatBranchDao()
      .observeByConversation(conversationId)
      .map { rows -> rows.map { it.toDomainModel() } }

    val commitsFlow = chatDatabase.chatCommitDao()
      .observeByConversation(conversationId)
      .map { rows -> rows.map { it.toBranchCommit() }.groupBy({ it.first }, { it.second }) }

    val membersFlow = chatDatabase.chatMemberDao()
      .observeDirect(conversationId)
      .map { rows -> rows.map { it.toDomainModel() } }

    // Непрочитанное магистрали — счётчик беседы, тот же, что показывает чат: граф прочитанность
    // показывает, а не считает (решение владельца, журнал, итерация 39, п. 1).
    //
    // Нулём вперёд, потому что `combine` ждёт первого значения от каждого потока: без этого граф
    // целиком стоял бы до первого снимка Firestore ради бейджа, которого может и не быть.
    val baselineUnreadFlow = unreadCountApi.unreadCountLive(conversationId.value).onStart { emit(0L) }

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
        commitApi.readCommits(
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
    val userId = requireUserId()
    chatDatabase.withWriteTransaction {
      commits.forEach { commit ->
        val row = commit.toCacheRow(
          conversationId = conversationId,
          selfUserId = userId,
          isPending = false
        )
        chatDatabase.chatCommitDao().insertOrReplace(row)
      }
    }
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
