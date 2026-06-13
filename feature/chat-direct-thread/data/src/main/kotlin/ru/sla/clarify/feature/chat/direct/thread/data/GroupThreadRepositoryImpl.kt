package ru.sla.clarify.feature.chat.direct.thread.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.PersistedDB
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.generateColorHex
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.mapToCommit
import ru.sla.clarify.feature.chat.direct.thread.domain.GroupThreadRepository
import ru.sla.clarify.feature.chat.direct.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.ThreadTarget
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.FirestoreChange
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.clarify.lib.google.firestore.toEpochSeconds
import java.time.LocalDateTime
import javax.inject.Inject

@SingleIn(ThreadScope::class)
@ContributesBinding(ThreadScope::class)
class GroupThreadRepositoryImpl @Inject constructor(
  target: ThreadTarget,
  private val firestore: Firestore,
  private val persistedDB: PersistedDB,
  private val authSessionPersistence: AuthSessionPersistence
) : GroupThreadRepository {

  private val conversationId = (target as ThreadTarget.Group).conversationId

  private var lastReadWatermark: LocalDateTime? = null

  override suspend fun subscribeOnCommitChanges() {
    val userId = requireUserId()
    firestore.groupCommitsLive(
      conversationId = conversationId.value,
      limit = LIVE_COMMIT_LIMIT
    ).flowOn(
      context = Dispatchers.IO
    ).collect { changes ->
      applyCommitChanges(userId = userId, changes = changes)
    }
  }

  override suspend fun fetchHistoryCommits(count: Int, before: Commit?) {
    val historyCommits = firestore.getCommits(
      conversationId = conversationId.value,
      branchId = conversationId.value,
      count = count,
      before = before?.timestamp
    )
    val userId = requireUserId()
    withContext(Dispatchers.IO) {
      persistedDB.transaction {
        historyCommits.forEach { commit ->
          applyInsertOrReplaceCommit(
            commit = commit,
            userId = userId,
            hasPendingWrites = false
          )
        }
      }
    }
  }

  override suspend fun sendCommit(text: String) {
    val participantUids = persistedDB.chatConversationQueries
      .selectGroupById(
        id = conversationId.value,
        mapper = { _, _, _, participantUids, _, _, _, _, _ -> participantUids }
      )
      .executeAsOneOrNull()
      .orEmpty()
    firestore.postGroupCommit(
      conversationId = conversationId.value,
      text = text,
      colorHex = generateColorHex(),
      participantUids = participantUids
    )
  }

  override suspend fun markReadUpTo(lastReadAt: LocalDateTime) {
    val current = lastReadWatermark
    if (current != null && !lastReadAt.isAfter(current)) return
    lastReadWatermark = lastReadAt
    firestore.patchReadWatermark(conversationId.value, lastReadAt)
    firestore.patchClearUnreadCount(conversationId.value)
  }

  override val commits: Flow<List<Commit>> = flow {
    persistedDB.chatCommitQueries
      .selectByBranchId(conversationId.value, conversationId.value, ::mapToCommit)
      .observeList()
      .collect { emit(it) }
  }

  override val unreadCount: Flow<Long> = flow {
    firestore.unreadCountLive(conversationId.value)
      .collect { emit(it) }
  }

  private suspend fun applyCommitChanges(
    userId: UserId,
    changes: List<FirestoreChange<CommitNM>>
  ): Unit = withContext(Dispatchers.IO) {
    persistedDB.transaction {
      changes.forEach { change ->
        when (change.changeType) {
          FirestoreDocumentResult.Removed -> {
            persistedDB.chatCommitQueries.deleteById(change.data.id)
          }
          FirestoreDocumentResult.Added,
          FirestoreDocumentResult.Modified -> {
            applyInsertOrReplaceCommit(
              commit = change.data,
              userId = userId,
              hasPendingWrites = change.hasPendingWrites
            )
          }
        }
      }
    }
  }

  private fun applyInsertOrReplaceCommit(
    commit: CommitNM,
    userId: UserId,
    hasPendingWrites: Boolean
  ) {
    persistedDB.chatCommitQueries.insertOrReplace(
      id = commit.id,
      conversationId = conversationId.value,
      branchId = commit.branchId,
      senderId = commit.senderUid,
      type = commit.type.value,
      text = commit.text.orEmpty(),
      invitedUid = commit.invitedUid,
      colorHex = commit.colorHex.orEmpty(),
      timestamp = commit.createdAt?.toEpochSeconds() ?: 0L,
      isSelf = commit.senderUid == userId.value,
      status = if (hasPendingWrites) {
        Commit.Status.Sending.value
      } else {
        Commit.Status.Sent.value
      }
    )
  }

  private suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) })
  }
}

private const val LIVE_COMMIT_LIMIT = 30L
