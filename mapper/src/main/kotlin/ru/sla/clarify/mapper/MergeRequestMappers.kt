package ru.sla.clarify.mapper

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.adapter.StringList
import ru.sla.clarify.entity.chat.Branch

fun mapToMergeRequest(
  status: String?,
  initiatorUid: String?,
  requestedAt: Long?,
  approvedByUids: StringList?,
  mergedAt: Long?,
  mergedIntoBranchId: String?
): Branch.MergeRequest? {
  if (status == null || initiatorUid == null || requestedAt == null) return null
  return Branch.MergeRequest(
    status = Branch.MergeRequest.Status.fromValue(status),
    initiatorId = UserId(initiatorUid),
    requestedAt = requestedAt,
    approvedByIds = approvedByUids.orEmpty().map(::UserId).toSet(),
    mergedAt = mergedAt,
    mergedIntoBranchId = mergedIntoBranchId?.let(Branch::Id)
  )
}
