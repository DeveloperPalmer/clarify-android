package ru.sla.clarify.database.entity

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit

data class ChronologyCommitRow(
  val id: Commit.Id,
  val branchId: Branch.Id,
  val senderId: UserId,
  val type: String,
  val text: String,
  val replyCommit: Commit.Reply?,
  val invitedId: UserId?,
  val createdAtNanos: Long,
  val isSelf: Boolean,
  val status: String,
  val editedAtNanos: Long?
)
