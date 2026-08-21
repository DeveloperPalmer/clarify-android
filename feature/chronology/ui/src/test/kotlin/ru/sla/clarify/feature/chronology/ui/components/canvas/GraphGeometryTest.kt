package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.TimeGap

/**
 * Геометрия вынесена из композабла именно для того, чтобы её можно было проверить без Compose.
 *
 * Тест сторожит три регрессии, каждая из которых уже случалась или ждала своего часа: ось X должна
 * выводиться из паузы, а не из литералов на экране; место магистрали должно считаться от занятых
 * дорожек, а не от константы, совпавшей с половиной высоты полотна; кламп камеры должен вырождаться
 * в центрирование, когда содержимое помещается целиком, а не запирать его у края.
 */
class GraphGeometryTest {

  @Test
  fun `x accumulates step widths of gaps`() {
    val geometry = GraphGeometry(
      listOf(
        node(gap = TimeGap.Minutes),
        node(gap = TimeGap.Hours),
        node(gap = TimeGap.Long)
      )
    )

    assertEquals(
      listOf(40.dp, 40.dp + 96.dp, 40.dp + 96.dp + 152.dp),
      geometry.offsetsX()
    )
  }

  @Test
  fun `lane range always contains the trunk`() {
    val geometry = GraphGeometry(listOf(node(lane = -2), node(lane = -1)))

    assertEquals(-2..0, geometry.laneRange())
  }

  @Test
  fun `trunk shifts down when lanes are occupied above it`() {
    val alone = GraphGeometry(listOf(node(lane = 0)))
    val withLaneAbove = GraphGeometry(listOf(node(lane = -1), node(lane = 0)))

    assertTrue(
      withLaneAbove.laneYOf(0) > alone.laneYOf(0),
      "магистраль обязана уехать вниз, освободив место дорожке сверху"
    )
  }

  @Test
  fun `lane step is the same between any two neighbours`() {
    val geometry = GraphGeometry(listOf(node(lane = -1), node(lane = 0), node(lane = 1)))

    assertEquals(
      geometry.laneYOf(0) - geometry.laneYOf(-1),
      geometry.laneYOf(1) - geometry.laneYOf(0)
    )
  }

  @Test
  fun `topmost lane stays inside the canvas`() {
    val geometry = GraphGeometry(listOf(node(lane = -2), node(lane = 1)))

    assertTrue(
      geometry.laneYOf(-2) > 0.dp,
      "самая верхняя дорожка не должна уходить за верх полотна"
    )
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

  private fun node(lane: Int = 0, gap: TimeGap = TimeGap.Minutes): GraphNode {
    return GraphNode(id = GraphNode.Id("node-$lane-$gap"), lane = lane, gap = gap)
  }
}
