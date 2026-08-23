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
 *
 * @param color цвет линий паттерна
 * @param offset сдвиг паттерна: камера, уже умноженная на параллакс
 * @param spacing перпендикулярное расстояние между соседними диагоналями
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
    width = width,
    height = height,
    phase = phase(offset.x - offset.y, stepX),
    stepX = stepX,
    slope = DiagonalSlope.DownRight,
    color = color
  )
  drawDiagonals(
    width = width,
    height = height,
    phase = phase(offset.x + offset.y, stepX),
    stepX = stepX,
    slope = DiagonalSlope.DownLeft,
    color = color
  )
}

/**
 * Одно семейство параллельных диагоналей во всю высоту вьюпорта.
 *
 * Семейство рисуется с запасом: диагональ пересекает вьюпорт по горизонтали ровно на его высоту,
 * поэтому диапазон расширен на эту величину в ту сторону, откуда линии въезжают. Без запаса у
 * кромки, к которой едет камера, появлялась бы пустая полоса шириной в высоту экрана.
 *
 * @param width ширина вьюпорта
 * @param height высота вьюпорта
 * @param phase сдвиг первой линии внутри шага, см. [phase]
 * @param stepX расстояние между соседними линиями вдоль оси X
 * @param slope наклон семейства
 * @param color цвет линий
 */
@Suppress("LongParameterList")
private fun DrawScope.drawDiagonals(
  width: Float,
  height: Float,
  phase: Float,
  stepX: Float,
  slope: DiagonalSlope,
  color: Color
) {
  val down = slope == DiagonalSlope.DownRight
  val from = if (down) -height + phase else phase
  val to = if (down) width else width + height
  var x = from
  while (x <= to) {
    val endX = if (down) x + height else x - height
    drawLine(
      color = color,
      start = Offset(x, 0f),
      end = Offset(endX, height),
      strokeWidth = LINE_WIDTH.toPx()
    )
    x += stepX
  }
}

/**
 * Остаток от деления, приведённый к `[0, step)`.
 *
 * Неотрицательный он нужен потому, что `%` в Kotlin сохраняет знак делимого: у камеры, уехавшей в
 * минус, фаза стала бы отрицательной, первая линия семейства ушла бы за край, и на переходе камеры
 * через ноль паттерн прыгал бы на целый шаг.
 *
 * @param value приводимое значение
 * @param step длина периода
 * @return значение в пределах `[0, step)`
 */
private fun phase(value: Float, step: Float): Float {
  return value - step * floor(value / step)
}

private const val PARALLAX = 0.3f
private const val PATTERN_ALPHA = 0.04f
private val PATTERN_SPACING: Dp = 120.dp
private val LINE_WIDTH: Dp = 1.dp
