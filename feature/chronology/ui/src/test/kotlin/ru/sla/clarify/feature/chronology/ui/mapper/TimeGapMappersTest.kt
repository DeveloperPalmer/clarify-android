package ru.sla.clarify.feature.chronology.ui.mapper

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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

    ordered.zipWithNext { shorter, longer ->
      assertTrue(
        longer.toStepWidth() > shorter.toStepWidth(),
        "пауза $longer обязана давать зазор больше, чем $shorter"
      )
    }
  }
}
