package ru.sla.clarify.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
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

  @Query(
    """
    SELECT
      EXISTS (SELECT 1 FROM ChatConversation WHERE id = :conversationId)
    """
  )
  suspend fun selectConversationExists(conversationId: Conversation.Id): Boolean

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrReplace(entity: ChatCommitEntity)

  /**
   * Пишет коммит, только пока его разговор есть в кэше. Разговор удаляют из кэша раньше, чем
   * отменяется живой слушатель ленты, и последний снимок может прийти уже без родителя: вставка
   * упала бы на внешнем ключе. Такой коммит больше некому показывать — он пропускается.
   */
  @Transaction
  suspend fun insertOrReplaceIfConversationExists(entity: ChatCommitEntity) {
    if (selectConversationExists(entity.conversationId)) {
      insertOrReplace(entity)
    }
  }

  @Query(
    """
    UPDATE ChatCommit
    SET
      text = :text,
      editedAtNanos = :editedAtNanos,
      status = :status
    WHERE id = :id
    """
  )
  suspend fun updateEdit(id: Commit.Id, text: String, editedAtNanos: Long?, status: String)

  @Query(
    """
    DELETE FROM ChatCommit
    WHERE id = :id
    """
  )
  suspend fun delete(id: Commit.Id)
}
