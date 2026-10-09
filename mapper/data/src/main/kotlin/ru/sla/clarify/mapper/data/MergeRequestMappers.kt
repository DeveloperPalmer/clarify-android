package ru.sla.clarify.mapper.data

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.entity.MergeRequestEntity
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.BranchRecord

fun mapToMergeRequest(
  status: String?,
  initiatorId: UserId?,
  requestedAt: Long?,
  approvedByIds: Set<UserId>?,
  mergedAt: Long?,
  mergedIntoBranchId: Branch.Id?
): Branch.MergeRequest? {
  if (status == null || initiatorId == null || requestedAt == null) return null
  return Branch.MergeRequest(
    status = Branch.MergeRequest.Status.fromValue(status),
    initiatorId = initiatorId,
    requestedAt = requestedAt,
    approvedByIds = approvedByIds.orEmpty(),
    mergedAt = mergedAt,
    mergedIntoBranchId = mergedIntoBranchId
  )
}

fun BranchRecord.MergeRequest.toCacheRow(branchId: Branch.Id): MergeRequestEntity {
  return MergeRequestEntity(
    branchId = branchId,
    status = status.value,
    initiatorId = initiatorId,
    requestedAt = requestedAtSeconds,
    approvedByIds = approvedByIds,
    mergedAt = mergedAtSeconds,
    mergedIntoBranchId = mergedIntoBranchId
  )
}
