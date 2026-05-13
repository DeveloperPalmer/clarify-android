package ru.sla.clarify.core.ui.event

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import javax.inject.Inject

/**
 * Wrapper class over the ViewEvents stream rendered by the root
 * [ru.sla.clarify.uikit.event.ViewEventsHost], bound to the application flow scope.
 *
 * Primary use case is calling indirectly via [ru.sla.clarify.core.ui.screen.ViewModel.sendViewEvent],
 * but generally it can be injected by any component that needs to fire one-time view events
 * (e.g. a coordinator).
 */
@Stable
@SingleIn(AppFlowScope::class)
class ViewEventsHostMediator @Inject constructor() {
  private val _events = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 3)
  val events: Flow<ViewEvent> = _events

  fun sendViewEvent(event: ViewEvent) {
    _events.tryEmit(event)
  }
}

val LocalViewEventsHostMediator = staticCompositionLocalOf<ViewEventsHostMediator> {
  error("No ViewEventsHostMediator provided")
}
