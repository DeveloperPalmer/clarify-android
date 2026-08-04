package ru.sla.clarify.mapper.ui

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.resourcerefs.resRef
import ru.sla.resourcerefs.strRef
import ru.sla.clarify.entity.chat.Commit as DomainCommit

fun DomainCommit.Reply.toUiModel(
  memberNames: Map<UserId, String?>
): Commit.Message.Reply {
  val author = if (isSelf) {
    resRef(R.string.chat_reply_self)
  } else {
    memberNames[senderId]?.takeIf { it.isNotBlank() }?.let(::strRef)
  }
  return Commit.Message.Reply(
    targetId = id,
    author = author,
    text = text
  )
}
