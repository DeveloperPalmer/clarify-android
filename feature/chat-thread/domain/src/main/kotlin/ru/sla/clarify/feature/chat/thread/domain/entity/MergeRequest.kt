package ru.sla.clarify.feature.chat.thread.domain.entity

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.UserId

@Immutable
data class MergeRequest(
  val initiatorUid: UserId,
  val requestedAt: Long,
  val approvedByUids: Set<UserId>
)
