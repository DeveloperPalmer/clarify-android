package ru.sla.clarify.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Conversation

@Entity(tableName = "ChatConversation")
data class ChatConversationEntity(
  @PrimaryKey
  val id: Conversation.Id,
  val type: String,
  val name: String?,
  val ownerId: UserId?,
  val lastCommit: String?,
  val lastCommitSenderId: UserId?,
  val lastCommitTimestamp: Long,
  @ColumnInfo(defaultValue = "0")
  val unreadCount: Long
)
