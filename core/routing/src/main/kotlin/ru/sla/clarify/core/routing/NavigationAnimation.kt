package ru.sla.clarify.core.routing

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.kode.way.Path
import ru.kode.way.startsWith

@Composable
fun rememberTransitionSpec(
  ambiguousTransitionResolver: AnimatedContentTransitionScope<Path?>.() -> ContentTransform
): AnimatedContentTransitionScope<Path?>.() -> ContentTransform {
  return remember {
    {
      val fromPath = initialState
      val toPath = targetState
      when {
        fromPath != null && toPath != null -> {
          if (toPath.length > fromPath.length && toPath.startsWith(fromPath)) {
            pushTransition()
          } else if (toPath.length < fromPath.length && fromPath.startsWith(toPath)) {
            popTransition()
          } else {
            ambiguousTransitionResolver()
          }
        }
        else -> {
          noTransition()
        }
      }
    }
  }
}

fun AnimatedContentTransitionScope<*>.pushTransition(): ContentTransform {
  val transition = slideIntoContainer(
    animationSpec = tween(durationMillis = FORWARD_DURATION_MS),
    towards = AnimatedContentTransitionScope.SlideDirection.Left
  ) togetherWith slideOutOfContainer(
    animationSpec = tween(durationMillis = FORWARD_DURATION_MS),
    towards = AnimatedContentTransitionScope.SlideDirection.Left
  )
  return transition.using(sizeTransform = null)
}

fun AnimatedContentTransitionScope<*>.popTransition(): ContentTransform {
  val transition = slideIntoContainer(
    animationSpec = tween(durationMillis = POP_DURATION_MS),
    towards = AnimatedContentTransitionScope.SlideDirection.Right
  ) togetherWith slideOutOfContainer(
    animationSpec = tween(durationMillis = POP_DURATION_MS),
    towards = AnimatedContentTransitionScope.SlideDirection.Right
  )
  return transition.using(sizeTransform = null)
}

fun AnimatedContentTransitionScope<*>.noTransition(): ContentTransform {
  return (EnterTransition.None togetherWith ExitTransition.None).using(sizeTransform = null)
}

// Aligned with Material motion durations recommended by the Navigation 3 guideline:
// https://developer.android.com/guide/navigation/navigation-3/animate-destinations
private const val FORWARD_DURATION_MS = 200
private const val POP_DURATION_MS = 200
