package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.unit.Dp
import ru.sla.atlas.entity.ScaleBand
import ru.sla.atlas.entity.TimeGap
import ru.sla.atlas.lod.LevelScheme
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel
import ru.sla.clarify.feature.chronology.ui.mapper.toLaneStep
import ru.sla.clarify.feature.chronology.ui.mapper.toRestScale
import ru.sla.clarify.feature.chronology.ui.mapper.toScaleBand
import ru.sla.clarify.feature.chronology.ui.mapper.toStepWidth

/**
 * Уровни детализации хронологии глазами полотна — §5 брифа.
 *
 * Переходник, а не таблица: метрики он берёт у мапперов, где они и жили, и добавляет к ним ровно
 * одно знание, которого у отдельных мапперов не было и быть не могло, — кто чей сосед. Обзор
 * крупнее эпизодов по охвату, эпизоды подробнее обзора, и третьего уровня нет: `null` у обоих краёв
 * означает, что выход за полосу там — обычный упор, а не переход.
 *
 * Третий уровень — отдельные сообщения внутри раскрытого эпизода — появится строкой здесь и строкой
 * в каждом мапере, и `when` без ветки `else` не даст об этом забыть.
 */
internal object ChronologyLevels : LevelScheme<GraphLevel> {

  override fun laneStepOf(level: GraphLevel): Dp {
    return level.toLaneStep()
  }

  override fun stepWidthOf(level: GraphLevel, gap: TimeGap): Dp {
    return gap.toStepWidth(level)
  }

  override fun bandOf(level: GraphLevel, fitScale: Float): ScaleBand {
    return level.toScaleBand(fitScale)
  }

  override fun restScaleOf(level: GraphLevel, band: ScaleBand): Float {
    return level.toRestScale(band)
  }

  override fun coarserThan(level: GraphLevel): GraphLevel? {
    return when (level) {
      GraphLevel.Episodes -> GraphLevel.Overview
      GraphLevel.Overview -> null
    }
  }

  override fun finerThan(level: GraphLevel): GraphLevel? {
    return when (level) {
      GraphLevel.Overview -> GraphLevel.Episodes
      GraphLevel.Episodes -> null
    }
  }
}
