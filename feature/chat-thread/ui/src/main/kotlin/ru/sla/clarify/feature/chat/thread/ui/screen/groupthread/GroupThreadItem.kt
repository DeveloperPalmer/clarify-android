package ru.sla.clarify.feature.chat.thread.ui.screen.groupthread

import androidx.compose.runtime.Immutable
import ru.sla.clarify.uikit.component.bubble.BubbleMessage

@Immutable
sealed interface GroupThreadItem {
  val key: String

  @Immutable
  data class Bubble(val bubble: BubbleMessage) : GroupThreadItem {
    override val key: String get() = "bubble:${bubble.id.value}"
  }

  @Immutable
  data class System(val id: String, val text: String) : GroupThreadItem {
    override val key: String get() = "system:$id"
  }
}
