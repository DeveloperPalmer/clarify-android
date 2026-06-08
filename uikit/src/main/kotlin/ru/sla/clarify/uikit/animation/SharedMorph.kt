package ru.sla.clarify.uikit.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.theme.AppTheme
import kotlin.math.roundToInt

@Composable
fun SharedContainer(
  key: Any,
  visible: Boolean,
  restingCorner: Dp,
  morphedCorner: Dp,
  modifier: Modifier = Modifier,
  content: @Composable SharedMorphScope.() -> Unit
) {
  val visibleState = remember { MutableTransitionState(initialState = false) }
  visibleState.targetState = visible
  AnimatedVisibility(
    visibleState = visibleState,
    modifier = modifier,
    enter = fadeIn(tween(AppTheme.motion.mediumMillis)),
    exit = fadeOut(tween(AppTheme.motion.mediumMillis))
  ) {
    val scope = remember(this, key, restingCorner, morphedCorner) {
      SharedMorphScopeImpl(key, this, restingCorner, morphedCorner)
    }
    scope.content()
  }
}

@Stable
interface SharedMorphScope {
  @Composable
  fun Modifier.sharedSurface(
    color: Color,
    elevation: Dp = 0.dp,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
  ): Modifier

  @Composable
  fun Modifier.revealContent(window: ClosedFloatingPointRange<Float> = 0f..1f): Modifier
}

private class SharedMorphScopeImpl(
  private val key: Any,
  private val scope: AnimatedVisibilityScope,
  private val restingCorner: Dp,
  private val morphedCorner: Dp
) : SharedMorphScope {

  @Composable
  override fun Modifier.sharedSurface(
    color: Color,
    elevation: Dp,
    enabled: Boolean,
    onClick: (() -> Unit)?
  ): Modifier {
    return sharedSurfaceInternal(
      key = key,
      scope = scope,
      restingCorner = restingCorner,
      morphedCorner = morphedCorner,
      color = color,
      elevation = elevation,
      enabled = enabled,
      onClick = onClick
    )
  }

  @Composable
  override fun Modifier.revealContent(window: ClosedFloatingPointRange<Float>): Modifier {
    return revealContentInternal(scope, window)
  }
}

@Composable
private fun Modifier.revealContentInternal(
  scope: AnimatedVisibilityScope,
  window: ClosedFloatingPointRange<Float>
): Modifier {
  val totalMillis = AppTheme.motion.mediumMillis
  val delayMillis = (window.start * totalMillis).roundToInt()
  val durationMillis = ((window.endInclusive - window.start) * totalMillis)
    .roundToInt()
    .coerceAtLeast(1)
  val spec = tween<Float>(
    delayMillis = delayMillis,
    durationMillis = durationMillis,
    easing = AppTheme.motion.emphasized
  )
  return this.then(with(scope) { Modifier.animateEnterExit(fadeIn(spec), fadeOut(spec)) })
}

@Suppress("LongParameterList")
@Composable
private fun Modifier.sharedSurfaceInternal(
  key: Any,
  scope: AnimatedVisibilityScope,
  restingCorner: Dp,
  morphedCorner: Dp,
  color: Color,
  enabled: Boolean = true,
  elevation: Dp = 0.dp,
  onClick: (() -> Unit)? = null
): Modifier = with(LocalSharedTransitionScope.current) {
  val cornerTween = AppTheme.motion.mediumTween<Dp>()
  val corner = scope.transition.animateValue(
    typeConverter = Dp.VectorConverter,
    label = "shared-morph-corner",
    transitionSpec = { cornerTween }
  ) { state -> if (state == EnterExitState.Visible) restingCorner else morphedCorner }

  this@sharedSurfaceInternal
    .sharedBounds(
      sharedContentState = rememberSharedContentState(key),
      animatedVisibilityScope = scope,
      boundsTransform = AppTheme.motion.mediumBoundsTransform(),
      resizeMode = RemeasureToBounds
    )
    .graphicsLayer {
      shape = RoundedCornerShape(corner.value)
      clip = true
      shadowElevation = elevation.toPx()
    }
    .drawBehind { drawRect(color) }
    .then(
      if (onClick != null) {
        Modifier.combinedClickable(
          enabled = enabled,
          onClick = onClick
        )
      } else {
        Modifier
      }
    )
}
