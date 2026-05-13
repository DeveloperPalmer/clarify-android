package ru.sla.clarify.core.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.kode.amvi.viewmodel.ViewIntents
import ru.sla.clarify.core.ui.event.LocalViewEventsHostMediator
import ru.sla.clarify.core.ui.event.ScreenViewEvent

@Suppress("UNCHECKED_CAST") // TODO figure out type safe ScreenViewEvent handling mechanism
@Composable
fun <S : Any, I : ViewIntents> MviComponent(
  viewModel: ViewModel<S, I>,
  intents: I,
  content: @Composable (state: S, intents: I) -> Unit
) {
  LifecycleEffect(viewModel, intents)
  val viewEventsHostController = LocalViewEventsHostMediator.current
  val state by viewModel.viewStateFlow.collectAsState()

  LaunchedEffect(viewModel) {
    viewModel.eventsFlow
      .map { event ->
        if (event is ScreenViewEvent<*>) {
          (event as ScreenViewEvent<I>).event(
            intents
          )
        } else {
          event
        }
      }
      .onEach { event -> viewEventsHostController.sendViewEvent(event) }
      .collect()
  }

  content(state, intents)
}

@Composable
inline fun <reified VI : ViewIntents> rememberViewIntents(): VI {
  return remember { VI::class.java.getDeclaredConstructor().newInstance() }
}

@Composable
private fun <VI : ViewIntents, VM : ViewModel<*, VI>> LifecycleEffect(viewModel: VM, intents: VI) {
  var attachedViewModel by remember { mutableStateOf<ViewModel<*, *>?>(null) }
  DisposableEffect(viewModel) {
    attachedViewModel?.detach()
    viewModel.attach(intents)
    attachedViewModel = viewModel

    onDispose {
      attachedViewModel?.detach()
    }
  }
}
