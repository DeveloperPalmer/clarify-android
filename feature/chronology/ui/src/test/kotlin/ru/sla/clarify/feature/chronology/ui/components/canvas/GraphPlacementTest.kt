package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.clarify.feature.chronology.ui.entity.GraphPlacement

/**
 * Раскладка вынесена в чистую функцию потому, что все регрессии этой фичи жили именно здесь:
 * содержимое центрировалось мимо камеры, дорожки группировались по координате, зазор применялся к
 * центрам и плашки наезжали друг на друга. Каждая из них ниже — утверждение, а не наблюдение на
 * скриншоте.
 */
class GraphPlacementTest {

  @Test
  fun `the first node is centred in the viewport`() {
    val placement = placementOf(lanes = listOf(0, 0), gaps = listOf(0f, 50f))

    val centre = placement.nodes[0].x + NODE_WIDTH / 2
    assertEquals(VIEWPORT_WIDTH / 2, centre, "начало истории встаёт в центр экрана")
  }

  @Test
  fun `plates never overlap whatever the gap`() {
    val placement = placementOf(lanes = listOf(0, 0, 0), gaps = listOf(0f, 1f, 2f))

    placement.nodes.zipWithNext { left, right ->
      assertTrue(
        right.x >= left.x + NODE_WIDTH,
        "плашки обязаны стоять рядом, а не друг на друге"
      )
    }
  }

  @Test
  fun `an edge is as long as the pause that made it`() {
    val placement = placementOf(lanes = listOf(0, 0), gaps = listOf(0f, 70f))

    assertEquals(1, placement.edges.size)
    assertEquals(70f, placement.edges[0].endX - placement.edges[0].startX)
  }

  @Test
  fun `the last node of a lane has no trailing edge`() {
    val placement = placementOf(lanes = listOf(0, 0, 0), gaps = listOf(0f, 40f, 40f))

    assertEquals(2, placement.edges.size, "три узла дают ровно две связи, а не три")
  }

  @Test
  fun `nodes of different lanes are never connected`() {
    val placement = placementOf(lanes = listOf(0, 1), gaps = listOf(0f, 40f))

    assertTrue(placement.edges.isEmpty(), "связь только внутри дорожки")
  }

  @Test
  fun `a lane reconnects across a node of another lane`() {
    val placement = placementOf(lanes = listOf(0, 1, 0), gaps = listOf(0f, 40f, 40f))

    assertEquals(1, placement.edges.size, "магистраль продолжается через узел ветки")
  }

  @Test
  fun `an edge sits on the lane, not on a plate`() {
    val tall = IntSize(NODE_WIDTH, 41)
    val short = IntSize(NODE_WIDTH, 28)

    val placement = graphPlacementOf(
      lanes = listOf(0, 0),
      gaps = listOf(0f, 40f),
      laneYs = listOf(LANE_Y, LANE_Y),
      sizes = listOf(tall, short),
      viewportWidth = VIEWPORT_WIDTH
    )

    assertEquals(
      LANE_Y,
      placement.edges[0].y,
      "y ребра берётся у дорожки: у плашек разной высоты центры расходятся на пиксель"
    )
  }

  @Test
  fun `bounds wrap every plate`() {
    val placement = placementOf(lanes = listOf(0, 0), gaps = listOf(0f, 40f))

    assertEquals(placement.nodes[0].x.toFloat(), placement.bounds.left)
    assertEquals((placement.nodes[1].x + NODE_WIDTH).toFloat(), placement.bounds.right)
  }

  @Test
  fun `an empty graph places nothing`() {
    val placement = graphPlacementOf(
      lanes = emptyList(),
      gaps = emptyList(),
      laneYs = emptyList(),
      sizes = emptyList(),
      viewportWidth = VIEWPORT_WIDTH
    )

    assertEquals(GraphPlacement.Empty, placement)
  }

  private fun placementOf(lanes: List<Int>, gaps: List<Float>): GraphPlacement {
    return graphPlacementOf(
      lanes = lanes,
      gaps = gaps,
      laneYs = lanes.map { LANE_Y + it * LANE_Y },
      sizes = lanes.map { IntSize(NODE_WIDTH, NODE_HEIGHT) },
      viewportWidth = VIEWPORT_WIDTH
    )
  }
}

private const val VIEWPORT_WIDTH = 1000
private const val NODE_WIDTH = 120
private const val NODE_HEIGHT = 28
private const val LANE_Y = 200f
