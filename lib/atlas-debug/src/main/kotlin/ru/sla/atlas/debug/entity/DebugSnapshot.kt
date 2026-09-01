package ru.sla.atlas.debug.entity

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import ru.sla.atlas.entity.DebugInfo
import ru.sla.atlas.entity.Telemetry

/**
 * Худший такт за смену: пиковые скорости фаз и полотно в тот момент.
 *
 * Числа и обстановка держатся вместе намеренно. Пиковое значение само по себе отвечает «стало
 * плохо», но не «на чём»; рядом с камерой и числом узлов оно отвечает на оба вопроса. Разложенные
 * по отдельным переменным, эти величины обновлялись бы четырьмя присваиваниями подряд, и первая же
 * забытая ветка дала бы снимок из разных моментов — то есть ровно то расхождение, ради чтения
 * которого панель и открывают.
 *
 * @param L уровень детализации вызывающего
 * @param rates пиковые скорости, по каждой фазе своя
 * @param totals накопленные счётчики на момент пика
 * @param info камера и раскладка на момент пика
 * @param lastPan последнее приращение жеста на момент пика
 * @param lastFling скорость последнего отпускания на момент пика
 */
@Immutable
data class DebugSnapshot<out L>(
  val rates: Telemetry,
  val totals: Telemetry,
  val info: DebugInfo<L>,
  val lastPan: Offset,
  val lastFling: Velocity
)
