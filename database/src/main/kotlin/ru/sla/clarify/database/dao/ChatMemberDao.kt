package ru.sla.clarify.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import ru.sla.clarify.database.DistinctFlow
import ru.sla.clarify.database.entity.ChatMemberEntity
import ru.sla.clarify.database.entity.DirectMemberRow
import ru.sla.clarify.database.entity.GroupMemberRow
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Member

@Dao
interface ChatMemberDao {

  @Query(
    """
    SELECT DISTINCT
      ChatMember.id
    FROM ChatMember
    LEFT JOIN User
      ON User.id = ChatMember.id
    WHERE User.id IS NULL AND ChatMember.id != :currentUserId
    """
  )
  fun observeWithoutProfile(currentUserId: Member.Id): DistinctFlow<List<Member.Id>>

  @Query(
    """
    SELECT
      id
    FROM ChatMember
    WHERE conversationId = :conversationId
    """
  )
  suspend fun selectIds(conversationId: Conversation.Id): List<Member.Id>

  @Query(
    """
    SELECT
      ChatMember.id,
      User.displayName,
      User.email,
      User.photoUrl
    FROM ChatMember
    LEFT JOIN User
      ON User.id = ChatMember.id
    WHERE ChatMember.conversationId = :conversationId
    """
  )
  fun observeGroup(conversationId: Conversation.Id): DistinctFlow<List<GroupMemberRow>>

  @Query(
    """
    SELECT
      ChatMember.id,
      User.displayName,
      User.photoUrl
    FROM ChatMember
    LEFT JOIN User
      ON User.id = ChatMember.id
    WHERE ChatMember.conversationId = :conversationId
    """
  )
  suspend fun selectDirect(conversationId: Conversation.Id): List<DirectMemberRow>

  @Query(
    """
    SELECT
      ChatMember.id,
      User.displayName,
      User.photoUrl
    FROM ChatMember
    LEFT JOIN User
      ON User.id = ChatMember.id
    WHERE ChatMember.conversationId = :conversationId
    """
  )
  fun observeDirect(conversationId: Conversation.Id): DistinctFlow<List<DirectMemberRow>>

  @Query(
    """
    SELECT
      ChatMember.id,
      User.displayName,
      User.photoUrl
    FROM ChatMember
    LEFT JOIN User
      ON User.id = ChatMember.id
    WHERE ChatMember.conversationId = :conversationId AND ChatMember.id = :id
    """
  )
  fun observeDirectById(conversationId: Conversation.Id, id: Member.Id): DistinctFlow<DirectMemberRow?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrReplace(entity: ChatMemberEntity)

  @Query(
    """
    DELETE FROM ChatMember
    WHERE conversationId = :conversationId
    """
  )
  suspend fun delete(conversationId: Conversation.Id)

  @Query(
    """
    DELETE FROM ChatMember
    WHERE conversationId = :conversationId AND id NOT IN (:memberIds)
    """
  )
  suspend fun deleteExcept(conversationId: Conversation.Id, memberIds: List<Member.Id>)

  @Query(
    """
    DELETE FROM ChatMember
    WHERE conversationId = :conversationId AND id = :id
    """
  )
  suspend fun deleteById(conversationId: Conversation.Id, id: Member.Id)
}
