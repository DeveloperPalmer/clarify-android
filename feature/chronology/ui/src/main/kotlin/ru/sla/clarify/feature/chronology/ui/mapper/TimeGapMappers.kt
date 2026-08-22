package ru.sla.clarify.feature.chronology.ui.mapper

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.feature.chronology.ui.entity.TimeGap

/**
 * Зазор, которым пауза отделяет плашку от предыдущей.
 *
 * Пять дискретных значений вместо реальной длительности: абсолютное время растянуло бы ночную паузу
 * на километры пустоты, а одинаковый зазор стёр бы паузы вовсе. Дискретный зазор оставляет порядок
 * величины, а точную длительность выводит подпись.
 *
 * Значения обязаны монотонно расти по длительности — это и проверяет тест: обратный порядок
 * означал бы, что пауза покороче раздвинула узлы сильнее длинной.
 *
 * @return расстояние от предыдущей плашки
 */
internal fun TimeGap.toStepWidth(): Dp {
  return when (this) {
    TimeGap.Minutes -> 40.dp
    TimeGap.Hour -> 68.dp
    TimeGap.Hours -> 96.dp
    TimeGap.Day -> 124.dp
    TimeGap.Long -> 152.dp
  }
}
