package ru.sla.clarify.uikit.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment

@Immutable
class AppMotion {
  val smallestMillis = 100
  val smallMillis = 150
  val mediumMillis = 250
  val largeMillis = 400

  /**
   * Период бесконечного равномерного повтора: бегущий пунктир, марширующая линия.
   *
   * Стоит вне ряда `100 / 150 / 250 / 400` потому, что это не длительность перехода: переход тем
   * лучше, чем короче, а бег, уложенный в четверть секунды, читается не движением, а дрожью.
   */
  val loopMillis = 900

  @Composable
  fun <S> mediumTransitionSpec(): AnimatedContentTransitionScope<S>.() -> ContentTransform {
    val tween = mediumTween<Float>()
    return { fadeIn(tween) togetherWith fadeOut(tween) }
  }

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
  fun <T> smallestTween(): TweenSpec<T> {
    return tween(
      durationMillis = AppTheme.motion.smallMillis
    )
  }

  @Composable
  fun <T> smallTween(): TweenSpec<T> {
    return tween(
      durationMillis = AppTheme.motion.smallMillis
    )
  }

  @Composable
  fun <T> mediumTween(): TweenSpec<T> {
    return tween(
      easing = AppTheme.motion.emphasized,
      durationMillis = AppTheme.motion.mediumMillis
    )
  }

  /** Длинное движение, затухающее без разгона: перелёты камеры, стягивание линии. */
  @Composable
  fun <T> largeTween(): TweenSpec<T> {
    return tween(
      easing = AppTheme.motion.decelerate,
      durationMillis = AppTheme.motion.largeMillis
    )
  }

  /**
   * Бесконечный равномерный цикл: значение идёт от начала к концу за [loopMillis] и начинает заново.
   *
   * Кривая линейная, а не `emphasized`, и это не упущение: у повтора нет ни начала, ни конца, а
   * любое ускорение с торможением превращает шов между итерациями в рывок на ровном месте.
   *
   * @return спека повтора; куда именно движется значение, решает точка использования
   */
  @Composable
  fun <T> loopTween(): InfiniteRepeatableSpec<T> {
    return infiniteRepeatable(
      animation = tween(
        easing = LinearEasing,
        durationMillis = AppTheme.motion.loopMillis
      ),
      repeatMode = RepeatMode.Restart
    )
  }

  /** Element slides in from the start edge while pushing siblings aside (expand + slide + fade). */
  @Composable
  fun slideInFromStart(): EnterTransition {
    return expandHorizontally(
      animationSpec = mediumTween(),
      expandFrom = Alignment.Start
    ) + slideInHorizontally(
      animationSpec = mediumTween(),
      initialOffsetX = { fullWidth -> -fullWidth }
    ) + fadeIn(
      animationSpec = mediumTween()
    )
  }

  /** Reverse of [slideInFromStart]: element slides back out towards the start edge. */
  @Composable
  fun slideOutToStart(): ExitTransition {
    return shrinkHorizontally(
      animationSpec = mediumTween(),
      shrinkTowards = Alignment.Start
    ) + slideOutHorizontally(
      animationSpec = mediumTween(),
      targetOffsetX = { fullWidth -> -fullWidth }
    ) + fadeOut(
      animationSpec = mediumTween()
    )
  }

  val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

  /** Без разгона: движение начинается на полной скорости и тормозит. Для камеры на полотне. */
  val decelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
}

internal val LocalAppMotion = staticCompositionLocalOf<AppMotion> {
  error("No AppMotion provided")
}
