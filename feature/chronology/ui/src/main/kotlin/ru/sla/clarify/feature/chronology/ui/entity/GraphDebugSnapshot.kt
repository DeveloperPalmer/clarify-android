package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity

/**
 * Худший такт за смену: пиковые скорости фаз и полотно в тот момент.
 *
 * Числа и обстановка держатся вместе намеренно. Пиковое значение само по себе отвечает «стало
 * плохо», но не «на чём»; рядом с камерой и числом узлов оно отвечает на оба вопроса. Разложенные
 * по отдельным переменным, эти величины обновлялись бы четырьмя присваиваниями подряд, и первая же
 * забытая ветка дала бы снимок из разных моментов — то есть ровно то расхождение, ради чтения
 * которого панель и открывают.
 *
 * @param rates пиковые скорости, по каждой фазе своя
 * @param totals накопленные счётчики на момент пика
 * @param info камера и раскладка на момент пика
 * @param lastPan последнее приращение жеста на момент пика
 * @param lastFling скорость последнего отпускания на момент пика
 */
@Immutable
data class GraphDebugSnapshot(
  val rates: GraphTelemetry,
  val totals: GraphTelemetry,
  val info: GraphDebugInfo,
  val lastPan: Offset,
  val lastFling: Velocity
)
