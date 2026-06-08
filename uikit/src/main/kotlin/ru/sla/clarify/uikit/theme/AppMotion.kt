package ru.sla.clarify.uikit.theme

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
class AppMotion {
  val mediumMillis = 250

  @Composable
  fun mediumBoundsTransform(): BoundsTransform {
    val easing = AppTheme.motion.emphasized
    val durationMillis = AppTheme.motion.mediumMillis
    return BoundsTransform { _, _ ->
      tween(
        easing = easing,
        durationMillis = durationMillis
      )
    }
  }

  @Composable
  fun <T> mediumTween(): TweenSpec<T> {
    return tween(
      easing = AppTheme.motion.emphasized,
      durationMillis = AppTheme.motion.mediumMillis
    )
  }

  val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

internal val LocalAppMotion = staticCompositionLocalOf<AppMotion> {
  error("No AppMotion provided")
}
