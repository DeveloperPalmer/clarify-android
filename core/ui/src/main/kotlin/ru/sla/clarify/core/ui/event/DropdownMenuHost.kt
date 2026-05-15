package ru.sla.clarify.core.ui.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.sla.clarify.core.domain.logError

/**
 * Modeled very closely to [androidx.compose.material.SnackbarHost] (except there's no auto-dismiss)
 * Check out those classes if you'll need to add support for queueing (using mutex) etc
 */
@Composable
fun DropdownMenuHost(hostState: DropdownMenuHostState) {
  val currentDropdownMenuData = hostState.currentDropdownMenuData
  if (currentDropdownMenuData != null) {
    val scope = remember(currentDropdownMenuData) {
      ViewEventHostScope { currentDropdownMenuData.dismiss() }
    }
    @Suppress("UnnecessaryApply") // apply actually cannot be replaced here without context receivers
    currentDropdownMenuData.component.apply { scope.Content() }
  }
}

@Stable
class DropdownMenuHostState {
  internal var currentDropdownMenuData by mutableStateOf<DropdownMenuData?>(null)

  suspend fun showDropdownMenu(configuration: ViewEvent.DropdownMenu) {
    val data = currentDropdownMenuData
    if (data != null) {
      logError {
        "Already showing a dropdown menu for event: $data. New event $configuration will be ignored."
      }
    } else {
      try {
        return suspendCancellableCoroutine { continuation ->
          currentDropdownMenuData = DropdownMenuData(
            component = configuration,
            continuation = continuation
          )
        }
      } finally {
        currentDropdownMenuData = null
      }
    }
  }
}

@Stable
internal class DropdownMenuData(
  val component: ViewEvent.DropdownMenu,
  private val continuation: CancellableContinuation<Unit>
) {
  fun dismiss() {
    if (continuation.isActive) continuation.resume(Unit) { _, _, _ -> }
  }
}
