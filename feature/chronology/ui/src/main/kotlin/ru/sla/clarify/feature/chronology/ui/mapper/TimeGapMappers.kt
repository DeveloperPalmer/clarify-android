package ru.sla.clarify.feature.chronology.ui.mapper

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel
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
 * На обзоре та же лестница делится на четыре, а не заводится заново пятью новыми числами. Причина не
 * в экономии: делённая лестница сохраняет **пропорции истории**, и засечки мини-карты на переходе
 * переезжают не более чем на 24 dp из 372 против 43 dp у ровного зазора — а мир полосы выводится из
 * раскладки, и другого способа удержать засечки на месте нет.
 *
 * @param level уровень детализации: он же решает, чем меряется зазор
 * @return расстояние от предыдущей плашки
 */
internal fun TimeGap.toStepWidth(level: GraphLevel): Dp {
  val step = when (this) {
    TimeGap.Minutes -> 40.dp
    TimeGap.Hour -> 68.dp
    TimeGap.Hours -> 96.dp
    TimeGap.Day -> 124.dp
    TimeGap.Long -> 152.dp
  }
  return when (level) {
    GraphLevel.Episodes -> step
    GraphLevel.Overview -> step / 4
  }
}
