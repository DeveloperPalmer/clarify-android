package ru.sla.atlas.layout

import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.atlas.entity.CanvasMargins
import ru.sla.atlas.entity.Placement

/**
 * Раскладка вынесена в чистую функцию потому, что все регрессии этой фичи жили именно здесь:
 * содержимое центрировалось мимо камеры, дорожки группировались по координате, зазор применялся к
 * центрам и узлы наезжали друг на друга. Каждая из них ниже — утверждение, а не наблюдение на
 * скриншоте.
 */
class PlacementTest {

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
        "узлы обязаны стоять рядом, а не друг на друге"
      )
    }
  }

  @Test
  fun `bounds wrap every plate`() {
    val placement = placementOf(lanes = listOf(0, 0), gaps = listOf(0f, 40f))

    assertEquals(placement.nodes[0].x.toFloat(), placement.bounds.left)
    assertEquals((placement.nodes[1].x + NODE_WIDTH).toFloat(), placement.bounds.right)
  }

  @Test
  fun `an empty graph places nothing`() {
    val placement = placementOf(
      lanes = emptyList(),
      gaps = emptyList(),
      laneYs = emptyList(),
      sizes = emptyList(),
      margins = CanvasMargins.Zero
    )

    assertEquals(Placement.Empty, placement)
  }

  /**
   * Модель и результат измерения — разные источники, и разъехаться они могут только по ошибке
   * вызывающего. Отказ обязан называть эту ошибку, а не проявляться индексом за границей списка
   * где-то в середине арифметики.
   */
  @Test
  fun `a model out of step with the measured sizes is refused, not indexed past the end`() {
    val grown = assertThrows(IllegalStateException::class.java) {
      placementOf(
        lanes = listOf(0, 0, 0),
        gaps = listOf(0f, 40f, 40f),
        laneYs = listOf(LANE_Y, LANE_Y, LANE_Y),
        sizes = listOf(IntSize(NODE_WIDTH, NODE_HEIGHT)),
        margins = CanvasMargins.Zero
      )
    }
    assertTrue(
      grown.message.orEmpty().contains("lanes=3"),
      "сообщение обязано называть длины, иначе оно не помогает"
    )

    assertThrows(IllegalStateException::class.java) {
      placementOf(
        lanes = listOf(0),
        gaps = listOf(0f),
        laneYs = listOf(LANE_Y),
        sizes = List(3) { IntSize(NODE_WIDTH, NODE_HEIGHT) },
        margins = CanvasMargins.Zero
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
      placementOf(
        lanes = emptyList(),
        gaps = emptyList(),
        laneYs = emptyList(),
        sizes = listOf(IntSize(NODE_WIDTH, NODE_HEIGHT)),
        margins = CanvasMargins.Zero
      )
    }
  }

  @Test
  fun `canvas margins keep the content off the screen edge`() {
    val bare = placementOf(lanes = listOf(0, 0), gaps = listOf(0f, 40f))

    val padded = placementOf(
      lanes = listOf(0, 0),
      gaps = listOf(0f, 40f),
      laneYs = listOf(LANE_Y, LANE_Y),
      sizes = listOf(IntSize(NODE_WIDTH, NODE_HEIGHT), IntSize(NODE_WIDTH, NODE_HEIGHT)),
      margins = CanvasMargins(left = 32f, top = 32f, right = 32f, bottom = 32f)
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
  fun `canvas margins differ per side`() {
    val bare = placementOf(lanes = listOf(0, 0), gaps = listOf(0f, 40f))

    val padded = placementOf(
      lanes = listOf(0, 0),
      gaps = listOf(0f, 40f),
      laneYs = listOf(LANE_Y, LANE_Y),
      sizes = listOf(IntSize(NODE_WIDTH, NODE_HEIGHT), IntSize(NODE_WIDTH, NODE_HEIGHT)),
      margins = CanvasMargins(left = 32f, top = 92f, right = 32f, bottom = 72f)
    )

    assertEquals(bare.bounds.left - 32f, padded.bounds.left, "по бокам только базовое поле")
    assertEquals(bare.bounds.right + 32f, padded.bounds.right)
    assertEquals(bare.bounds.top - 92f, padded.bounds.top, "сверху поле больше на то, что заняли сверху")
    assertEquals(
      bare.bounds.bottom + 72f,
      padded.bounds.bottom,
      "снизу поле больше на ту же величину: разные стороны раздувают границы независимо"
    )
  }

  @Test
  fun `plate centres follow the model order`() {
    val placement = placementOf(lanes = listOf(0, 1), gaps = listOf(12f, 40f))

    assertEquals(12f + NODE_WIDTH / 2f, placement.centres[0].x, "порядок центров — порядок модели")
    assertEquals(LANE_Y, placement.centres[0].y, "центр узла по вертикали — это его дорожка")
    assertEquals(2 * LANE_Y, placement.centres[1].y)
  }

  /**
   * Размер узла нужен не только границам полотна: из него строится прямоугольник узла на экране,
   * из которого растёт превью-карточка. Выводить его обратным счётом из центра нельзя — центр уже
   * округлён.
   */
  @Test
  fun `measured sizes come back in the model order`() {
    val placement = placementOf(
      lanes = listOf(0, 0),
      gaps = listOf(0f, 40f),
      laneYs = listOf(LANE_Y, LANE_Y),
      sizes = listOf(IntSize(NODE_WIDTH, NODE_HEIGHT), IntSize(24, 24)),
      margins = CanvasMargins.Zero
    )

    assertEquals(IntSize(NODE_WIDTH, NODE_HEIGHT), placement.sizes[0])
    assertEquals(IntSize(24, 24), placement.sizes[1], "узлы меряются каждый сам по себе и бывают разного размера")
  }

  private fun placementOf(lanes: List<Int>, gaps: List<Float>): Placement {
    return placementOf(
      lanes = lanes,
      gaps = gaps,
      laneYs = lanes.map { LANE_Y + it * LANE_Y },
      sizes = lanes.map { IntSize(NODE_WIDTH, NODE_HEIGHT) },
      margins = CanvasMargins.Zero
    )
  }
}

private const val NODE_WIDTH = 120
private const val NODE_HEIGHT = 28
private const val LANE_Y = 200f
