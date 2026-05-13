package ru.sla.clarify.core.ui.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.sla.clarify.core.domain.logError

/**
 * Modeled very closely to [androidx.compose.material.SnackbarHost] (except there's no auto-dismiss)
 * Check out those classes if you'll need to add support for queueing (using mutex) etc
 */
@Composable
fun BottomSheetHost(hostState: BottomSheetHostState) {
  val coroutineScope = rememberCoroutineScope()
  val currentBottomSheetData = hostState.currentBottomSheetData
  if (currentBottomSheetData != null) {
    val scope = remember(currentBottomSheetData) {
      ViewEventHostScope {
        coroutineScope.launch {
          currentBottomSheetData.component.sheetState.value?.hide()
          currentBottomSheetData.dismiss()
        }
      }
    }
    @Suppress("UnnecessaryApply") // apply actually cannot be replaced here without context receivers
    currentBottomSheetData.component.apply { scope.Content() }
  }
}

@Stable
class BottomSheetHostState {
  internal var currentBottomSheetData by mutableStateOf<BottomSheetData?>(null)

  suspend fun showBottomSheet(configuration: ViewEvent.BottomSheet) {
    val data = currentBottomSheetData
    if (data != null) {
      logError {
        "Already showing a modal sheet for event: $data. New event $configuration will be ignored."
      }
    } else {
      try {
        return suspendCancellableCoroutine { continuation ->
          currentBottomSheetData = BottomSheetData(
            component = configuration,
            continuation = continuation
          )
        }
      } finally {
        currentBottomSheetData = null
      }
    }
  }
}

@Stable
internal class BottomSheetData(
  val component: ViewEvent.BottomSheet,
  private val continuation: CancellableContinuation<Unit>
) {
  fun dismiss() {
    if (continuation.isActive) continuation.resume(Unit, onCancellation = null)
  }
}
