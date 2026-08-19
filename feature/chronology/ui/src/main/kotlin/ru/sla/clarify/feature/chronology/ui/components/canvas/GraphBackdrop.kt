package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.theme.AppTheme
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Фон полотна: ромбовидный паттерн и направляющие дорожек.
 *
 * Паттерн двигается медленнее графа ([PARALLAX]) — это даёт ощущение глубины и опору взгляду
 * при панорамировании. Направляющие двигаются вместе с графом, без параллакса: они объясняют,
 * почему узлы стоят именно на этих высотах.
 *
 * Сетка рисуется `contentPrimary` с очень низкой альфой, а не `cardQuinary`: в тёмной теме
 * `cardQuinary` равен `cardPrimary`, и узлы слились бы с фоном.
 */
@Composable
internal fun GraphBackdrop(
  camera: Offset,
  lanes: IntRange,
  modifier: Modifier = Modifier
) {
  val patternColor = AppTheme.colors.contentPrimary.copy(alpha = PATTERN_ALPHA)
  val guideColor = AppTheme.colors.contentPrimary.copy(alpha = GUIDE_ALPHA)
  Canvas(modifier = modifier) {
    drawDiamondPattern(
      color = patternColor,
      offset = camera * PARALLAX,
      spacing = PATTERN_SPACING.toPx()
    )
    drawLaneGuides(
      color = guideColor,
      offset = camera,
      lanes = lanes
    )
  }
}

/**
 * Две решётки под +45° и −45°.
 *
 * Прямая семейства «вниз-вправо» задаётся `x - y = c`, «вниз-влево» — `x + y = c`. Сдвиг камеры
 * на `(dx, dy)` меняет `c` на `dx - dy` и `dx + dy` соответственно — отсюда две разные фазы.
 */
private fun DrawScope.drawDiamondPattern(
  color: Color,
  offset: Offset,
  spacing: Float
) {
  if (spacing <= 0f) return
  // Шаг вдоль оси X: перпендикулярное расстояние между диагоналями равно spacing.
  val stepX = spacing * sqrt(2f)
  val height = size.height
  val width = size.width

  drawDiagonals(color, phase(offset.x - offset.y, stepX), stepX, width, height, slopeDown = true)
  drawDiagonals(color, phase(offset.x + offset.y, stepX), stepX, width, height, slopeDown = false)
}

@Suppress("LongParameterList")
private fun DrawScope.drawDiagonals(
  color: Color,
  phase: Float,
  stepX: Float,
  width: Float,
  height: Float,
  slopeDown: Boolean
) {
  // Диагональ пересекает viewport по горизонтали на `height`, поэтому семейство расширено
  // на эту величину в сторону, откуда линии «въезжают».
  val from = if (slopeDown) -height + phase else phase
  val to = if (slopeDown) width else width + height
  var x = from
  while (x <= to) {
    val endX = if (slopeDown) x + height else x - height
    drawLine(
      color = color,
      start = Offset(x, 0f),
      end = Offset(endX, height),
      strokeWidth = LINE_WIDTH.toPx()
    )
    x += stepX
  }
}

private fun DrawScope.drawLaneGuides(
  color: Color,
  offset: Offset,
  lanes: IntRange
) {
  lanes.forEach { index ->
    val y = GraphGeometry.laneY(index).toPx() + offset.y
    if (y in 0f..size.height) {
      drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = LINE_WIDTH.toPx()
      )
    }
  }
}

/** Остаток от деления, приведённый к `[0, step)` — `%` в Kotlin сохраняет знак делимого. */
private fun phase(value: Float, step: Float): Float = value - step * floor(value / step)

private const val PARALLAX = 0.3f
private const val PATTERN_ALPHA = 0.04f
private const val GUIDE_ALPHA = 0.06f
private val PATTERN_SPACING: Dp = 120.dp
private val LINE_WIDTH: Dp = 1.dp
