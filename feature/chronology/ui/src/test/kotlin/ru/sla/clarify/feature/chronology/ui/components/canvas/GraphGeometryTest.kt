package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.clarify.feature.chronology.ui.entity.GraphCameraRange
import ru.sla.clarify.feature.chronology.ui.entity.GraphPlacement

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
      centreSpanX = 60f..900f
    )
  }

  private fun Float.asCamera(): Offset {
    return Offset(x = this, y = 0f)
  }
}
