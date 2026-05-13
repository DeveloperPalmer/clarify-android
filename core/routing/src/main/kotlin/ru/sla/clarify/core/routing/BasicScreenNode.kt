package ru.sla.clarify.core.routing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.kode.way.Event
import ru.kode.way.Ignore
import ru.kode.way.ScreenTransition
import ru.kode.way.compose.ComposableNode
import ru.kode.way.extension.node.hook.BaseScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen

// NOTE: DO NOT make it abstract or open
// It is supposed to be a quick drop in class for simple screens.
// If you need custom transition handling or custom content handling, just make your own copy.
// END NOTE: DO NOT make it abstract or open
class BasicScreenNode(private val screen: WiredComposableScreen) : BaseScreenNode(), ComposableNode {
  override fun transition(event: Event): ScreenTransition {
    return Ignore
  }

  @Composable
  override fun Content(modifier: Modifier) {
    screen.Content(modifier)
  }

  override fun onExit(event: Event) {
    super.onExit(event)
    screen.destroy()
  }
}
