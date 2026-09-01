package ru.sla.atlas.debug.mapper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Velocity
import ru.sla.atlas.debug.entity.DebugRow
import ru.sla.atlas.entity.DebugInfo
import kotlin.math.roundToInt

/**
 * Снимок полотна в строки отладочной панели.
 *
 * Числа округляются до целых пикселей: доли пикселя в диагностике ничего не решают, а строку вроде
 * `Rect.fromLTRB(252.0, 100.0, 2629.0, 447.0)` в колонку не уложить.
 *
 * @param labelOf имя уровня детализации словами: как называются уровни, знает только тот, у кого
 *   они есть
 * @param lastPan последнее приращение жеста
 * @param lastFling скорость последнего отпускания
 * @param tickMillis период, с которым панель снимает счётчики
 * @return строки правой колонки панели
 */
fun <L> DebugInfo<L>.toFactRows(
  labelOf: (L) -> String,
  lastPan: Offset,
  lastFling: Velocity,
  tickMillis: Long
): List<DebugRow> {
  return listOf(
    DebugRow("viewport", "$viewportWidth × $viewportHeight"),
    DebugRow(
      label = "camera",
      value = lineOf(camera) + if (isCameraMoved) "" else " · rest",
      isAnomalous = !camera.isValid()
    ),
    // Масштаб — в процентах, а не кратностью: панель считает целыми числами везде, а «40 %» и
    // «250 %» читаются с той же дистанции, что и остальные строки.
    DebugRow(
      label = "scale",
      value = "${(scale * PERCENT).roundToInt()} %",
      isAnomalous = !scale.isFinite() || scale <= 0f
    ),
    // Уровень и его полоса стоят рядом с масштабом: без них «40 %» ничего не значит — тот же
    // масштаб на обзорном уровне показывает в разы больше истории, чем на подробном.
    DebugRow(
      label = "lod",
      value = "${labelOf(level)} · ${(levelBand.min * PERCENT).roundToInt()} … " +
        "${(levelBand.max * PERCENT).roundToInt()} %"
    ),
    DebugRow("bounds", lineOf(contentBounds)),
    DebugRow(
      label = "span x",
      value = "${centreSpanX.start.roundToInt()} … ${centreSpanX.endInclusive.roundToInt()}"
    ),
    DebugRow(
      label = "nodes",
      value = "$nodeCount · edges $edgeCount",
      isAnomalous = nodeCount > 0 && contentBounds.isEmpty
    ),
    DebugRow("last pan", lineOf(lastPan)),
    // Скорость отпускания пишется до отбраковки слабого броска: ноль здесь при живом жесте означает,
    // что трекер остался без точек, а не что палец вели медленно. Различить это по поведению
    // картинки нельзя — в обоих случаях инерции просто нет.
    DebugRow(
      label = "last v",
      value = "${lastFling.x.roundToInt()}, ${lastFling.y.roundToInt()}"
    ),
    DebugRow("tick", "$tickMillis ms")
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

private const val PERCENT = 100f
