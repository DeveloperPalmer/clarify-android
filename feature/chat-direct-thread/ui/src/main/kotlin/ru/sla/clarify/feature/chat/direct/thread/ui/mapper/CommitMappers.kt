package ru.sla.clarify.feature.chat.direct.thread.ui.mapper

import ru.sla.clarify.uikit.component.bubble.BubbleMessage
import ru.sla.clarify.entity.chat.Commit as DomainCommit

internal fun DomainCommit.updateSelection(
  inSelectionMode: Boolean,
  selectedIds: Set<DomainCommit.Id>
): BubbleMessage.Selection {
  return BubbleMessage.Selection(
    isSelected = id in selectedIds,
    inSelectionMode = inSelectionMode
  )
}
