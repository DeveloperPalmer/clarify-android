package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.clarify.feature.chronology.ui.entity.TimeGap

/**
 * Геометрия вынесена из композабла именно для того, чтобы её можно было проверить без Compose.
 *
 * Тест сторожит регрессии, которые уже случались: место магистрали должно считаться от занятых
 * дорожек, а не от константы, совпавшей с половиной высоты полотна; зазор должен отделять плашки, а
 * не центры; кламп камеры должен вырождаться в центрирование, когда содержимое помещается целиком, и
 * в ноль, когда содержимого нет.
 */
class GraphGeometryTest {

  @Test
  fun `top lane is never below the trunk`() {
    assertEquals(0, topLaneOf(listOf(0, 1, 2)), "магистраль учитывается всегда")
    assertEquals(-2, topLaneOf(listOf(-2, -1, 0)))
    assertEquals(0, topLaneOf(emptyList()))
  }

  @Test
  fun `trunk shifts down when lanes are occupied above it`() {
    val alone = GraphGeometry(topLaneOf(listOf(0)))
    val withLaneAbove = GraphGeometry(topLaneOf(listOf(-1, 0)))

    assertTrue(
      withLaneAbove.laneYOf(0) > alone.laneYOf(0),
      "магистраль обязана уехать вниз, освободив место дорожке сверху"
    )
  }

  @Test
  fun `lane step is the same between any two neighbours`() {
    val geometry = GraphGeometry(topLaneOf(listOf(-1, 0, 1)))

    assertEquals(
      geometry.laneYOf(0) - geometry.laneYOf(-1),
      geometry.laneYOf(1) - geometry.laneYOf(0)
    )
  }

  @Test
  fun `topmost lane stays inside the canvas`() {
    val geometry = GraphGeometry(topLaneOf(listOf(-2, 1)))

    assertTrue(
      geometry.laneYOf(-2) > 0.dp,
      "самая верхняя дорожка не должна уходить за верх полотна"
    )
  }

  @Test
  fun `left offsets accumulate gaps and widths`() {
    val lefts = leftOffsetsOf(
      gaps = listOf(10f, 20f, 30f),
      widths = listOf(100f, 200f, 300f)
    )

    assertEquals(listOf(10f, 130f, 360f), lefts)
  }

  @Test
  fun `a plate wider than the gap does not swallow its neighbour`() {
    val gaps = listOf(0f, 40f)
    val widths = listOf(600f, 100f)

    val lefts = leftOffsetsOf(gaps, widths)

    assertEquals(
      40f,
      lefts[1] - (lefts[0] + widths[0]),
      "зазор отделяет плашки, а не центры: иначе широкая плашка накрыла бы соседнюю"
    )
  }

  @Test
  fun `a longer pause never gives a smaller gap`() {
    val ordered = listOf(TimeGap.Minutes, TimeGap.Hour, TimeGap.Hours, TimeGap.Day, TimeGap.Long)

    ordered.zipWithNext { shorter, longer ->
      assertTrue(
        stepWidthOf(longer) > stepWidthOf(shorter),
        "пауза $longer обязана давать зазор больше, чем $shorter"
      )
    }
  }

  @Test
  fun `pan range spans content wider than the viewport`() {
    val range = panRangeOf(min = 0f, max = 1000f, viewport = 400f)

    assertEquals(-600f, range.start)
    assertEquals(0f, range.endInclusive)
  }

  @Test
  fun `pan range reaches a node hanging left of the canvas`() {
    val range = panRangeOf(min = -30f, max = 1000f, viewport = 400f)

    assertEquals(
      30f,
      range.endInclusive,
      "выступ узла за левый край обязан оставаться достижимым прокруткой"
    )
  }

  @Test
  fun `pan range collapses to a centring value when content fits`() {
    val range = panRangeOf(min = 0f, max = 200f, viewport = 400f)

    assertEquals(range.start, range.endInclusive, "помещающееся содержимое незачем прижимать к краю")
    assertEquals(100f, range.start)
  }

  @Test
  fun `timeline rests with the leftmost plate centred`() {
    val range = timelinePanRangeOf(centreSpanX = 60f..900f, viewport = 1000f)

    assertEquals(
      500f - 60f,
      range.endInclusive,
      "в покое камера наводится на самый левый узел"
    )
  }

  @Test
  fun `timeline ends with the rightmost plate centred`() {
    val range = timelinePanRangeOf(centreSpanX = 60f..900f, viewport = 1000f)

    assertEquals(
      500f - 900f,
      range.start,
      "докрутив вправо до упора, пользователь видит последний узел в центре"
    )
  }

  @Test
  fun `a single plate is centred and cannot be panned away`() {
    val range = timelinePanRangeOf(centreSpanX = 60f..60f, viewport = 1000f)

    assertEquals(range.start, range.endInclusive)
    assertEquals(440f, range.start)
  }

  @Test
  fun `pan range stays at zero without content`() {
    val range = panRangeOf(min = 0f, max = 0f, viewport = 400f)

    assertEquals(0f, range.start, "пустое полотно не должно уезжать на пол-экрана")
    assertEquals(0f, range.endInclusive)
  }
}
