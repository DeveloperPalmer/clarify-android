package ru.sla.clarify.database

import androidx.room3.withWriteTransaction
import ru.sla.clarify.database.mapper.toCacheRow
import ru.sla.clarify.entity.chat.BranchRecord

/**
 * Записывает ветку вместе с её запросом слияния: запрос есть — строка заменяется, нет — удаляется.
 * Обе записи в одной транзакции, чтобы подписчик не увидел ветку со старым запросом слияния.
 */
suspend fun ChatDatabase.upsertBranch(branch: BranchRecord) {
  withWriteTransaction {
    chatBranchDao().upsert(
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
      mergeRequestDao().insertOrReplace(mergeRequest.toCacheRow(branch.id))
    } else {
      mergeRequestDao().delete(branch.id)
    }
  }
}
