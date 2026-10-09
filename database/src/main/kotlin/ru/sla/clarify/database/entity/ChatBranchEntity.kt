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
  tableName = "ChatBranch",
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
      name = "ChatBranch_conversationId"
    )
  ]
)
data class ChatBranchEntity(
  @PrimaryKey
  val id: Branch.Id,
  val conversationId: Conversation.Id,
  val parentBranchId: Branch.Id,
  val branchedFromCommitId: Commit.Id,
  val name: String,
  val lastCommit: String?,
  val lastCommitTimestamp: Long,
  @ColumnInfo(defaultValue = "0")
  val unreadCount: Long,
  val createdAt: Long,
  val createdById: UserId
)
