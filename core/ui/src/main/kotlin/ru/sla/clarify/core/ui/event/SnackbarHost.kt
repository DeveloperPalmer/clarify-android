package ru.sla.clarify.core.ui.event

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume as resumeContinuation

/**
 * Modified version of [androidx.compose.material.SnackbarHost], with support for custom snackbars
 */
@Composable
fun SnackbarHost(
  hostState: SnackbarHostState,
  modifier: Modifier = Modifier
) {
  var visible by remember { mutableStateOf(false) }
  val currentSnackbarData = hostState.currentSnackbarData
  val accessibilityManager = LocalAccessibilityManager.current
  LaunchedEffect(currentSnackbarData) {
    if (currentSnackbarData != null) {
      visible = true
      delay(currentSnackbarData.component.duration.toMillis(accessibilityManager))
      visible = false
    }
  }
  AnimatedVisibility(
    modifier = modifier,
    visible = visible,
    enter = slideInVertically(),
    exit = slideOutVertically()
  ) {
    val scope = remember(currentSnackbarData) { ViewEventHostScope { visible = false } }
    @Suppress("UnnecessaryApply") // apply actually cannot be replaced here without context receivers
    currentSnackbarData?.component?.apply { scope.Content() }
    DisposableEffect(Unit) { onDispose { currentSnackbarData?.dismiss() } }
  }
}

@Stable
class SnackbarHostState {

  private val mutex = Mutex()

  var currentSnackbarData by mutableStateOf<SnackbarData?>(null)
    private set

  suspend fun showSnackbar(
    configuration: ViewEvent.Snackbar
  ): SnackbarResult = mutex.withLock {
    try {
      return suspendCancellableCoroutine { continuation ->
        currentSnackbarData = SnackbarData(configuration, continuation)
      }
    } finally {
      currentSnackbarData = null
    }
  }
}

@Stable
class SnackbarData(
  val component: ViewEvent.Snackbar,
  private val continuation: CancellableContinuation<SnackbarResult>
) {
  fun dismiss() {
    if (continuation.isActive) continuation.resumeContinuation(SnackbarResult.Dismissed)
  }
}

private fun ViewEvent.Snackbar.Duration.toMillis(
  accessibilityManager: AccessibilityManager?
): Long {
  val original = when (duration) {
    SnackbarDuration.Indefinite -> Long.MAX_VALUE
    SnackbarDuration.Long -> 10_000L
    SnackbarDuration.Short -> 4_000L
  }
  if (accessibilityManager == null) {
    return original
  }
  return accessibilityManager.calculateRecommendedTimeoutMillis(
    original,
    containsIcons = true,
    containsText = true,
    containsControls = hasActions
  )
}
