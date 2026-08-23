package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.clarify.feature.chronology.ui.entity.GraphCameraRange
import ru.sla.clarify.feature.chronology.ui.entity.GraphPlacement

/**
 * Камера вынесена в чистые функции ровно затем, чтобы её можно было проверить без Compose и без
 * устройства: диапазон, покой, шаг и признак упора — обычная арифметика.
 *
 * Сторожит дефекты, которые уже случались: кламп на чтении вместо записи копил мёртвую зону, покой
 * прижимал содержимое к краю вместо начала истории, а вырожденная ось — норма этого экрана, а не
 * краевой случай.
 */
class GraphCameraTest {

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

  @Test
  fun `an empty graph has nowhere to pan`() {
    val range = cameraRangeOf(GraphPlacement.Empty, IntSize(width = 1000, height = 600))

    assertEquals(GraphCameraRange.Empty, range)
  }

  @Test
  fun `a lone lane leaves the vertical axis degenerate`() {
    val range = cameraRangeOf(placement(), IntSize(width = 1000, height = 600))

    assertEquals(-400f, range.x.start)
    assertEquals(440f, range.x.endInclusive)
    assertEquals(
      range.y.start,
      range.y.endInclusive,
      "одна дорожка помещается по высоте целиком, и это норма экрана, а не краевой случай"
    )
  }

  @Test
  fun `the camera rests with the first plate centred`() {
    val viewport = IntSize(width = 1000, height = 600)
    // Содержимое выше экрана, а первая плашка — в его середине: центр экрана ей достижим.
    val tall = placement().copy(
      bounds = Rect(left = 0f, top = 0f, right = 960f, bottom = 2000f),
      centres = listOf(Offset(x = 60f, y = 800f))
    )

    val rest = cameraRestOf(tall, viewport, cameraRangeOf(tall, viewport))

    assertEquals(500f - 60f, rest.x, "начало истории обязано оказаться под глазами, а не в углу")
    assertEquals(300f - 800f, rest.y)
  }

  @Test
  fun `a first plate that cannot reach the centre stops at the boundary`() {
    val viewport = IntSize(width = 1000, height = 600)
    // Первая плашка у самого верха: центрировать её значило бы оторвать содержимое от края.
    val tall = placement().copy(
      bounds = Rect(left = 0f, top = 0f, right = 960f, bottom = 2000f),
      centres = listOf(Offset(x = 60f, y = 40f))
    )

    val rest = cameraRestOf(tall, viewport, cameraRangeOf(tall, viewport))

    assertEquals(0f, rest.y, "наведение зажимается диапазоном, а не выносит содержимое за границу")
  }

  @Test
  fun `an empty graph rests at zero`() {
    val viewport = IntSize(width = 1000, height = 600)

    val rest = cameraRestOf(GraphPlacement.Empty, viewport, GraphCameraRange.Empty)

    assertEquals(Offset.Zero, rest)
  }

  @Test
  fun `consumed delta equals the request while the camera has room`() {
    val step = panStepOf(
      camera = Offset(x = 0f, y = 0f),
      delta = Offset(x = 100f, y = 0f),
      range = GraphCameraRange(x = -400f..440f, y = 0f..0f)
    )

    assertEquals(Offset(x = 100f, y = 0f), step.camera)
    assertEquals(Offset(x = 100f, y = 0f), step.consumed)
  }

  @Test
  fun `consumed delta is truncated at the boundary`() {
    val step = panStepOf(
      camera = Offset(x = -350f, y = 0f),
      delta = Offset(x = -200f, y = 0f),
      range = GraphCameraRange(x = -400f..440f, y = 0f..0f)
    )

    assertEquals(-400f, step.camera.x, "камера обязана встать ровно на границе")
    assertEquals(-50f, step.consumed.x, "потреблено ровно то, что уместилось")
  }

  @Test
  fun `consumed delta is zero on a saturated axis`() {
    val step = panStepOf(
      camera = Offset(x = -400f, y = 0f),
      delta = Offset(x = -120f, y = 0f),
      range = GraphCameraRange(x = -400f..440f, y = 0f..0f)
    )

    assertEquals(Offset.Zero, step.consumed, "нулевое потребление — это и есть сигнал упора")
  }

