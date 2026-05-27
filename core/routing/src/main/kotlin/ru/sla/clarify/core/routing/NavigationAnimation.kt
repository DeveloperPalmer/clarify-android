package ru.sla.clarify.core.routing

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.kode.way.compose.NodeWithPath
import ru.kode.way.startsWith

/**
 * Creates a TransitionSpec which detects basic push/pop transitions based on source and target paths.
 * For example a transition
 *
 * appFlow.loginFlow.passwordInput → appFlow.loginFlow.passwordInput.otpInput
 *
 * will be recognized as a "push"-transition, because source path is contained within the target path.
 *
 * Similar logic, but in reverse is applied to detect "pop"-transitions.
 *
 * If neither source path contained in target path nor target path is contained in source path, this is treated as
 * an ambiguous situation and then [ambiguousTransitionResolver] will be called to build a transition.
 *
 * This resolver can be used to inspect source/target paths and produce the desired transition based on them,
 * for example:
 *
 * ```
 * rememberTransitionSpec(
 *   ambiguousTransitionResolver = {
 *     val fromNode = initialState; val target = targetState
 *     when {
 *       // (or use PermissionFlowSchema/LoginFlowSchema to find paths)
 *       fromNode.path == Path("appFlow", "permissionsFlow") && toNode.path == Path("appFlow", "loginFlow") -> {
 *         pushTransition()
 *       }
 *       else -> fadeTransition()
 *   }
 * )
 * ```
 *
 * **NOTE**: If automatic push/pop transition is derived incorrectly for your case, you should write your own transition
 * spec lambda and not use [rememberTransitionSpec] at all
 *
 * @param ambiguousTransitionResolver will be called to build a transition from/to nodes are such that
 * transition kind can not be determined automatically. See function description for an example implementation
 */
@Composable
fun rememberTransitionSpec(
  ambiguousTransitionResolver: AnimatedContentTransitionScope<NodeWithPath?>.() -> ContentTransform = {
    pushTransition()
  }
): AnimatedContentTransitionScope<NodeWithPath?>.() -> ContentTransform {
  return remember {
    {
      val fromNode = initialState
      val toNode = targetState
      when {
        fromNode != null && toNode != null -> {
          if (toNode.path.length > fromNode.path.length && toNode.path.startsWith(fromNode.path)) {
            pushTransition()
          } else if (toNode.path.length < fromNode.path.length && fromNode.path.startsWith(toNode.path)) {
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
