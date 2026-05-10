package ru.sla.clarify.core.routing

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import ru.kode.way.NavigationService
import ru.kode.way.NavigationState
import ru.kode.way.Path
import ru.kode.way.compose.NodeWithPath
import java.util.concurrent.atomic.AtomicBoolean

// Aligned with Material motion durations recommended by the Navigation 3 guideline:
// https://developer.android.com/guide/navigation/navigation-3/animate-destinations
private const val FORWARD_DURATION_MS = 350
private const val POP_DURATION_MS = 300

/**
 * Builds a transition spec for [ru.kode.way.compose.NodeHost] implementing the horizontal slide
 * navigation animations from the Navigation 3 guideline:
 *
 *  * Forward navigation: new content slides in from the trailing edge while the previous content
 *    slides out toward the leading edge.
 *  * Backward navigation (pop): new content slides in from the leading edge while the current
 *    content slides out toward the trailing edge.
 *
 * Direction is derived from a path back-stack maintained via [NavigationService.addTransitionListener].
 * If the new active path is already present in the stack we treat the change as a pop and trim the
 * stack down to it; otherwise we treat it as a push and append.
 *
 * Why a back-stack rather than `event == Event.Back`: `way` finishes a child flow as a chained
 * sequence of events (`Event.Back` → `*ChildFinishRequest.*` → `NavigateTo(parent)`), so by the time
 * the navigation state reaches the parent path the latest event isn't `Event.Back` anymore. The
 * back-stack approach is independent of event semantics and works for any flow topology, including
 * sibling-flow transitions whose paths don't share a prefix.
 *
 * `sizeTransform` is disabled in every branch — `AnimatedContent` would otherwise animate the
 * container size between the previous and the next content, producing an "expand from center"
 * effect, especially noticeable on the very first composition.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun rememberNavigationTransitionSpec(
  service: NavigationService<*>
): AnimatedContentTransitionScope<NodeWithPath?>.() -> ContentTransform {
  // Listeners run on the same thread that calls `service.sendEvent` (the main thread in this app),
  // so a plain MutableList is enough and the AtomicBoolean is just for memory visibility from the
  // `transitionSpec` lambda invoked by AnimatedContent.
  val backStack = remember { mutableListOf<Path>() }
  val isPop = remember { AtomicBoolean(false) }
  DisposableEffect(service) {
    val listener: (NavigationState) -> Unit = { state ->
      val newPath = state.regions.values.firstOrNull()?.active
      if (newPath != null) {
        val existingIndex = backStack.indexOf(newPath)
        when {
          backStack.isEmpty() -> {
            backStack += newPath
            isPop.set(false)
          }
          backStack.last() == newPath -> Unit
          existingIndex >= 0 -> {
            while (backStack.lastIndex > existingIndex) backStack.removeAt(backStack.lastIndex)
            isPop.set(true)
          }
          else -> {
            backStack += newPath
            isPop.set(false)
          }
        }
      }
    }
    service.addTransitionListener(listener)
    onDispose { service.removeTransitionListener(listener) }
  }
  return remember {
    spec@{
      val initial = initialState
      val target = targetState
      val transform = when {
        initial == null || target == null -> {
          fadeIn(
            animationSpec = tween(durationMillis = FORWARD_DURATION_MS)
          ) togetherWith fadeOut(
            animationSpec = tween(durationMillis = FORWARD_DURATION_MS / 2)
          )
        }
        isPop.get() -> {
          slideIntoContainer(
            animationSpec = tween(durationMillis = POP_DURATION_MS),
            towards = SlideDirection.Right
          ) togetherWith slideOutOfContainer(
            animationSpec = tween(durationMillis = POP_DURATION_MS),
            towards = SlideDirection.Right
          )
        }
        else -> {
          slideIntoContainer(
            animationSpec = tween(durationMillis = FORWARD_DURATION_MS),
            towards = SlideDirection.Left
          ) togetherWith slideOutOfContainer(
            animationSpec = tween(durationMillis = FORWARD_DURATION_MS),
            towards = SlideDirection.Left
          )
        }
      }
      transform.using(sizeTransform = null)
    }
  }
}
