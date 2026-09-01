package ru.sla.clarify.feature.chronology.ui.mapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel

/**
 * Таблицы уровней: шаг дорожки и полоса масштаба.
 *
 * Раскладка обзора держится на одном числе — она раскладка эпизодов, ужатая вчетверо по обеим осям,
 * — и тест сторожит именно это правило, а не значение шага. Разъехавшись, оси дали бы карту, у
 * которой вертикаль растянута против горизонтали, и заметить это можно было бы только глазом на
 * устройстве: ни компилятор, ни раскладка о разной степени сжатия ничего не скажут.
 *
 * Нижний край обзора — вторая такая таблица, и её арифметика в библиотеку не уехала: сколько
 * пикселей остаётся от глифа на 0.077×, знает только тот, кто его рисует.
 */
class GraphLevelMappersTest {

  @Test
  fun `the overview lane step is a quarter of the episode one`() {
    assertEquals(
      GraphLevel.LOD0.toLaneStep() / 4,
      GraphLevel.LOD1.toLaneStep(),
      "то же число делит и зазоры: одно правило на всю раскладку уровня"
    )
  }

  @Test
  fun `the overview floor sinks to fit-all but never below the glyph limit`() {
    assertEquals(
      0.305f,
      GraphLevel.LOD1.toScaleBand(fitScale = 0.305f).min,
      1e-4f,
      "на демо-наборе упор обзора приходится ровно на вписанный граф"
    )
    assertEquals(
      0.2f,
      GraphLevel.LOD1.toScaleBand(fitScale = 0.077f).min,
      1e-4f,
      "переписка в двести эпизодов вписалась бы на 0.077×, где глиф вырождается в полтора пикселя"
    )
    assertEquals(
      0.4f,
      GraphLevel.LOD1.toScaleBand(fitScale = 0.9f).min,
      1e-4f,
      "короткая история вписывается и так: ниже общего предела обзор не опускается"
    )
  }
}
