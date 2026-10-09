package ru.sla.clarify.database.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Branch

@Entity(
  tableName = "MergeRequest",
  foreignKeys = [
    ForeignKey(
      entity = ChatBranchEntity::class,
      parentColumns = ["id"],
      childColumns = ["branchId"],
      onDelete = ForeignKey.CASCADE,
      onUpdate = ForeignKey.CASCADE
    )
  ]
)
data class MergeRequestEntity(
  @PrimaryKey
  val branchId: Branch.Id,
  val status: String,
  val initiatorId: UserId,
  val requestedAt: Long,
  val approvedByIds: Set<UserId>,
  val mergedAt: Long?,
  val mergedIntoBranchId: Branch.Id?
)
