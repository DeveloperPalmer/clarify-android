package ru.sla.clarify.mapper.ui

import ru.sla.clarify.core.domain.date.TIME_FORMATTER_HOUR_MINUTE
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.component.bubble.BubbleMessage
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.resourcerefs.resRef
import ru.sla.clarify.entity.chat.Commit as DomainCommit

fun List<DomainCommit>.toUiCommits(
  memberNames: Map<UserId, String?> = emptyMap()
): List<Commit> {
  val commits = this
  return commits.mapIndexedNotNull { index, commit ->
    when (commit) {
      is DomainCommit.InviteMember -> {
        val senderName = memberNames[commit.senderId]
        val invitedName = memberNames[commit.invitedId]
        if (!senderName.isNullOrBlank() && !invitedName.isNullOrBlank()) {
          Commit.InviteMember(
            source = commit,
            key = "invite-member:${commit.id.value}",
            text = resRef(R.string.thread_system_member_invited, senderName, invitedName)
          )
        } else {
          null
        }
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
            sender = commit.sender(bubbleType, memberNames),
            isSelected = false
          )
        )
      }
    }
  }
}

private fun DomainCommit.sender(
  type: BubbleMessage.Type,
  memberNames: Map<UserId, String?>
): BubbleMessage.Sender? {
  val senderName = memberNames[senderId]
  val conditions = listOf(
    !isSelf,
    !senderName.isNullOrBlank(),
    type == BubbleMessage.Type.Top
  )
  return if (conditions.all { it }) {
    BubbleMessage.Sender(
      id = senderId,
      name = senderName.orEmpty()
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
  // The list is sorted newest-first (selectByBranchId orders by timestamp DESC) and rendered
  // bottom-up with reverseLayout, so index-1 is visually below and index+1 is visually above.
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
