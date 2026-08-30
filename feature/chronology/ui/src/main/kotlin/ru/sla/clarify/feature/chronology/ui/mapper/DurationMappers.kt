package ru.sla.clarify.feature.chronology.ui.mapper

import ru.sla.clarify.feature.chronology.ui.entity.TimeGap
import java.time.Duration

/**
 * Пауза между соседними узлами — в ступень зазора.
 *
 * Ступени, а не длительность: ось X полотна — порядковое время, и реальная пауза растянула бы ночь
 * на километры пустоты, а одинаковый зазор стёр бы паузы вовсе (см. [TimeGap]).
 *
 * Границы включающие: ровно пять минут — это ещё «реплики одной очереди», а не следующая ступень.
 * Отрицательная длительность невозможна по построению — узлы приходят сюда отсортированными, — но
 * попади она сюда, ступень будет наименьшей, а не наибольшей.
 *
 * @return ступень зазора, которой пауза отделит узел от предыдущего
 */
internal fun Duration.toTimeGap(): TimeGap {
  return when {
    this <= Duration.ofMinutes(5) -> TimeGap.Minutes
    this <= Duration.ofHours(1) -> TimeGap.Hour
    this <= Duration.ofHours(6) -> TimeGap.Hours
    this <= Duration.ofDays(1) -> TimeGap.Day
    else -> TimeGap.Long
  }
}