  @Test
  fun `a degenerate range consumes nothing`() {
    val step = panStepOf(
      camera = Offset(x = 440f, y = 0f),
      delta = Offset(x = 60f, y = 90f),
      range = GraphCameraRange(x = 440f..440f, y = 0f..0f)
    )

    assertEquals(Offset(x = 440f, y = 0f), step.camera)
    assertEquals(Offset.Zero, step.consumed)
  }

  @Test
  fun `a step that moved nothing reports itself rejected`() {
    val range = GraphCameraRange(x = -400f..440f, y = 0f..0f)

    val intoTheWall = panStepOf(camera = Offset(x = -400f, y = 0f), delta = Offset(x = -120f, y = 0f), range)
    val zeroFrame = panStepOf(camera = Offset(x = -400f, y = 0f), delta = Offset.Zero, range)
    val moved = panStepOf(camera = Offset(x = 0f, y = 0f), delta = Offset(x = -120f, y = 0f), range)

    assertTrue(intoTheWall.isRejected, "нулевое потребление при непустом запросе — это упор")
    assertTrue(!zeroFrame.isRejected, "первый кадр затухания не двигает ничего по построению")
    assertTrue(!moved.isRejected)
  }

  @Test
  fun `a rejected delta cannot be banked for later`() {
    val range = GraphCameraRange(x = -400f..440f, y = 0f..0f)
    val intoTheWall = panStepOf(camera = 440f.asCamera(), delta = Offset(x = -3840f, y = 0f), range)

    val back = panStepOf(camera = intoTheWall.camera, delta = Offset(x = 100f, y = 0f), range)

    assertEquals(
      -300f,
      back.camera.x,
      "жест обратно двигает картинку сразу: отвергнутое за границей нигде не копится"
    )
  }

  @Test
  fun `a fling survives a wall while the other axis has room`() {
    val stuck = isCameraStuck(
      camera = Offset(x = -400f, y = 0f),
      direction = Offset(x = -0.8f, y = 0.6f),
      range = GraphCameraRange(x = -400f..440f, y = -200f..200f)
    )

    assertTrue(!stuck, "пока бросок несёт хоть одна ось, инерция едет вдоль стенки")
  }

  @Test
  fun `a fling dies when the only live axis hits its wall`() {
    val stuck = isCameraStuck(
      camera = Offset(x = -400f, y = 0f),
      direction = Offset(x = -0.8f, y = 0.6f),
      // Одна дорожка: вертикаль вырождена и нести бросок ей нечем.
      range = GraphCameraRange(x = -400f..440f, y = 0f..0f)
    )

    assertTrue(stuck)
  }

  @Test
  fun `a fling along an axis ignores the range of the other one`() {
    val stuck = isCameraStuck(
      camera = Offset(x = -400f, y = 0f),
      direction = Offset(x = -1f, y = 0f),
      range = GraphCameraRange(x = -400f..440f, y = -200f..200f)
    )

    assertTrue(stuck, "запас по Y не оживляет бросок, у которого по Y нет скорости")
  }

  @Test
  fun `a fling is not stuck before it has moved`() {
    val stuck = isCameraStuck(
      camera = Offset(x = 0f, y = 0f),
      direction = Offset(x = -0.8f, y = 0.6f),
      range = GraphCameraRange(x = -400f..440f, y = -200f..200f)
    )

    assertTrue(!stuck, "нулевой кадр в начале затухания не должен читаться как упор")
  }

  private fun placement(): GraphPlacement {
    return GraphPlacement(
      nodes = listOf(IntOffset.Zero),
      bounds = Rect(left = 0f, top = 0f, right = 960f, bottom = 100f),
      edges = emptyList(),
      centreSpanX = 60f..900f,
      centres = listOf(Offset(x = 60f, y = 50f))
    )
  }

  private fun Float.asCamera(): Offset {
    return Offset(x = this, y = 0f)
  }
}
