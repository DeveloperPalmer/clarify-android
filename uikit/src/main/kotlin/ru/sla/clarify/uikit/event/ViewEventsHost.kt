package ru.sla.clarify.uikit.event

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.sla.clarify.core.ui.event.BottomSheetHost
import ru.sla.clarify.core.ui.event.BottomSheetHostState
import ru.sla.clarify.core.ui.event.ContentHost
import ru.sla.clarify.core.ui.event.ContentHostState
import ru.sla.clarify.core.ui.event.DropdownMenuHost
import ru.sla.clarify.core.ui.event.DropdownMenuHostState
import ru.sla.clarify.core.ui.event.LocalViewEventsHostMediator
import ru.sla.clarify.core.ui.event.SnackbarHost
import ru.sla.clarify.core.ui.event.SnackbarHostState
import ru.sla.clarify.core.ui.event.ViewEvent

/**
 * Root level host that renders all one-time [ViewEvent]s emitted to [LocalViewEventsHostMediator].
 *
 * Place this composable once near the root of the composition tree (next to [NodeHost]).
 * Error snackbars are rendered to a dedicated host so a non-error snackbar can be shown
 * simultaneously with an error one without one preempting the other.
 */
@Composable
fun ViewEventsHost(modifier: Modifier = Modifier) {
  val configurationsFlow = LocalViewEventsHostMediator.current.events

  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val errorSnackbarHostState = remember { SnackbarHostState() }
  val contentHostState = remember { ContentHostState() }
  val bottomSheetHostState = remember { BottomSheetHostState() }
  val dropdownMenuHostState = remember { DropdownMenuHostState() }

  LaunchedEffect(Unit) {
    configurationsFlow
      .filterIsInstance<ViewEvent.Snackbar>()
      .onEach { snackbarConfiguration ->
        if (snackbarConfiguration.isError) {
          errorSnackbarHostState.showSnackbar(snackbarConfiguration)
        } else {
          snackbarHostState.showSnackbar(snackbarConfiguration)
        }
      }
      .launchIn(scope)

    configurationsFlow
      .filterIsInstance<ViewEvent.Content>()
      .onEach { event -> contentHostState.showContent(event) }
      .launchIn(scope)

    configurationsFlow
      .filterIsInstance<ViewEvent.BottomSheet>()
      .onEach { event -> bottomSheetHostState.showBottomSheet(event) }
      .launchIn(scope)

    configurationsFlow
      .filterIsInstance<ViewEvent.DropdownMenu>()
      .onEach { event -> dropdownMenuHostState.showDropdownMenu(event) }
      .launchIn(scope)
  }

  ContentHost(
    hostState = contentHostState
  )

  DropdownMenuHost(
    hostState = dropdownMenuHostState
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .systemBarsPadding()
      .navigationBarsPadding()
      .displayCutoutPadding()
  ) {
    BottomSheetHost(
      hostState = bottomSheetHostState
    )

    SnackbarHost(
      modifier = Modifier.align(Alignment.TopCenter),
      hostState = snackbarHostState
    )

    SnackbarHost(
      modifier = Modifier.align(Alignment.TopCenter),
      hostState = errorSnackbarHostState
    )
  }
}
