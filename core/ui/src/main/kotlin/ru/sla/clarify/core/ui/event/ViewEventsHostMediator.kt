package ru.sla.clarify.core.ui.event

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.app.domain.di.AppFlowScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

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
