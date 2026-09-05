package ru.sla.clarify.chat.api

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.entity.chat.ChatChange
import ru.sla.clarify.entity.chat.CommitCursor
import ru.sla.clarify.entity.chat.CommitRecord
import ru.sla.clarify.entity.chat.LastCommitUpdate

/**
 * Сообщения: чтение страницами, живые потоки, отправка, правка, скрытие и удаление.
 *
 * Тема из `wiki/method/`: интерфейс отделён от остальных, потому что им не нужно знать
 * друг о друге, и это же деление даёт имя операции в спеке.
 */
interface CommitApi {

  suspend fun deleteDirectCommits(
    conversationId: String,
    peerId: String,
    commitIds: List<String>,
    lastCommit: LastCommitUpdate,
    peerUnreadDelta: Int
  )

  suspend fun deleteBranchCommits(
    conversationId: String,
    branchId: String,
    peerId: String,
    commitIds: List<String>,
    lastCommit: LastCommitUpdate,
    peerUnreadDelta: Int
  )

  suspend fun hideCommits(conversationId: String, commitIds: List<String>)

  fun commitsLive(
    conversationId: String,
    branchId: String,
    limit: Long
  ): Flow<List<ChatChange<CommitRecord>>>

  suspend fun readCommits(
    conversationId: String,
    branchId: String,
    limit: Long,
    before: CommitCursor?,
    fromServerOnly: Boolean = false
  ): List<CommitRecord>

  fun directCommitsLive(
    branchId: String,
    peerId: String,
    from: CommitCursor?
  ): Flow<List<ChatChange<CommitRecord>>>

  fun groupCommitsLive(
    conversationId: String,
    limit: Long
  ): Flow<List<ChatChange<CommitRecord>>>

  suspend fun createBranchCommit(
    conversationId: String,
    branchId: String,
    text: String,
    memberUids: List<String>,
    replyCommit: CommitRecord.Reply?
  )

  suspend fun createDirectCommit(
    peerId: String,
    branchId: String?,
    conversationId: String?,
    text: String,
    replyCommit: CommitRecord.Reply?
  )

  suspend fun updateDirectCommit(
    conversationId: String,
    commitId: String,
    text: String
  )

  suspend fun updateBranchCommit(
    conversationId: String,
    branchId: String,
    commitId: String,
    text: String
  )

  suspend fun createGroupCommit(
    conversationId: String,
    text: String,
    memberUids: List<String>
  )

  fun commitsLive(
    conversationId: String,
    branchId: String,
    from: CommitCursor?
  ): Flow<List<ChatChange<CommitRecord>>>
}
