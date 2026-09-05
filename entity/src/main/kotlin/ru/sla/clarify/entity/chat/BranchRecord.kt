package ru.sla.clarify.entity.chat

import ru.sla.clarify.core.domain.entity.UserId

/**
 * Ветка такой, какой её отдаёт источник истины. В отличие от [Branch], который собирается для
 * экрана и знает про непрочитанное и про то, как показать время, здесь только пришедшее снаружи:
 * время в секундах эпохи и состояние merge request'а как оно записано.
 */
data class BranchRecord(
  val id: Branch.Id,
  val conversationId: Conversation.Id,
  val parentBranchId: Branch.Id,
  val branchedFromCommitId: Commit.Id,
  val name: String,
  val lastCommitText: String?,
  val lastCommitAtSeconds: Long,
  val createdAtSeconds: Long,
  val createdById: UserId,
  val mergeRequest: MergeRequest? = null
) {

  data class MergeRequest(
    val status: Branch.MergeRequest.Status,
    val initiatorId: UserId,
    val requestedAtSeconds: Long,
    val approvedByIds: Set<UserId>,
    val mergedAtSeconds: Long? = null,
    val mergedIntoBranchId: Branch.Id? = null
  )
}
