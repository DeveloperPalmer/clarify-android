package ru.sla.clarify.feature.chronology.ui.mapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel

/**
 * Раскладка обзора держится на одном числе: она — раскладка эпизодов, ужатая вчетверо по обеим осям.
 *
 * Тест сторожит именно это правило, а не значение шага. Разъехавшись, оси дали бы карту, у которой
 * вертикаль растянута против горизонтали, и заметить это можно было бы только глазом на устройстве:
 * ни компилятор, ни раскладка о разной степени сжатия ничего не скажут.
 */
class GraphLevelMappersTest {

  @Test
  fun `the overview lane step is a quarter of the episode one`() {
    assertEquals(
      GraphLevel.Episodes.toLaneStep() / 4,
      GraphLevel.Overview.toLaneStep(),
      "то же число делит и зазоры: одно правило на всю раскладку уровня"
    )
  }
}
