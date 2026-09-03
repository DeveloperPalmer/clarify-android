package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.ui.unit.Dp
import ru.sla.atlas.entity.ScaleBand
import ru.sla.atlas.entity.TimeGap
import ru.sla.atlas.lod.LevelScheme
import ru.sla.clarify.feature.chronology.ui.mapper.toLaneStep
import ru.sla.clarify.feature.chronology.ui.mapper.toScaleBand
import ru.sla.clarify.feature.chronology.ui.mapper.toStepWidth

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
    return when (level) {
      GraphLevel.LOD0 -> 1f
      GraphLevel.LOD1 -> band.min
    }
  }

  override fun coarserThan(level: GraphLevel): GraphLevel? {
    return when (level) {
      GraphLevel.LOD0 -> GraphLevel.LOD1
      GraphLevel.LOD1 -> null
    }
  }

  override fun finerThan(level: GraphLevel): GraphLevel? {
    return when (level) {
      GraphLevel.LOD1 -> GraphLevel.LOD0
      GraphLevel.LOD0 -> null
    }
  }
}
