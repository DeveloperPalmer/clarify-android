package ru.sla.clarify.feature.chat.direct.thread.ui.mapper

import ru.sla.clarify.core.domain.date.TIME_FORMATTER_HOUR_MINUTE
import ru.sla.clarify.feature.chat.direct.thread.ui.entity.Commit
import ru.sla.clarify.uikit.component.bubble.BubbleMessage
import ru.sla.clarify.feature.entity.chat.Commit as DomainCommit

internal fun List<DomainCommit>.toUiCommits(): List<Commit> {
  val commits = this
  return commits.mapIndexedNotNull { index, commit ->
    when (commit) {
      is DomainCommit.Message -> {
        val bubbleType = bubbleType(
          index = index,
          commits = commits
        )
        Commit.Message(
          source = commit,
          key = "message:${commit.id.value}",
          bubble = BubbleMessage(
            id = BubbleMessage.Id(commit.id.value),
            type = bubbleType,
            side = commit.side(),
            text = commit.text,
            time = commit.timestamp.format(TIME_FORMATTER_HOUR_MINUTE),
            sender = commit.sender(bubbleType)
          )
        )
      }
      is DomainCommit.InviteMember -> null
    }
  }
}

private fun DomainCommit.sender(
  type: BubbleMessage.Type
): BubbleMessage.Sender? {
  return if (!isSelf && type == BubbleMessage.Type.Top) {
    BubbleMessage.Sender(
      id = senderId,
      name = "?"
    )
  } else {
    null
  }
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
  val current = commits[index] as? DomainCommit.Message ?: return BubbleMessage.Type.Top
  val older = (commits.getOrNull(index - 1) as? DomainCommit.Message)
  val newer = (commits.getOrNull(index + 1) as? DomainCommit.Message)
  val sameSenderOlder = older?.senderId == current.senderId
  val sameSenderNewer = newer?.senderId == current.senderId
  return when {
    sameSenderOlder && sameSenderNewer -> BubbleMessage.Type.Middle
    sameSenderNewer -> BubbleMessage.Type.Top
    sameSenderOlder -> BubbleMessage.Type.Bottom
    else -> BubbleMessage.Type.Top
  }
}
