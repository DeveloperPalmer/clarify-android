package ru.sla.clarify.feature.chat.thread.ui.entity

import androidx.compose.runtime.Immutable
import ru.sla.clarify.uikit.component.bubble.BubbleMessage
import ru.sla.clarify.feature.entity.chat.Commit as DomainCommit

@Immutable
sealed interface Commit {

  val source: DomainCommit

  @Immutable
  data class Message(
    override val source: DomainCommit.Message,
    val bubble: BubbleMessage
  ) : Commit
}
