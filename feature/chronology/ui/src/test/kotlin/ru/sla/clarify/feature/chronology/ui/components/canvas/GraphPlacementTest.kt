package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
  fun `the canvas starts at the first gap, not at a centring shift`() {
    val placement = placementOf(lanes = listOf(0, 0), gaps = listOf(12f, 50f))

    assertEquals(
      12,
      placement.nodes[0].x,
      "центрирование первого узла \u2014 дело камеры, а не раскладки"
    )
  }

  @Test
  fun `the centre span covers the outermost plates`() {
    val placement = placementOf(lanes = listOf(0, 0), gaps = listOf(0f, 40f))

    assertEquals(NODE_WIDTH / 2f, placement.centreSpanX.start)
    assertEquals(NODE_WIDTH + 40f + NODE_WIDTH / 2f, placement.centreSpanX.endInclusive)
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
      edgePadding = 0f
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
      edgePadding = 0f
    )

    assertEquals(GraphPlacement.Empty, placement)
  }

  /**
   * Модель и результат измерения — разные источники, и разъехаться они могут только по ошибке
   * вызывающего. Отказ обязан называть эту ошибку, а не проявляться индексом за границей списка
   * где-то в середине арифметики.
   */
  @Test
  fun `a model out of step with the measured sizes is refused, not indexed past the end`() {
    val grown = assertThrows(IllegalStateException::class.java) {
      graphPlacementOf(
        lanes = listOf(0, 0, 0),
        gaps = listOf(0f, 40f, 40f),
        laneYs = listOf(LANE_Y, LANE_Y, LANE_Y),
        sizes = listOf(IntSize(NODE_WIDTH, NODE_HEIGHT)),
        edgePadding = 0f
      )
    }
    assertTrue(
      grown.message.orEmpty().contains("lanes=3"),
      "сообщение обязано называть длины, иначе оно не помогает"
    )

    assertThrows(IllegalStateException::class.java) {
      graphPlacementOf(
        lanes = listOf(0),
        gaps = listOf(0f),
        laneYs = listOf(LANE_Y),
        sizes = List(3) { IntSize(NODE_WIDTH, NODE_HEIGHT) },
        edgePadding = 0f
      )
    }
  }

  /**
   * Пустая модель при непустом измерении — не «пустой граф», а тот же рассинхрон. Ранний выход по
   * пустым размерам не должен превращать его в молчаливо пустую раскладку.
   */
  @Test
  fun `an empty model with measured nodes is refused too`() {
    assertThrows(IllegalStateException::class.java) {
      graphPlacementOf(
        lanes = emptyList(),
        gaps = emptyList(),
        laneYs = emptyList(),
        sizes = listOf(IntSize(NODE_WIDTH, NODE_HEIGHT)),
        edgePadding = 0f
      )
    }
  }

  @Test
  fun `canvas margins keep the content off the screen edge`() {
    val bare = placementOf(lanes = listOf(0, 0), gaps = listOf(0f, 40f))

    val padded = graphPlacementOf(
      lanes = listOf(0, 0),
      gaps = listOf(0f, 40f),
      laneYs = listOf(LANE_Y, LANE_Y),
      sizes = listOf(IntSize(NODE_WIDTH, NODE_HEIGHT), IntSize(NODE_WIDTH, NODE_HEIGHT)),
      edgePadding = 32f
    )

    assertEquals(bare.bounds.left - 32f, padded.bounds.left, "поля входят в протяжённость полотна")
    assertEquals(bare.bounds.bottom + 32f, padded.bounds.bottom)
    assertEquals(
      bare.centreSpanX.start,
      padded.centreSpanX.start,
      "на наведение камеры поля не влияют: она ходит по центрам плашек"
    )
  }

  @Test
  fun `plate centres follow the model order`() {
    val placement = placementOf(lanes = listOf(0, 1), gaps = listOf(12f, 40f))

    assertEquals(12f + NODE_WIDTH / 2f, placement.centres[0].x, "порядок центров — порядок модели")
    assertEquals(LANE_Y, placement.centres[0].y, "центр плашки по вертикали — это её дорожка")
    assertEquals(2 * LANE_Y, placement.centres[1].y)
  }

  private fun placementOf(lanes: List<Int>, gaps: List<Float>): GraphPlacement {
    return graphPlacementOf(
      lanes = lanes,
      gaps = gaps,
      laneYs = lanes.map { LANE_Y + it * LANE_Y },
      sizes = lanes.map { IntSize(NODE_WIDTH, NODE_HEIGHT) },
      edgePadding = 0f
    )
  }
}

private const val NODE_WIDTH = 120
private const val NODE_HEIGHT = 28
private const val LANE_Y = 200f
