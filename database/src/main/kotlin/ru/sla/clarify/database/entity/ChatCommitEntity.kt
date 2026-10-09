package ru.sla.clarify.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation

@Entity(
  tableName = "ChatCommit",
  foreignKeys = [
    ForeignKey(
      entity = ChatConversationEntity::class,
      parentColumns = ["id"],
      childColumns = ["conversationId"],
      onDelete = ForeignKey.CASCADE,
      onUpdate = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(
      value = ["conversationId"],
      name = "ChatCommit_conversationId"
    ),
    Index(
      value = ["conversationId", "branchId", "createdAtNanos"],
      name = "ChatCommit_branchId"
    )
  ]
)
data class ChatCommitEntity(
  @PrimaryKey
  val id: Commit.Id,
  val conversationId: Conversation.Id,
  val branchId: Branch.Id,
  val senderId: UserId,
  @ColumnInfo(defaultValue = "'text'")
  val type: String,
  val text: String,
  val replyCommit: Commit.Reply?,
  val invitedId: UserId?,
  val createdAtNanos: Long,
  val isSelf: Boolean,
  val status: String,
  val editedAtNanos: Long?
)
