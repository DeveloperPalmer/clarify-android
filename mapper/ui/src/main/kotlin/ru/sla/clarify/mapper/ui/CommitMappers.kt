package ru.sla.clarify.mapper.ui

import ru.sla.clarify.core.domain.date.TIME_FORMATTER_HOUR_MINUTE
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.resources.R
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
            key = "invite-member:${commit.id.value}",
            source = commit,
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
          key = "message:${commit.id.value}",
          source = commit,
          side = commit.side(),
          shape = bubbleType,
          text = commit.text,
          time = commit.timestamp.format(TIME_FORMATTER_HOUR_MINUTE),
          selected = false,
          edited = commit.editedAt != null,
          sender = commit.sender(bubbleType, memberNames)
        )
      }
    }
  }
}

private fun DomainCommit.sender(
  shape: Commit.Message.Shape,
  memberNames: Map<UserId, String?>
): Commit.Message.Sender? {
  val senderName = memberNames[senderId]
  val conditions = listOf(
    !isSelf,
    !senderName.isNullOrBlank(),
    shape == Commit.Message.Shape.Top
  )
  return if (conditions.all { it }) {
    Commit.Message.Sender(
      id = senderId,
      name = senderName.orEmpty()
    )
  } else {
    null
  }
}

private fun DomainCommit.side(): Commit.Message.Side {
  return if (isSelf) {
    Commit.Message.Side.Right(status.toReadStatus())
  } else {
    Commit.Message.Side.Left
  }
}

private fun DomainCommit.Status.toReadStatus(): Commit.Message.ReadStatus {
  return when (this) {
    DomainCommit.Status.Sending -> Commit.Message.ReadStatus.Sending
    DomainCommit.Status.Sent -> Commit.Message.ReadStatus.Sent
    DomainCommit.Status.Read -> Commit.Message.ReadStatus.Read
  }
}

private fun bubbleType(
  index: Int,
  commits: List<DomainCommit>
): Commit.Message.Shape {
  // The list is sorted newest-first (ChatCommit.select orders by timestamp DESC) and rendered
  // bottom-up with reverseLayout, so index-1 is visually below and index+1 is visually above.
  val current = commits[index] as? DomainCommit.Message ?: return Commit.Message.Shape.Top
  val below = (commits.getOrNull(index - 1) as? DomainCommit.Message)
  val above = (commits.getOrNull(index + 1) as? DomainCommit.Message)
  val sameSenderBelow = below?.senderId == current.senderId
  val sameSenderAbove = above?.senderId == current.senderId
  return when {
    sameSenderAbove && sameSenderBelow -> Commit.Message.Shape.Middle
    sameSenderBelow -> Commit.Message.Shape.Top
    sameSenderAbove -> Commit.Message.Shape.Bottom
    else -> Commit.Message.Shape.Top
  }
}
