package ru.sla.clarify.openapi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Сторож единственного значения, которым клиент представляется серверу.
 *
 * Константу печатает сборка, и тест читает YAML заново, своим разбором: сойдись он с печатью в
 * одном коде — сверять было бы нечего, обе стороны врали бы одинаково. Разные разборы одного
 * файла расходятся только тогда, когда сломан один из них.
 */
class SpecVersionTest {

  private val spec = File("clarify.yaml")

  @Test
  fun `the generated version constant matches the spec info version`() {
    // Блок info кончается там, где начинается следующий ключ верхнего уровня, то есть первая
    // строка без отступа. Пустые строки внутри блока — часть его, а не конец
    val declared = spec.readLines()
      .dropWhile { it != "info:" }
      .drop(1)
      .takeWhile { it.isBlank() || it.startsWith(" ") }
      .firstNotNullOfOrNull { Regex("""^ {2}version:\s*"?([^"\s]+)"?$""").find(it)?.groupValues?.get(1) }

    assertEquals(declared, SPEC_VERSION)
  }

  @Test
  fun `the spec declares a version that could be compared by major`() {
    // Сервер сверяет мажор, поэтому версия обязана его иметь: строка без точки прошла бы сверку
    // как угодно, и расхождение осталось бы незамеченным ровно там, где его ищут
    assertTrue(SPEC_VERSION.matches(Regex("""\d+\.\d+\.\d+""")), "not a semantic version: $SPEC_VERSION")
  }
}
