package ru.sla.clarify.uikit.component.tabsrow

import androidx.compose.runtime.Stable
import ru.sla.resourcerefs.TextRef

@Stable
interface Tab {
  val title: TextRef
}
