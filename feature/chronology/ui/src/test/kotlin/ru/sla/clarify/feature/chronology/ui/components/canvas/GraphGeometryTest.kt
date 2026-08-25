package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Геометрия вынесена из композабла именно для того, чтобы её можно было проверить без Compose.
 *
 * Тест сторожит регрессии, которые уже случались: место магистрали должно считаться от занятых
 * дорожек, а не от константы, совпавшей с половиной высоты полотна, а зазор должен отделять плашки,
 * а не центры.
 *
 * Камера проверяется отдельно, в [GraphCameraTest]: где ей разрешено быть, выводится из раскладки,
 * но раскладка об этом не знает.
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
  fun `the nearest node is chosen by distance, not by order in the list`() {
    val centres = listOf(
      Offset(x = 0f, y = 0f),
      Offset(x = 1000f, y = 0f),
      Offset(x = 400f, y = 100f)
    )

    assertEquals(
      2,
      nearestCentreIndexOf(centres, x = 380f),
      "узлы упорядочены временем, а не осью, и ближайший к точке может стоять последним"
    )
  }

  @Test
  fun `an empty graph has no nearest node`() {
    assertEquals(-1, nearestCentreIndexOf(emptyList(), x = 100f))
  }
}
