package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel
import ru.sla.clarify.feature.chronology.ui.mapper.toLaneStep

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
  fun `top lane is never below the baseline`() {
    assertEquals(0, topLaneOf(listOf(0, 1, 2)), "магистраль учитывается всегда")
    assertEquals(-2, topLaneOf(listOf(-2, -1, 0)))
    assertEquals(0, topLaneOf(emptyList()))
  }

  @Test
  fun `baseline shifts down when lanes are occupied above it`() {
    val alone = GraphGeometry(topLaneOf(listOf(0)), GraphLevel.Episodes.toLaneStep())
    val withLaneAbove = GraphGeometry(topLaneOf(listOf(-1, 0)), GraphLevel.Episodes.toLaneStep())

    assertTrue(
      withLaneAbove.laneYOf(0) > alone.laneYOf(0),
      "магистраль обязана уехать вниз, освободив место дорожке сверху"
    )
  }

  @Test
  fun `lane offsets follow the step they are given`() {
    val episodes = GraphGeometry(topLaneOf(listOf(-1, 0)), GraphLevel.Episodes.toLaneStep())
    val overview = GraphGeometry(topLaneOf(listOf(-1, 0)), GraphLevel.Overview.toLaneStep())

    assertEquals(
      episodes.laneYOf(0) / 4,
      overview.laneYOf(0),
      "обзор ужимает вертикаль тем же числом, что и зазоры: раскладка уровня — одно правило, " +
        "а не две независимые таблицы"
    )
  }

  @Test
  fun `lane step is the same between any two neighbours`() {
    val geometry = GraphGeometry(topLaneOf(listOf(-1, 0, 1)), GraphLevel.Episodes.toLaneStep())

    assertEquals(
      geometry.laneYOf(0) - geometry.laneYOf(-1),
      geometry.laneYOf(1) - geometry.laneYOf(0)
    )
  }

  @Test
  fun `topmost lane stays inside the canvas`() {
    val geometry = GraphGeometry(topLaneOf(listOf(-2, 1)), GraphLevel.Episodes.toLaneStep())

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

  @Test
  fun `a node rect is the canvas rect scaled and shifted by the camera`() {
    val rect = nodeRectOf(
      topLeft = IntOffset(x = 1000, y = 300),
      size = IntSize(width = 200, height = 72),
      camera = Offset(x = -800f, y = -100f),
      scale = 1f
    )

    assertEquals(Rect(left = 200f, top = 200f, right = 400f, bottom = 272f), rect)
  }

  /**
   * Морф стартует из **нарисованной** плашки, а не из её размера в раскладке: на верхней границе
   * диапазона это разница между 200 × 72 и 500 × 180.
   */
  @Test
  fun `a node rect grows with the scale`() {
    val topLeft = IntOffset(x = 1000, y = 300)
    val size = IntSize(width = 200, height = 72)

    val plain = nodeRectOf(topLeft, size, camera = Offset.Zero, scale = 1f)
    val zoomed = nodeRectOf(topLeft, size, camera = Offset.Zero, scale = 2.5f)

    assertEquals(2.5f * plain.width, zoomed.width)
    assertEquals(2.5f * plain.height, zoomed.height)
    assertEquals(2.5f * plain.left, zoomed.left, "масштаб отсчитывается от угла вьюпорта")
  }

  @Test
  fun `a node rect at the origin of an untouched camera is the plate itself`() {
    val rect = nodeRectOf(
      topLeft = IntOffset(x = 40, y = 12),
      size = IntSize(width = 24, height = 24),
      camera = Offset.Zero,
      scale = 1f
    )

    assertEquals(Rect(left = 40f, top = 12f, right = 64f, bottom = 36f), rect)
  }
}
