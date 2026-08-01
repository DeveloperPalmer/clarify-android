package ru.sla.clarify.feature.chat.branch.data

import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.chat.ChatCommit
import ru.sla.clarify.database.chat.SelectOldestCursor
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.lib.google.firestore.entity.CommitCursor
import ru.sla.clarify.lib.google.firestore.epochNanosToTimestamp
import ru.sla.clarify.lib.google.firestore.toEpochNanos

/**
 * Доступ к закэшированным коммитам одной ветки: пагинационный курсор и точечные правки строк,
 * которыми репозиторий обслуживает оптимистичные редактирование и удаление.
 */
internal class BranchCommitCache(
  private val inMemoryDB: InMemoryDB,
  private val branchId: Branch.Id
) {

  /** Курсор самого старого закэшированного коммита — с него страничится история из Firestore. */
  fun oldestCursor(conversationId: String): Flow<CommitCursor?> {
    return inMemoryDB.chatCommitQueries
      .selectOldestCursor(conversationId, branchId.value)
      .observeOneOrNull()
      .map { oldest -> oldest?.toCursor() }
  }

  suspend fun readOldestCursor(conversationId: String): CommitCursor? {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries
        .selectOldestCursor(conversationId, branchId.value)
        .executeAsOneOrNull()
        ?.toCursor()
    }
  }

  suspend fun readEditState(id: Commit.Id): EditState? {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries
        .selectEditStateById(id.value)
        .executeAsOneOrNull()
        ?.let { EditState(text = it.text, editedAtNanos = it.editedAtNanos, status = it.status) }
    }
  }

  suspend fun applyEdit(id: Commit.Id, text: String, status: Commit.Status) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries.updateEdit(
        id = id.value,
        text = text,
        editedAtNanos = Timestamp.now().toEpochNanos(),
        status = status.value
      )
    }
  }

  suspend fun revertEdit(id: Commit.Id, previous: EditState) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries.updateEdit(
        id = id.value,
        text = previous.text,
        editedAtNanos = previous.editedAtNanos,
        status = previous.status
      )
    }
  }

  suspend fun readCommits(ids: List<Commit.Id>): List<ChatCommit> {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatCommitQueries
        .selectByIds(ids.map { it.value })
        .executeAsList()
    }
  }

  suspend fun deleteCommits(ids: List<Commit.Id>) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.transaction {
        ids.forEach { inMemoryDB.chatCommitQueries.deleteById(it.value) }
      }
    }
  }

  suspend fun restoreCommits(commits: List<ChatCommit>) {
    if (commits.isEmpty()) {
      return
    }
    return withContext(Dispatchers.IO) {
      inMemoryDB.transaction {
        commits.forEach { inMemoryDB.chatCommitQueries.insertOrReplace(it) }
      }
    }
  }

  /** Снимок редактируемых полей строки кэша для отката оптимистичной правки при ошибке записи. */
  data class EditState(
    val text: String,
    val editedAtNanos: Long?,
    val status: String
  )
}

private fun SelectOldestCursor.toCursor(): CommitCursor {
  return CommitCursor(
    id = id,
    createdAt = createdAtNanos.epochNanosToTimestamp()
  )
}
