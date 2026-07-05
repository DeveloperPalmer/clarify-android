package ru.sla.clarify.feature.chat.group.thread.ui.mapper

import ru.sla.clarify.core.domain.date.TIME_FORMATTER_HOUR_MINUTE
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chat.group.thread.domain.entity.GroupMember
import ru.sla.clarify.uikit.component.bubble.BubbleMessage
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.resourcerefs.resRef
import ru.sla.clarify.entity.chat.Commit as DomainCommit

internal fun List<DomainCommit>.toUiCommits(
  members: List<GroupMember> = emptyList()
): List<Commit> {
  val commits = this
  val names = members.associate { it.id.value to it.displayName }
  return commits.mapIndexedNotNull { index, commit ->
    when (commit) {
      is DomainCommit.InviteMember -> {
        val senderName = names.displayName(commit.senderId)
        val invitedName = names.displayName(commit.invitedId)
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
            sender = commit.sender(bubbleType, names),
            selection = BubbleMessage.Selection(
              inSelectionMode = false,
              isSelected = false
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
  val senderName = names.displayName(senderId)
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

private fun Map<String, String?>.displayName(id: UserId): String? {
  return this[id.value]
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
