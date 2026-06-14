package ru.sla.clarify.feature.chat.group.thread.ui.mapper

import ru.sla.clarify.core.domain.date.TIME_FORMATTER_HOUR_MINUTE
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chat.conversation.domain.entity.GroupMember
import ru.sla.clarify.feature.chat.group.thread.ui.entity.Commit
import ru.sla.clarify.uikit.component.bubble.BubbleMessage
import ru.sla.resourcerefs.resRef
import ru.sla.clarify.feature.entity.chat.Commit as DomainCommit

internal fun List<DomainCommit>.toUiCommits(
  members: List<GroupMember> = emptyList()
): List<Commit> {
  val commits = this
  val names = members.associate { it.id.value to it.displayName }
  return commits.mapIndexed { index, commit ->
    when (commit) {
      is DomainCommit.InviteParticipant -> {
        Commit.InviteParticipant(
          source = commit,
          key = "invite-participant:${commit.id.value}",
          text = resRef(
            R.string.thread_system_member_invited,
            names.displayName(commit.senderId),
            names.displayName(commit.invitedId)
          )
        )
      }
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
            sender = commit.sender(
              type = bubbleType,
              names = names
            )
          )
        )
      }
    }
  }
}

private fun DomainCommit.sender(
  type: BubbleMessage.Type,
  names: Map<String, String?>
): BubbleMessage.Sender? {
  return if (!isSelf && type == BubbleMessage.Type.Top) {
    BubbleMessage.Sender(
      id = senderId,
      name = names.displayName(senderId)
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

private fun Map<String, String?>.displayName(id: UserId): String {
  return this[id.value].orEmpty().ifBlank { "?" }
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
