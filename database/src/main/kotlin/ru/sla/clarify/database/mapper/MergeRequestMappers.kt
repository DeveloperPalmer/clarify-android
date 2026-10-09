package ru.sla.clarify.database.mapper

import ru.sla.clarify.database.entity.MergeRequestEntity
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.BranchRecord

internal fun BranchRecord.MergeRequest.toCacheRow(branchId: Branch.Id): MergeRequestEntity {
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
