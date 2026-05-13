package ru.sla.clarify.core.ui.event

import androidx.compose.runtime.Composable
import ru.kode.amvi.viewmodel.ViewIntents

/**
 * [ViewEvent] wrapper which enables access to screen view intents when they are required to
 * build the actual event payload. It postpones ViewEvent creation until view intents become
 * available during [ru.sla.clarify.core.ui.screen.MviComponent] initialization.
 *
 * Inherits [ViewEvent] so that both wrapped and non-wrapped events can be emitted from a
 * [ru.sla.clarify.core.ui.screen.ViewModel].
 *
 * Usage:
 * ```
 * sendViewEvent(
 *   ScreenViewEvent<ViewIntents> { intents ->
 *     Dialog.Decision(..., primaryAction = intents.confirmDelete)
 *   }
 * )
 * ```
 */
fun interface ScreenViewEvent<VI : ViewIntents> : ViewEvent {
  fun event(viewIntents: VI): ViewEvent

  @Composable
  override fun ViewEventHostScope.Content() = Unit
}
