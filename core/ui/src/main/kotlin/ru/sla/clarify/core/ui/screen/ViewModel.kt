package ru.sla.clarify.core.ui.screen

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.kode.amvi.viewmodel.ViewIntents
import ru.sla.clarify.core.ui.event.ViewEvent
import ru.kode.amvi.viewmodel.ViewModel as BaseViewModel

abstract class ViewModel<VS : Any, VI : ViewIntents> : BaseViewModel<VS, VI>() {

  val eventsFlow = MutableSharedFlow<ViewEvent>()

  protected fun sendViewEvent(event: ViewEvent) {
    viewModelScope.launch { eventsFlow.ensureEmit(event) }
  }

  //  Subscription to the flow, in which the emitting value before the first subscriber to the flow appears.
  //  To ensure that you receive the value, you need to make sure that there is at least one subscriber
  private suspend fun <T> MutableSharedFlow<T>.ensureEmit(value: T) {
    subscriptionCount.firstOrNull { it > 0 }
    emit(value)
  }
}
