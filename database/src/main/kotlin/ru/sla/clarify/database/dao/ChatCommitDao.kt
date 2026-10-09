package ru.sla.clarify.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import ru.sla.clarify.database.DistinctFlow
import ru.sla.clarify.database.entity.ChatCommitEntity
import ru.sla.clarify.database.entity.ChronologyCommitRow
import ru.sla.clarify.database.entity.CommitCursorRow
import ru.sla.clarify.database.entity.CommitEditStateRow
import ru.sla.clarify.database.entity.CommitRow
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation

@Dao
interface ChatCommitDao {

  @Query(
    """
    SELECT
      id,
      senderId,
      type,
      text,
      replyCommit,
      invitedId,
      createdAtNanos,
      isSelf,
      status,
      editedAtNanos
    FROM ChatCommit
    WHERE conversationId = :conversationId AND branchId = :branchId
    ORDER BY createdAtNanos DESC, id DESC
    """
  )
  suspend fun select(conversationId: Conversation.Id, branchId: Branch.Id): List<CommitRow>

  @Query(
    """
    SELECT
      id,
      senderId,
      type,
      text,
      replyCommit,
      invitedId,
      createdAtNanos,
      isSelf,
      status,
      editedAtNanos
    FROM ChatCommit
    WHERE conversationId = :conversationId AND branchId = :branchId
    ORDER BY createdAtNanos DESC, id DESC
    """
  )
  fun observe(conversationId: Conversation.Id, branchId: Branch.Id): DistinctFlow<List<CommitRow>>

  /**
   * Коммит, у которого ещё нет серверного времени создания, хранится с `createdAtNanos = 0` и в
   * хронологию не попадает: без времени ему нет места на оси.
   */
  @Query(
    """
    SELECT
      id,
      branchId,
      senderId,
      type,
      text,
      replyCommit,
      invitedId,
      createdAtNanos,
      isSelf,
      status,
      editedAtNanos
    FROM ChatCommit
    WHERE conversationId = :conversationId AND createdAtNanos > 0
    ORDER BY createdAtNanos ASC, id ASC
    """
  )
  fun observeByConversation(conversationId: Conversation.Id): DistinctFlow<List<ChronologyCommitRow>>

  @Query(
    """
    SELECT
      id,
      createdAtNanos
    FROM ChatCommit
    WHERE conversationId = :conversationId AND branchId = :branchId AND createdAtNanos > 0
    ORDER BY createdAtNanos ASC, id ASC
    LIMIT 1
    """
  )
  suspend fun selectOldestCursor(conversationId: Conversation.Id, branchId: Branch.Id): CommitCursorRow?

  @Query(
    """
    SELECT
      id,
      createdAtNanos
    FROM ChatCommit
    WHERE conversationId = :conversationId AND branchId = :branchId AND createdAtNanos > 0
    ORDER BY createdAtNanos ASC, id ASC
    LIMIT 1
    """
  )
  fun observeOldestCursor(
    conversationId: Conversation.Id,
    branchId: Branch.Id
  ): DistinctFlow<CommitCursorRow?>

  @Query(
    """
    SELECT
      text,
      editedAtNanos,
      status
    FROM ChatCommit
    WHERE id = :id
    """
  )
  suspend fun selectEditState(id: Commit.Id): CommitEditStateRow?

  @Query(
    """
    SELECT *
    FROM ChatCommit
    WHERE id IN (:ids)
    """
  )
  suspend fun selectByIds(ids: List<Commit.Id>): List<ChatCommitEntity>
}
