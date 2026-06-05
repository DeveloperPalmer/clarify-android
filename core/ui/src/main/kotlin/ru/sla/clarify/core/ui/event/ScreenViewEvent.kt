package ru.sla.clarify.core.ui.event

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow
import ru.kode.amvi.viewmodel.ViewIntents
import ru.sla.clarify.core.ui.event.ViewEvent.Content

/**
 * [ViewEvent] wrapper which enables access to screen view intents and the live view state when
 * they are required to build the actual event payload. It postpones ViewEvent creation until view
 * intents and the [StateFlow] of the view state become available during
 * [ru.sla.clarify.core.ui.screen.MviComponent] initialization.
 *
 * The state is provided as a [StateFlow] so that the produced event can both read the current
 * snapshot via [StateFlow.value] and subscribe to changes reactively inside [Content] via
 * `collectAsState`, the same way a regular screen observes its state.
 *
 * Inherits [ViewEvent] so that both wrapped and non-wrapped events can be emitted from a
 * [ru.sla.clarify.core.ui.screen.ViewModel].
 *
 * Usage:
 * ```
 * sendViewEvent(
 *   ScreenViewEvent<ViewState, ViewIntents> { state, intents ->
 *     object : ViewEvent.BottomSheet {
 *       override val sheetState = mutableStateOf<SheetState?>(null)
 *
 *       @Composable
 *       override fun ViewEventHostScope.Content() {
 *         val current by state.collectAsState()
 *         // ...render using current and intents
 *       }
 *     }
 *   }
 * )
 * ```
 */
fun interface ScreenViewEvent<VS : Any, VI : ViewIntents> : ViewEvent {
  fun event(state: StateFlow<VS>, viewIntents: VI): ViewEvent

  @Composable
  override fun ViewEventHostScope.Content() = Unit
}
