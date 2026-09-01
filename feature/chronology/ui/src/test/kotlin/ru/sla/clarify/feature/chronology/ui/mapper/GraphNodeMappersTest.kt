package ru.sla.clarify.feature.chronology.ui.mapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Проверяется чистая половина маппера — сборка частей.
 *
 * Формулировки берутся из ресурсов, а `@Composable`-функция юнит-тестом не покрывается: Robolectric
 * в проекте нет. Зато сборка — обычная арифметика над строками, и ошибиться в ней можно ровно один
 * раз: оставить в подписи дырку от части, которой нет. Скринридер прочитает такую дырку вслух.
 */
class GraphNodeMappersTest {

  @Test
  fun `parts are joined in the order they were given`() {
    val description = nodeDescriptionOf(listOf("Эпизод", "12 сообщений", "14 марта"))

    assertEquals("Эпизод, 12 сообщений, 14 марта", description)
  }

  @Test
  fun `a missing part leaves no hole`() {
    val description = nodeDescriptionOf(listOf("Эпизод", null, "14 марта"))

    assertEquals(
      "Эпизод, 14 марта",
      description,
      "у узла магистрали нет ветки, и подпись не должна произносить пустоту на её месте"
    )
  }

  @Test
  fun `a blank part counts as missing`() {
    val description = nodeDescriptionOf(listOf("Слияние", "", "  "))

    assertEquals("Слияние", description, "пустая строка ресурса — та же дырка, что и null")
  }

  @Test
  fun `nothing to say is an empty description, not a comma`() {
    assertEquals("", nodeDescriptionOf(listOf(null, null)))
  }
}
