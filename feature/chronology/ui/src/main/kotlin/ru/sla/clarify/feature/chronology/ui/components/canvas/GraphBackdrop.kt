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
 * Фон полотна: ромбовидный паттерн.
 *
 * Паттерн двигается медленнее графа ([PARALLAX]) — это даёт ощущение глубины и опору взгляду при
 * панорамировании.
 *
 * Дорожки на фоне не рисуются. Дорожка — это приём раскладки, а не сведение для читающего: где
 * проходит ветка, видно по самим узлам и связям между ними, а лишняя горизонтальная сетка спорила
 * бы с графом за внимание и превращала бы карту в разлинованный лист. Технически дорожки есть,
 * визуально их нет.
 *
 * Паттерн рисуется `contentPrimary` с очень низкой альфой, а не `cardQuinary`: в тёмной теме
 * `cardQuinary` равен `cardPrimary`, и узлы слились бы с фоном.
 *
 * Камера читается внутри `Canvas`, а не в композиции: иначе фон перерисовывался бы через
 * рекомпозицию на каждом кадре панорамирования.
 *
 * @param state камера полотна
 * @param modifier модификатор фона
 */
@Composable
internal fun GraphBackdrop(
  state: GraphCanvasState,
  modifier: Modifier = Modifier
) {
  val patternColor = AppTheme.colors.contentPrimary.copy(alpha = PATTERN_ALPHA)
  Canvas(modifier = modifier) {
    state.telemetry.onBackdropDraw()
    drawDiamondPattern(
      color = patternColor,
      offset = state.offset.value * PARALLAX,
      spacing = PATTERN_SPACING.toPx()
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

  drawDiagonals(
    color = color,
    phase = phase(offset.x - offset.y, stepX),
    stepX = stepX,
    width = width,
    height = height,
    slopeDown = true
  )
  drawDiagonals(
    color = color,
    phase = phase(offset.x + offset.y, stepX),
    stepX = stepX,
    width = width,
    height = height,
    slopeDown = false
  )
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

/** Остаток от деления, приведённый к `[0, step)` — `%` в Kotlin сохраняет знак делимого. */
private fun phase(value: Float, step: Float): Float {
  return value - step * floor(value / step)
}

private const val PARALLAX = 0.3f
private const val PATTERN_ALPHA = 0.04f
private val PATTERN_SPACING: Dp = 120.dp
private val LINE_WIDTH: Dp = 1.dp
