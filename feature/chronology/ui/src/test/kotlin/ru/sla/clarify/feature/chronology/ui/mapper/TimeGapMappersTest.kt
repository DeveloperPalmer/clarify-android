package ru.sla.clarify.feature.chronology.ui.mapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel
import ru.sla.clarify.feature.chronology.ui.entity.TimeGap

/**
 * Таблица зазоров задана вручную, а не формулой, поэтому её монотонность ничем не гарантирована —
 * ровно это и проверяется. Перепутанные местами значения выглядели бы на экране правдоподобно:
 * узлы стоят, связи есть, а порядок величин врёт.
 */
class TimeGapMappersTest {

  @Test
  fun `a longer pause never gives a smaller gap`() {
    val ordered = listOf(TimeGap.Minutes, TimeGap.Hour, TimeGap.Hours, TimeGap.Day, TimeGap.Long)

    GraphLevel.entries.forEach { level ->
      ordered.zipWithNext { shorter, longer ->
        assertTrue(
          longer.toStepWidth(level) > shorter.toStepWidth(level),
          "пауза $longer обязана давать зазор больше, чем $shorter, и на уровне $level тоже"
        )
      }
    }
  }

  @Test
  fun `overview gaps are a quarter of the episode ones`() {
    TimeGap.entries.forEach { gap ->
      assertEquals(
        gap.toStepWidth(GraphLevel.Episodes) / 4,
        gap.toStepWidth(GraphLevel.Overview),
        "обзор — та же лестница, делённая на четыре: делённая сохраняет пропорции истории, " +
          "а заведённая заново уводит засечки мини-карты вдвое сильнее"
      )
    }
  }
}
