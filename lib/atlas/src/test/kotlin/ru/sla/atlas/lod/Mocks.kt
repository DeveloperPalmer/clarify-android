package ru.sla.atlas.lod

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.atlas.entity.ScaleBand
import ru.sla.atlas.entity.TimeGap

/** Уровни детализации вызывающего: два, от обзорного к подробному. */
internal enum class MockLevel {
  Coarse,
  Fine
}

/**
 * Лестница из двух уровней с круглыми числами.
 *
 * Числа здесь **свои**, а не переписанные у настоящего вызывающего, и это не мелочь: библиотека
 * проверяется на том, что она умеет, а не на том, какие пороги выбрала одна фича. Совпади они —
 * тест перестал бы отличать «арифметика верна» от «таблица та же».
 */
internal object MockLevels : LevelScheme<MockLevel> {

  override fun laneStepOf(level: MockLevel): Dp {
    return when (level) {
      MockLevel.Coarse -> 25.dp
      MockLevel.Fine -> 100.dp
    }
  }

  override fun stepWidthOf(level: MockLevel, gap: TimeGap): Dp {
    return laneStepOf(level)
  }

  override fun bandOf(level: MockLevel, fitScale: Float): ScaleBand {
    return when (level) {
      MockLevel.Coarse -> ScaleBand(min = fitScale.coerceIn(0.2f, 0.5f), max = 2f)
      MockLevel.Fine -> ScaleBand(min = 0.5f, max = 2f)
    }
  }

  override fun restScaleOf(level: MockLevel, band: ScaleBand): Float {
    return when (level) {
      MockLevel.Coarse -> band.min
      MockLevel.Fine -> 1f
    }
  }

  override fun coarserThan(level: MockLevel): MockLevel? {
    return MockLevel.Coarse.takeIf { level == MockLevel.Fine }
  }

  override fun finerThan(level: MockLevel): MockLevel? {
    return MockLevel.Fine.takeIf { level == MockLevel.Coarse }
  }
}

/**
 * Полотно без уровней детализации: вырожденная схема из одного уровня.
 *
 * Заведена затем, чтобы проверить обещание [LevelScheme] — что такое полотно не требует в движке
 * отдельной ветки. Соседей нет, и выход за полосу становится обычным упором сам собой.
 */
internal object MockSingleLevel : LevelScheme<Unit> {

  override fun laneStepOf(level: Unit): Dp {
    return 100.dp
  }

  override fun stepWidthOf(level: Unit, gap: TimeGap): Dp {
    return 40.dp
  }

  override fun bandOf(level: Unit, fitScale: Float): ScaleBand {
    return ScaleBand(min = 0.5f, max = 2f)
  }

  override fun restScaleOf(level: Unit, band: ScaleBand): Float {
    return 1f
  }

  override fun coarserThan(level: Unit): Unit? {
    return null
  }

  override fun finerThan(level: Unit): Unit? {
    return null
  }
}
