package ru.sla.clarify.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import ru.sla.clarify.database.DistinctFlow
import ru.sla.clarify.database.entity.DirectConversationRow
import ru.sla.clarify.database.entity.GroupConversationRow
import ru.sla.clarify.database.entity.GroupRow
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Member

@Dao
interface ChatConversationDao {

  @Query(
    """
    SELECT
      ChatConversation.id,
      ChatConversation.type,
      ChatConversation.lastCommit,
      ChatConversation.lastCommitTimestamp,
      ChatConversation.unreadCount,
      ChatMember.id AS peerId,
      User.displayName AS peerDisplayName,
      User.photoUrl AS peerPhotoUrl
    FROM ChatConversation
    INNER JOIN ChatMember
      ON ChatMember.conversationId = ChatConversation.id AND ChatMember.id != :currentUserId
    INNER JOIN User
      ON User.id = ChatMember.id
    WHERE ChatConversation.type = 'direct'
    ORDER BY ChatConversation.lastCommitTimestamp DESC
    """
  )
  fun observeDirects(currentUserId: Member.Id): DistinctFlow<List<DirectConversationRow>>

  @Query(
    """
    SELECT
      ChatConversation.id,
      ChatConversation.name,
      ChatConversation.ownerId,
      ChatConversation.lastCommit,
      ChatConversation.lastCommitSenderId,
      ChatConversation.lastCommitTimestamp,
      ChatConversation.unreadCount,
      (SELECT COUNT(*) FROM ChatMember WHERE conversationId = ChatConversation.id) AS memberCount,
      User.displayName AS lastCommitSenderDisplayName
    FROM ChatConversation
    LEFT JOIN User
      ON User.id = ChatConversation.lastCommitSenderId
    WHERE ChatConversation.type = 'group'
    ORDER BY ChatConversation.lastCommitTimestamp DESC
    """
  )
  fun observeGroups(): DistinctFlow<List<GroupConversationRow>>

  @Query(
    """
    SELECT
      id,
      name,
      ownerId,
      lastCommit,
      lastCommitSenderId,
      lastCommitTimestamp,
      unreadCount,
      (SELECT COUNT(*) FROM ChatMember WHERE conversationId = ChatConversation.id) AS memberCount
    FROM ChatConversation
    WHERE id = :id AND type = 'group'
    """
  )
  fun observeGroup(id: Conversation.Id): DistinctFlow<GroupRow?>

  @Query(
    """
    SELECT
      id
    FROM ChatConversation
    """
  )
  fun observeIds(): DistinctFlow<List<Conversation.Id>>

  @Query(
    """
    SELECT
      ChatConversation.id
    FROM ChatConversation
    INNER JOIN ChatMember
      ON ChatMember.conversationId = ChatConversation.id
    WHERE ChatConversation.type = :type
    GROUP BY ChatConversation.id
    HAVING COUNT(*) = :memberCount
      AND COUNT(CASE WHEN ChatMember.id IN (:memberIds) THEN 1 END) = :memberCount
    """
  )
  fun observeIdByMembers(
    type: String,
    memberIds: List<Member.Id>,
    memberCount: Long
  ): DistinctFlow<Conversation.Id?>

  @Query(
    """
    SELECT
      ChatConversation.id
    FROM ChatConversation
    INNER JOIN ChatMember
      ON ChatMember.conversationId = ChatConversation.id
    WHERE ChatConversation.type = :type
    GROUP BY ChatConversation.id
    HAVING COUNT(*) = :memberCount
      AND COUNT(CASE WHEN ChatMember.id IN (:memberIds) THEN 1 END) = :memberCount
    """
  )
  suspend fun selectIdByMembers(
    type: String,
    memberIds: List<Member.Id>,
    memberCount: Long
  ): Conversation.Id?
}
