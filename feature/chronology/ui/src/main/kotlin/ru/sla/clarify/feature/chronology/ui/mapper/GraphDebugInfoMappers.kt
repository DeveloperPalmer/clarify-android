package ru.sla.clarify.feature.chronology.ui.mapper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import ru.sla.clarify.feature.chronology.ui.entity.GraphDebugInfo
import ru.sla.clarify.feature.chronology.ui.entity.GraphDebugRow
import kotlin.math.roundToInt

/**
 * Снимок полотна в строки отладочной панели.
 *
 * Числа округляются до целых пикселей: доли пикселя в диагностике ничего не решают, а строку вроде
 * `Rect.fromLTRB(252.0, 100.0, 2629.0, 447.0)` в колонку не уложить.
 *
 * @param lastPan последнее приращение жеста
 * @param tickMillis период, с которым панель снимает счётчики
 * @return строки правой колонки панели
 */
internal fun GraphDebugInfo.toFactRows(lastPan: Offset, tickMillis: Long): List<GraphDebugRow> {
  return listOf(
    GraphDebugRow("viewport", "$viewportWidth × $viewportHeight"),
    GraphDebugRow(
      label = "camera",
      value = lineOf(camera) + if (isCameraMoved) "" else " · rest",
      isAnomalous = !camera.isValid()
    ),
    GraphDebugRow("bounds", lineOf(contentBounds)),
    GraphDebugRow(
      label = "span x",
      value = "${centreSpanX.start.roundToInt()} … ${centreSpanX.endInclusive.roundToInt()}"
    ),
    GraphDebugRow(
      label = "nodes",
      value = "$nodeCount · edges $edgeCount",
      isAnomalous = nodeCount > 0 && contentBounds.isEmpty
    ),
    GraphDebugRow("last pan", lineOf(lastPan)),
    GraphDebugRow("tick", "$tickMillis ms")
  )
}

private fun lineOf(offset: Offset): String {
  if (!offset.isValid()) {
    return offset.toString()
  }
  return "${offset.x.roundToInt()}, ${offset.y.roundToInt()}"
}

private fun lineOf(rect: Rect): String {
  return "${rect.left.roundToInt()}, ${rect.top.roundToInt()} … " +
    "${rect.right.roundToInt()}, ${rect.bottom.roundToInt()}"
}
