package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.feature.chronology.ui.entity.TimeGap

/**
 * Система координат полотна хронологии.
 *
 * Ось Y — принадлежность ветке, а не время и не иерархия: дорожка переиспользуется после слияния,
 * поэтому две разные ветки могут стоять на одном Y и обязаны различаться цветом идентичности.
 * Ось X — порядковое время, см. [TimeGap].
 */
object GraphGeometry {

  /** Магистраль — корневая ветка беседы. Дорожки нумеруются от неё: −1, −2 вверх, +1, +2 вниз. */
  val TrunkY: Dp = 310.dp

  /** Плашка узла 64–72 плюс воздух 32–40. При семи и более живых ветках полотно скроллится, шаг не ужимается. */
  val LaneStep: Dp = 104.dp

  fun laneY(index: Int): Dp = TrunkY + LaneStep * index

  fun stepWidth(gap: TimeGap): Dp = when (gap) {
    TimeGap.Minutes -> 40.dp
    TimeGap.Hour -> 68.dp
    TimeGap.Hours -> 96.dp
    TimeGap.Day -> 124.dp
    TimeGap.Long -> 152.dp
  }
}
