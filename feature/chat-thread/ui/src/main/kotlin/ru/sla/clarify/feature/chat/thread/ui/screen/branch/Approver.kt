package ru.sla.clarify.feature.chat.thread.ui.screen.branch

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.UserId

/**
 * UI-проекция participant'а на конкретный merge request: тот же participant
 * показывается на разных ветках с разным [isApproved]. Собирается в ViewModel
 * пересечением `participants` × `mergeRequest.approvedByUids` — Composable
 * получает уже готовые цвета индикаторов.
 */
@Immutable
data class Approver(
  val userId: UserId,
  val displayName: String?,
  val photoUrl: String?,
  val isApproved: Boolean
)
