package ru.sla.clarify.feature.chat.branch.ui.entity

import androidx.compose.runtime.Immutable
import ru.sla.clarify.uikit.component.bubble.BubbleMessage
import ru.sla.resourcerefs.TextRef
import ru.sla.clarify.feature.entity.chat.Commit as DomainCommit

@Immutable
sealed interface Commit {

  val key: String
  val source: DomainCommit

  @Immutable
  data class Message(
    override val source: DomainCommit.Message,
    override val key: String,
    val bubble: BubbleMessage
  ) : Commit

  @Immutable
  data class InviteMember(
    override val source: DomainCommit.InviteMember,
    override val key: String,
    val text: TextRef
  ) : Commit
}
