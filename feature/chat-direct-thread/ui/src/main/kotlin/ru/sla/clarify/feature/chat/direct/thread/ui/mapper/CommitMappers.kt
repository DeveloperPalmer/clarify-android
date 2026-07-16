package ru.sla.clarify.feature.chat.direct.thread.ui.mapper

import ru.sla.clarify.core.domain.date.TIME_FORMATTER_HOUR_MINUTE
import ru.sla.clarify.uikit.component.bubble.BubbleMessage
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.clarify.entity.chat.Commit as DomainCommit

internal fun List<DomainCommit>.toUiCommits(): List<Commit> {
  val commits = this
  return commits.mapIndexedNotNull { index, commit ->
    when (commit) {
      is DomainCommit.Message -> {
        Commit.Message(
          source = commit,
          key = "message:${commit.id.value}",
          bubble = BubbleMessage(
            id = BubbleMessage.Id(commit.id.value),
            type = bubbleType(index, commits),
            side = commit.side(),
            text = commit.text,
            time = commit.timestamp.format(TIME_FORMATTER_HOUR_MINUTE),
            sender = null,
            selection = BubbleMessage.Selection(
              inSelectionMode = false,
              isSelected = false
            )
          )
        )
      }
      is DomainCommit.InviteMember -> null
    }
  }
}

internal fun DomainCommit.updateSelection(
  inSelectionMode: Boolean,
  selectedIds: Set<DomainCommit.Id>
): BubbleMessage.Selection {
  return BubbleMessage.Selection(
    isSelected = id in selectedIds,
    inSelectionMode = inSelectionMode
  )
}

private fun DomainCommit.side(): BubbleMessage.Side {
  return if (isSelf) {
    BubbleMessage.Side.Right(status.toReadStatus())
  } else {
    BubbleMessage.Side.Left
  }
}

private fun DomainCommit.Status.toReadStatus(): BubbleMessage.ReadStatus {
  return when (this) {
    DomainCommit.Status.Sending -> BubbleMessage.ReadStatus.Sending
    DomainCommit.Status.Sent -> BubbleMessage.ReadStatus.Sent
    DomainCommit.Status.Read -> BubbleMessage.ReadStatus.Read
  }
}

private fun bubbleType(
  index: Int,
  commits: List<DomainCommit>
): BubbleMessage.Type {
  // Список отсортирован от новых к старым (репозиторий делает asReversed) и рендерится
  // reverseLayout-ом снизу вверх, поэтому index-1 — визуально ниже, index+1 — визуально выше.
  val current = commits[index] as? DomainCommit.Message ?: return BubbleMessage.Type.Top
  val below = (commits.getOrNull(index - 1) as? DomainCommit.Message)
  val above = (commits.getOrNull(index + 1) as? DomainCommit.Message)
  val sameSenderBelow = below?.senderId == current.senderId
  val sameSenderAbove = above?.senderId == current.senderId
  return when {
    sameSenderAbove && sameSenderBelow -> BubbleMessage.Type.Middle
    sameSenderBelow -> BubbleMessage.Type.Top
    sameSenderAbove -> BubbleMessage.Type.Bottom
    else -> BubbleMessage.Type.Top
  }
}
