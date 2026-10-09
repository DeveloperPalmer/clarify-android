package ru.sla.clarify.database.entity

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation

/**
 * Поля `mergeRequest…` приходят из `LEFT JOIN` и nullable все сразу: у ветки без запроса слияния
 * пусты все шесть. Собирать из них запрос слияния — дело маппера.
 */
data class BranchRow(
  val id: Branch.Id,
  val conversationId: Conversation.Id,
  val parentBranchId: Branch.Id,
  val branchedFromCommitId: Commit.Id,
  val name: String,
  val lastCommit: String?,
  val lastCommitTimestamp: Long,
  val unreadCount: Long,
  val createdAt: Long,
  val createdById: UserId,
  val mergeRequestStatus: String?,
  val mergeRequestInitiatorId: UserId?,
  val mergeRequestRequestedAt: Long?,
  val mergeRequestApprovedByIds: Set<UserId>?,
  val mergeRequestMergedAt: Long?,
  val mergeRequestMergedIntoBranchId: Branch.Id?
)
