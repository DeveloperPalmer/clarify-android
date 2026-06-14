package ru.sla.clarify.core.ui.event

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.sla.clarify.core.domain.logError
import ru.sla.clarify.core.ui.event.ViewEvent.Content

/**
 * Modeled very closely to [androidx.compose.material.SnackbarHost] (except there's no auto-dismiss)
 * Check out those classes if you'll need to add support for queueing (using mutex) etc
 */
@Composable
fun ContentHost(
  hostState: ContentHostState,
  modifier: Modifier = Modifier
) {
  var visible by remember { mutableStateOf(false) }
  val currentContentData = hostState.currentContentData

  LaunchedEffect(currentContentData) {
    visible = currentContentData != null
  }

  AnimatedVisibility(
    modifier = modifier,
    visible = visible,
    enter = when (currentContentData?.component?.animation) {
      ViewEvent.Animation.Fade -> fadeIn()
      ViewEvent.Animation.Slide -> slideIn { fullSize -> IntOffset(x = 0, y = fullSize.height) }
      null -> EnterTransition.None
    },
    exit = when (currentContentData?.component?.animation) {
      ViewEvent.Animation.Fade -> fadeOut()
      ViewEvent.Animation.Slide -> slideOut { fullSize -> IntOffset(x = 0, y = fullSize.height) }
      null -> ExitTransition.None
    }
  ) {
    val scope = remember(currentContentData) { ViewEventHostScope { visible = false } }
    @Suppress("UnnecessaryApply") // apply actually cannot be replaced here without context receivers
    currentContentData?.component?.apply {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clickable(enabled = false, onClick = {})
      ) {
        scope.Content()
      }
    }

    DisposableEffect(Unit) {
      onDispose {
        currentContentData?.let { data ->
          data.dismiss()
          data.component.postDestroy()
        }
      }
    }
  }
}

@Stable
class ContentHostState {
  internal var currentContentData by mutableStateOf<ContentData?>(null)

  suspend fun showContent(configuration: Content) {
    val data = currentContentData
    if (data != null) {
      logError {
        "Already showing a Content for event: $data. New event $configuration will be ignored."
      }
    } else {
      try {
        return suspendCancellableCoroutine { continuation ->
          currentContentData = ContentData(
            component = configuration,
            continuation = continuation
          )
        }
      } finally {
        currentContentData = null
      }
    }
  }
}

@Stable
internal class ContentData(
  val component: Content,
  private val continuation: CancellableContinuation<Unit>
) {
  fun dismiss() {
    if (continuation.isActive) continuation.resume(Unit) { _, _, _ -> }
  }
}
