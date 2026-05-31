package ru.sla.clarify.feature.chat.thread.ui.mapper

import ru.sla.clarify.core.domain.date.TIME_FORMATTER_HOUR_MINUTE
import ru.sla.clarify.feature.chat.thread.ui.entity.Commit
import ru.sla.clarify.uikit.component.bubble.BubbleMessage
import ru.sla.clarify.feature.entity.chat.Commit as DomainCommit

internal fun List<DomainCommit>.toUiCommits(): List<Commit> {
  val commits = this
  return commits.mapIndexed { index, commit ->
    when (commit) {
      is DomainCommit.Message -> Commit.Message(
        source = commit,
        bubble = BubbleMessage(
          id = BubbleMessage.Id(commit.id.value),
          type = bubbleType(index, commits),
          side = if (commit.isSelf) BubbleMessage.Side.Right() else BubbleMessage.Side.Left,
          text = commit.text,
          time = commit.timestamp.format(TIME_FORMATTER_HOUR_MINUTE)
        )
      )
    }
  }
}

private fun bubbleType(
  index: Int,
  commits: List<DomainCommit>
): BubbleMessage.Type {
  val current = commits[index] as? DomainCommit.Message ?: return BubbleMessage.Type.Top
  val older = commits.getOrNull(index - 1) as? DomainCommit.Message
  val newer = commits.getOrNull(index + 1) as? DomainCommit.Message
  val sameSenderOlder = older?.senderId == current.senderId
  val sameSenderNewer = newer?.senderId == current.senderId
  return when {
    sameSenderOlder && sameSenderNewer -> BubbleMessage.Type.Middle
    sameSenderNewer -> BubbleMessage.Type.Top
    sameSenderOlder -> BubbleMessage.Type.Bottom
    else -> BubbleMessage.Type.Top
  }
}
