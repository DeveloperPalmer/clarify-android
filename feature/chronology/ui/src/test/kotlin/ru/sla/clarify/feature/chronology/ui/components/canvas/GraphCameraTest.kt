package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.atlas.entity.Placement
import ru.sla.clarify.feature.chronology.ui.entity.GraphCameraRange
import kotlin.math.abs

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
  fun `content that fits roams inside the viewport instead of being pinned`() {
    val range = panRangeOf(min = 0f, max = 200f, viewport = 400f)

    assertEquals(0f, range.start, "начало содержимого доводится до начала экрана, но не дальше")
    assertEquals(
      200f,
      range.endInclusive,
      "и конец до конца экрана: защёлкнутая в одну точку камера не может держать точку под пальцами"
    )
  }

  @Test
  fun `timeline rests with the leftmost plate centred`() {
    val range = centrePanRangeOf(centreSpan = 60f..900f, viewport = 1000f)

    assertEquals(
      500f - 60f,
      range.endInclusive,
      "в покое камера наводится на самый левый узел"
    )
  }

  @Test
  fun `timeline ends with the rightmost plate centred`() {
    val range = centrePanRangeOf(centreSpan = 60f..900f, viewport = 1000f)

    assertEquals(
      500f - 900f,
      range.start,
      "докрутив вправо до упора, пользователь видит последний узел в центре"
    )
  }

  @Test
  fun `a single plate is centred and cannot be panned away`() {
    val range = centrePanRangeOf(centreSpan = 60f..60f, viewport = 1000f)

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
    val range = cameraRangeOf(Placement.Empty, IntSize(width = 1000, height = 600), scale = 1f)

    assertEquals(GraphCameraRange.Empty, range)
  }

  @Test
  fun `a lone lane keeps its vertical play inside the viewport`() {
    val range = cameraRangeOf(placement(), IntSize(width = 1000, height = 600), scale = 1f)

    assertEquals(-400f, range.x.start)
    assertEquals(440f, range.x.endInclusive)
    // Одна дорожка помещается по высоте целиком — это норма экрана, а не краевой случай. Но ход по
    // вертикали ей всё равно нужен: без него пинч над такой дорожкой не удержит точку под пальцами.
    assertEquals(0f, range.y.start, "верх содержимого доводится до верха экрана")
    assertEquals(500f, range.y.endInclusive, "низ — до низа, и ни пикселем дальше")
  }

  @Test
  fun `the camera rests with the first plate centred`() {
    val viewport = IntSize(width = 1000, height = 600)
    // Содержимое выше экрана, а первая плашка — в его середине: центр экрана ей достижим.
    val tall = placement().copy(
      bounds = Rect(left = 0f, top = 0f, right = 960f, bottom = 2000f),
      centres = listOf(Offset(x = 60f, y = 800f))
    )

    val rest = cameraRestOf(tall, viewport, cameraRangeOf(tall, viewport, scale = 1f), scale = 1f)

    assertEquals(500f - 60f, rest.x, "начало истории обязано оказаться под глазами, а не в углу")
    assertEquals(300f - 800f, rest.y)
  }

  @Test
  fun `a first plate at the very top is still brought to the centre`() {
    val viewport = IntSize(width = 1000, height = 600)
    // Первая плашка у самого верха содержимого: над ней только поле полотна.
    val tall = placement().copy(
      bounds = Rect(left = 0f, top = 0f, right = 960f, bottom = 2000f),
      centreSpanY = 40f..40f,
      centres = listOf(Offset(x = 60f, y = 40f))
    )

    val rest = cameraRestOf(tall, viewport, cameraRangeOf(tall, viewport, scale = 1f), scale = 1f)

    assertEquals(
      300f - 40f,
      rest.y,
      "экран открывают, чтобы увидеть начало истории: оно наводится в центр по обеим осям, а не " +
        "упирается в кромку содержимого"
    )
  }

  @Test
  fun `an empty graph rests at zero`() {
    val viewport = IntSize(width = 1000, height = 600)

    val rest = cameraRestOf(Placement.Empty, viewport, GraphCameraRange.Empty, scale = 1f)

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

  @Test
  fun `the camera range grows in proportion to the scale`() {
    val viewport = IntSize(width = 1000, height = 600)

    val single = cameraRangeOf(placement(), viewport, scale = 1f)
    val doubled = cameraRangeOf(placement(), viewport, scale = 2f)

    assertEquals(
      (single.x.endInclusive - single.x.start) * 2f,
      doubled.x.endInclusive - doubled.x.start,
      "вдвое крупнее содержимое — вдвое длиннее ход камеры"
    )
    assertEquals(500f - 120f, doubled.x.endInclusive, "покой наводится на удвоенный центр плашки")
  }

  @Test
  fun `zooming in opens the margin that the plate centres alone would hide`() {
    val viewport = IntSize(width = 1000, height = 600)
    // Одна плашка 400 шириной с полями по 100: при единичном масштабе половина плашки уже половины
    // экрана, при 2.5× — шире её.
    val single = placement().copy(
      bounds = Rect(left = 0f, top = 0f, right = 600f, bottom = 100f),
      centreSpanX = 300f..300f,
      centres = listOf(Offset(x = 300f, y = 50f))
    )

    val natural = cameraRangeOf(single, viewport, scale = 1f)
    val zoomed = cameraRangeOf(single, viewport, scale = 2.5f)

    assertEquals(
      0f..400f,
      natural.x,
      "пока содержимое помещается, ему разрешено гулять по экрану, но не выходить за него"
    )
    assertEquals(
      -500f,
      zoomed.x.start,
      "увеличенное поле справа обязано стать достижимым, иначе пинч у края упирается в кламп"
    )
    assertEquals(0f, zoomed.x.endInclusive, "и поле слева тоже")
  }

  @Test
  fun `a pinch at the start of the history drifts by less than half a plate`() {
    val viewport = IntSize(width = 1133, height = 2400)
    // Раскладка масштаба демо-набора: 22 плашки по 200 dp при плотности 2.75.
    val demo = placement().copy(
      bounds = Rect(left = 88f, top = -200f, right = 17930f, bottom = 4600f),
      centreSpanX = 539f..17479f,
      centres = listOf(Offset(x = 539f, y = 200f))
    )
    // Камера в покое: первая плашка в центре экрана. Пальцы левее её, в поле полотна, — сценарий,
    // в котором кламп и отбирал у пинча его точку.
    var camera = cameraRestOf(demo, viewport, cameraRangeOf(demo, viewport, scale = 1f), scale = 1f)
    val focus = Offset(x = 300f, y = 1200f)
    val point = focus - camera
    var scale = 1f

    // Двадцать шагов по 1.05 — столько приходит от жеста за полсекунды разведения пальцев.
    repeat(20) {
      val updated = scaleStepOf(scale, 1.05f, 0.4f..2.5f)
      val zoomed = zoomedCameraOf(camera = camera, focus = focus, from = scale, to = updated)
      camera = cameraRangeOf(demo, viewport, updated).clamp(zoomed)
      scale = updated
    }

    // Ноль здесь недостижим: у самого начала истории камере разрешено ровно то, что разрешают обе
    // политики, и на масштабах около единицы поле ещё уже половины экрана. Измеренная цена —
    // 126 px против 399 px, когда диапазон считался только по центрам плашек.
    assertTrue(
      abs(point.x * scale + camera.x - focus.x) < 200f,
      "пинч у начала истории обязан уводить точку меньше чем на половину плашки"
    )
  }

  @Test
  fun `a pinch below the centre holds its point while the graph fits the viewport`() {
    val viewport = IntSize(width = 1000, height = 600)
    // Граф целиком помещается по высоте — то состояние, в котором зум и уводил плашки из-под
    // пальцев: камере по вертикали было некуда двигаться.
    val short = placement().copy(
      bounds = Rect(left = 0f, top = 0f, right = 960f, bottom = 400f),
      centreSpanY = 350f..350f,
      centres = listOf(Offset(x = 60f, y = 350f))
    )
    var camera = Offset(x = 0f, y = 100f)
    // Пальцы разведены низко, под нижней плашкой.
    val focus = Offset(x = 500f, y = 480f)
    val point = focus - camera
    var scale = 1f

    repeat(10) {
      val updated = scaleStepOf(scale, 1.05f, 0.4f..2.5f)
      camera = cameraRangeOf(short, viewport, updated)
        .clamp(zoomedCameraOf(camera = camera, focus = focus, from = scale, to = updated))
      scale = updated
    }

    assertEquals(
      focus.y,
      point.y * scale + camera.y,
      0.5f,
      "плашка обязана расти под пальцами, а не уползать вниз от принудительного центрирования"
    )
  }

  @Test
  fun `the vertical range collapses without a jump at the fit threshold`() {
    val viewport = IntSize(width = 1000, height = 600)
    val tall = placement().copy(bounds = Rect(left = 0f, top = 0f, right = 960f, bottom = 1000f))

    // Порог: содержимое ровно во весь экран. Чуть выше него диапазон ещё живой, чуть ниже —
    // вырожденный, и кламп обязан вести камеру через порог непрерывно.
    val above = cameraRangeOf(tall, viewport, scale = 0.601f)
    val below = cameraRangeOf(tall, viewport, scale = 0.599f)

    assertTrue(
      abs(above.y.start - below.y.start) < 5f,
      "зум-аут через порог вырождения не должен дёргать камеру: у границы диапазон уже пуст"
    )
  }

  @Test
  fun `the camera rests on the first plate at any scale`() {
    val viewport = IntSize(width = 1000, height = 600)
    val tall = placement().copy(
      bounds = Rect(left = 0f, top = 0f, right = 960f, bottom = 2000f),
      centres = listOf(Offset(x = 60f, y = 800f))
    )

    val rest = cameraRestOf(tall, viewport, cameraRangeOf(tall, viewport, scale = 0.5f), scale = 0.5f)

    assertEquals(500f - 30f, rest.x, "центр плашки на полотне тоже уменьшился вдвое")
    assertEquals(300f - 400f, rest.y)
  }

  @Test
  fun `zoom keeps the pinched point under the fingers`() {
    val focus = Offset(x = 400f, y = 250f)
    val camera = Offset(x = -100f, y = -50f)
    // Точка полотна, оказавшаяся под пальцами: (focus - camera) / from.
    val point = Offset(x = 500f, y = 300f)

    val zoomed = zoomedCameraOf(camera = camera, focus = focus, from = 1f, to = 2f)

    assertEquals(focus.x, point.x * 2f + zoomed.x, "точка под пальцами обязана остаться под ними")
    assertEquals(focus.y, point.y * 2f + zoomed.y)
  }

  @Test
  fun `zoom out around a focus keeps it in place too`() {
    val focus = Offset(x = 400f, y = 250f)
    val camera = Offset(x = -100f, y = -50f)
    val point = Offset(x = 500f, y = 300f)

    val zoomed = zoomedCameraOf(camera = camera, focus = focus, from = 1f, to = 0.5f)

    assertEquals(focus.x, point.x * 0.5f + zoomed.x)
    assertEquals(focus.y, point.y * 0.5f + zoomed.y)
  }

  @Test
  fun `scale is truncated at the boundary instead of being dropped`() {
    val range = 0.4f..2.5f

    assertEquals(2.5f, scaleStepOf(scale = 2f, change = 4f, range = range))
    assertEquals(0.4f, scaleStepOf(scale = 0.5f, change = 0.1f, range = range))
    assertEquals(1.5f, scaleStepOf(scale = 1f, change = 1.5f, range = range))
  }

  @Test
  fun `a degenerate pinch cannot poison the scale`() {
    val scale = scaleStepOf(scale = 1.5f, change = Float.NaN, range = 0.4f..2.5f)

    assertEquals(
      1.5f,
      scale,
      "NaN проходит coerceIn насквозь, и масштаб, ставший NaN, уносит с собой всю камеру"
    )
  }

  private fun placement(): Placement {
    return Placement(
      nodes = listOf(IntOffset.Zero),
      sizes = listOf(IntSize(width = 120, height = 100)),
      bounds = Rect(left = 0f, top = 0f, right = 960f, bottom = 100f),
      centreSpanX = 60f..900f,
      centreSpanY = 50f..50f,
      centres = listOf(Offset(x = 60f, y = 50f))
    )
  }

  private fun Float.asCamera(): Offset {
    return Offset(x = this, y = 0f)
  }
}
