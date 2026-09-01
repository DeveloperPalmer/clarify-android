package ru.sla.atlas.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Сторожит уговор нулевого оттенка: он принадлежит магистрали, и ветка его не берёт никогда.
 *
 * Наивный остаток от деления соблюдает этот уговор для всех веток, кроме тех, чей номер делится на
 * размер палитры, — а такая ветка появляется не в краевом случае, а на шестой теме беседы, и
 * получает цвет ствола молча.
 */
class BranchTest {

  @Test
  fun `a branch never takes the colour of the baseline`() {
    assertTrue(
      (1..24).all { order -> Branch.colorIndexOf(order, paletteSize = PALETTE) != 0 },
      "ноль оставлен магистрали: `order.mod(paletteSize)` красил бы шестую ветку цветом ствола"
    )
  }

  @Test
  fun `colours run from one to the size of the palette`() {
    assertEquals(1, Branch.colorIndexOf(order = 1, paletteSize = PALETTE))
    assertEquals(PALETTE, Branch.colorIndexOf(order = PALETTE, paletteSize = PALETTE))
  }

  @Test
  fun `the palette repeats once its colours run out`() {
    assertEquals(
      1,
      Branch.colorIndexOf(order = PALETTE + 1, paletteSize = PALETTE),
      "повтор неизбежен: счёт веток идёт за всю жизнь беседы, а оттенков конечное число"
    )
  }

  @Test
  fun `a palette of one colour still leaves the baseline alone`() {
    assertTrue(
      (1..5).all { order -> Branch.colorIndexOf(order, paletteSize = 1) == 1 },
      "вырожденная палитра — не краевой случай: библиотека не знает, сколько оттенков у вызывающего"
    )
  }
}

// Шесть оттенков — палитра хронологии, на которой уговор и проверяется. Библиотека своего числа не
// имеет: его объявляет тот, кто рисует.
private const val PALETTE = 6
