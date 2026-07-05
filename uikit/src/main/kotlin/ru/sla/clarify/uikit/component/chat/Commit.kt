package ru.sla.clarify.uikit.component.chat

import androidx.compose.runtime.Immutable
import arrow.optics.optics
import ru.sla.clarify.uikit.component.bubble.BubbleMessage
import ru.sla.resourcerefs.TextRef
import ru.sla.clarify.entity.chat.Commit as DomainCommit

@optics
@Immutable
sealed interface Commit {

  val key: String
  val source: DomainCommit

  @optics
  @Immutable
  data class Message(
    override val source: DomainCommit.Message,
    override val key: String,
    val bubble: BubbleMessage
  ) : Commit {
    companion object
  }

  @optics
  @Immutable
  data class InviteMember(
    override val source: DomainCommit.InviteMember,
    override val key: String,
    val text: TextRef
  ) : Commit {
    companion object
  }

  companion object
}
