package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranch
import ru.sla.clarify.feature.chronology.ui.entity.GraphCameraRange
import ru.sla.clarify.feature.chronology.ui.entity.GraphLaneMark
import ru.sla.clarify.feature.chronology.ui.entity.GraphPlacement
import ru.sla.clarify.feature.chronology.ui.entity.GraphViewportSpan

/**
 * Перевод между камерой полотна и полосой мини-карты вынесен в чистые функции затем, чтобы
 * проверять его без Compose и без устройства: положение, ширина рамки и позиции засечек — это
 * обычная арифметика.
 *
 * Сторожит дефект, найденный владельцем на устройстве: у каждого края полосы набегало от 16 до
 * 29.5 dp, где палец ещё едет, а граф уже стоит. Причин было две — мир полосы включал поля полотна,
 * которых камере не достичь, и рамку, расширенную до размера пальца, приходилось зажимать краем.
 * Обе снимает одно правило: положение — доля **хода**, а ход рамки равен полосе за вычетом её
 * собственной ширины.
 *
 * И три вырожденных случая, каждый из которых на экране бывает: пустая история, история короче
 * экрана и вьюпорт нулевой ширины до первого измерения.
 */
class GraphMinimapGeometryTest {

  @Test
  fun `the frame spans the whole track when the history fits the screen`() {
    val span = viewportSpanOf(
      camera = Offset.Zero,
      scale = 1f,
      centreSpan = 100f..500f,
      viewport = VIEWPORT
    )

    assertEquals(1f, span.width, "видно всё — значит рамка занимает полосу целиком")
  }

  @Test
  fun `the frame narrows in proportion to the zoom`() {
    val natural = viewportSpanOf(
      camera = Offset(x = -4500f, y = 0f),
      scale = 1f,
      centreSpan = LONG_HISTORY,
      viewport = VIEWPORT
    )
    val zoomed = viewportSpanOf(
      camera = Offset(x = -9500f, y = 0f),
      scale = 2f,
      centreSpan = LONG_HISTORY,
      viewport = VIEWPORT
    )

    assertEquals(0.1f, natural.width, 1e-4f)
    assertEquals(0.05f, zoomed.width, 1e-4f, "вдвое крупнее содержимое — вдвое уже видимое")
    assertEquals(0.5f, natural.position, 1e-4f, "обе камеры смотрят в одно место истории")
    assertEquals(0.5f, zoomed.position, 1e-4f)
  }

  @Test
  fun `an empty history keeps the frame still instead of dividing by zero`() {
    val span = viewportSpanOf(
      camera = Offset.Zero,
      scale = 1f,
      centreSpan = 0f..0f,
      viewport = VIEWPORT
    )

    assertEquals(GraphViewportSpan.Full, span, "делить на длину пустой истории нечем")
  }

  @Test
  fun `a viewport of no width keeps the frame still too`() {
    // Состояние до первого измерения: узлы уже есть, вьюпорт ещё нулевой.
    val span = viewportSpanOf(
      camera = Offset.Zero,
      scale = 1f,
      centreSpan = LONG_HISTORY,
      viewport = IntSize.Zero
    )

    assertEquals(GraphViewportSpan.Full, span)
  }

  @Test
  fun `a frame thinner than a fingertip is widened without moving the camera`() {
    val thin = GraphViewportSpan(position = 0.5f, width = 0.02f)

    val widened = widenedSpanOf(span = thin, minWidth = 0.1f)

    assertEquals(0.1f, widened.width, 1e-4f)
    assertEquals(
      thin.position,
      widened.position,
      "расширяется хват, а не то, где стоит камера: ход укорачивается ровно на добавленную ширину"
    )
  }

  @Test
  fun `a frame wider than the minimum is left alone`() {
    val span = GraphViewportSpan(position = 0.2f, width = 0.3f)

    assertEquals(span, widenedSpanOf(span = span, minWidth = 0.1f))
  }

  @Test
  fun `the frame touches both ends of the track whatever its width`() {
    // Ход рамки — полоса за вычетом её ширины, поэтому края достижимы и у широкой, и у узкой.
    listOf(0.02f, 0.086f, 0.16f, 0.5f).forEach { width ->
      assertEquals(
        width / 2f,
        trackCentreOf(position = 0f, width = width),
        1e-5f,
        "в начале истории рамка обязана стоять левым краем на краю полосы"
      )
      assertEquals(
        1f - width / 2f,
        trackCentreOf(position = 1f, width = width),
        1e-5f,
        "а в конце — правым краем на правом краю"
      )
    }
  }

  @Test
  fun `the frame moves further per pixel when the history is longer`() {
    // Одна и та же доля хода на разных ширинах рамки: чем уже рамка, тем длиннее её ход.
    val narrow = trackCentreOf(position = 0.25f, width = 0.05f)
    val wide = trackCentreOf(position = 0.25f, width = 0.4f)

    assertTrue(narrow < wide, "у широкой рамки ход короче, и та же доля приходится правее")
    assertEquals(0.2625f, narrow, 1e-4f)
    assertEquals(0.35f, wide, 1e-4f)
  }

  @Test
  fun `a finger at either end of the track asks for the ends of the travel`() {
    val frame = 0.086f

    assertEquals(
      0f,
      scrubbedPositionOf(x = 8f, width = 388f, padding = 8f, frameWidth = frame),
      "палец у левого края полосы просит начало истории"
    )
    assertEquals(
      1f,
      scrubbedPositionOf(x = 380f, width = 388f, padding = 8f, frameWidth = frame),
      "а у правого — конец"
    )
  }

  @Test
  fun `a finger lands the frame centre under itself`() {
    val frame = 0.2f
    val position = scrubbedPositionOf(x = 200f, width = 400f, padding = 0f, frameWidth = frame)

    assertEquals(
      0.5f,
      trackCentreOf(position = position, width = frame),
      1e-4f,
      "центр рамки обязан оказаться там, где палец, — иначе рамка уезжает из-под него"
    )
  }

  @Test
  fun `a touch inside the padding still lands on the track`() {
    assertEquals(0f, scrubbedPositionOf(x = 0f, width = 400f, padding = 20f, frameWidth = 0.1f))
    assertEquals(1f, scrubbedPositionOf(x = 400f, width = 400f, padding = 20f, frameWidth = 0.1f))
  }

  @Test
  fun `a track squeezed to nothing cannot be scrubbed`() {
    assertEquals(
      0f,
      scrubbedPositionOf(x = 10f, width = 30f, padding = 20f, frameWidth = 0.1f),
      "полоса, ужатая до собственных полей, не должна делить на отрицательную ширину"
    )
  }

  @Test
  fun `a frame filling the track cannot be scrubbed either`() {
    assertEquals(
      0f,
      scrubbedPositionOf(x = 200f, width = 400f, padding = 0f, frameWidth = 1f),
      "когда видно всю историю, ходу рамки взяться неоткуда"
    )
  }

  @Test
  fun `scrubbing to the middle brings the middle of the history to the centre of the screen`() {
    val placement = longPlacement()

    val camera = scrubbedCameraXOf(
      position = 0.5f,
      scale = 1f,
      centreSpan = placement.centreSpanX,
      viewport = VIEWPORT,
      range = cameraRangeOf(placement, VIEWPORT, scale = 1f)
    )

    assertEquals(
      500f - 5000f,
      camera,
      "середина истории обязана встать в середину экрана, а не у его края"
    )
  }

  @Test
  fun `either end of the travel lands exactly on the camera boundary`() {
    val placement = longPlacement()
    val range = cameraRangeOf(placement, VIEWPORT, scale = 1f)

    val start = scrubbedCameraXOf(0f, 1f, placement.centreSpanX, VIEWPORT, range)
    val end = scrubbedCameraXOf(1f, 1f, placement.centreSpanX, VIEWPORT, range)

    assertEquals(
      range.x.endInclusive,
      start,
      "нулевая доля хода — это упор камеры, а не «почти упор»: иначе у края остаётся мёртвый ход"
    )
    assertEquals(range.x.start, end, "и единица — противоположный упор")
  }

  @Test
  fun `the scrubbed camera returns the same position it was asked for`() {
    val placement = longPlacement()
    val asked = 0.3f

    val camera = scrubbedCameraXOf(
      position = asked,
      scale = 1f,
      centreSpan = placement.centreSpanX,
      viewport = VIEWPORT,
      range = cameraRangeOf(placement, VIEWPORT, scale = 1f)
    )
    val span = viewportSpanOf(
      camera = Offset(x = camera, y = 0f),
      scale = 1f,
      centreSpan = placement.centreSpanX,
      viewport = VIEWPORT
    )

    assertEquals(
      asked,
      span.position,
      1e-4f,
      "прямой и обратный переводы обязаны сходиться: разъедутся они молча, потому что политику " +
        "диапазона камеры меняют в другом файле"
    )
  }

  @Test
  fun `scrubbing an empty history moves nothing`() {
    val camera = scrubbedCameraXOf(
      position = 0.5f,
      scale = 1f,
      centreSpan = 0f..0f,
      viewport = VIEWPORT,
      range = GraphCameraRange.Empty
    )

    assertEquals(0f, camera, "пустому графу двигать нечего")
  }

  @Test
  fun `a lane mark stands where the first node of its lane stands`() {
    val marks = laneMarksOf(
      branchIds = branchIds("trunk", "a", "trunk", "b"),
      lanes = listOf(0, 1, 0, -1),
      colorIndexes = listOf(0, 1, 0, 2),
      centres = listOf(
        Offset(x = 0f, y = 0f),
        Offset(x = 2000f, y = 100f),
        Offset(x = 3000f, y = 0f),
        Offset(x = 5000f, y = -100f)
      ),
      centreSpan = LONG_HISTORY
    )

    assertEquals(
      listOf(
        GraphLaneMark(position = 0.2f, lane = 1, colorIndex = 1),
        GraphLaneMark(position = 0.5f, lane = -1, colorIndex = 2)
      ),
      marks
    )
  }

  @Test
  fun `the trunk has no lane mark of its own`() {
    val marks = laneMarksOf(
      branchIds = branchIds("trunk", "trunk", "trunk"),
      lanes = listOf(0, 0, 0),
      colorIndexes = listOf(0, 0, 0),
      centres = listOf(Offset(x = 0f, y = 0f), Offset(x = 200f, y = 0f), Offset(x = 300f, y = 0f)),
      centreSpan = LONG_HISTORY
    )

    assertTrue(marks.isEmpty(), "магистраль — не ветка, засекать на полосе её начало незачем")
  }

  @Test
  fun `lane marks survive a graph whose nodes are not sorted by lane`() {
    // Узлы упорядочены временем, а не дорожками: второй узел ветки встретился раньше первого.
    val marks = laneMarksOf(
      branchIds = branchIds("a", "trunk", "a"),
      lanes = listOf(1, 0, 1),
      colorIndexes = listOf(1, 0, 1),
      centres = listOf(
        Offset(x = 5000f, y = 100f),
        Offset(x = 0f, y = 0f),
        Offset(x = 3000f, y = 100f)
      ),
      centreSpan = LONG_HISTORY
    )

    assertEquals(
      listOf(GraphLaneMark(position = 0.3f, lane = 1, colorIndex = 1)),
      marks,
      "начало ветки — самый левый её узел, а не первый в списке"
    )
  }

  @Test
  fun `lane marks are ordered along the timeline`() {
    val marks = laneMarksOf(
      branchIds = branchIds("a", "b", "c"),
      lanes = listOf(3, -1, 2),
      colorIndexes = listOf(3, 1, 2),
      centres = listOf(
        Offset(x = 8000f, y = 300f),
        Offset(x = 1000f, y = -100f),
        Offset(x = 4000f, y = 200f)
      ),
      centreSpan = LONG_HISTORY
    )

    assertEquals(
      listOf(-1, 2, 3),
      marks.map { it.lane },
      "порядок обхода хеш-таблицы не определён, и список, тасующийся при том же содержимом, " +
        "заставлял бы derivedStateOf считать себя изменившимся на каждой раскладке"
    )
  }

  @Test
  fun `a degenerate history has no lane marks`() {
    val marks = laneMarksOf(
      branchIds = branchIds("a"),
      lanes = listOf(1),
      colorIndexes = listOf(1),
      centres = listOf(Offset(x = 100f, y = 100f)),
      centreSpan = 0f..0f
    )

    assertTrue(marks.isEmpty())
  }

  @Test
  fun `a lane mark sits under the frame centre that shows its branch`() {
    val placement = longPlacement()
    val range = cameraRangeOf(placement, VIEWPORT, scale = 1f)
    // Ветка начинается ровно посередине истории.
    val mark = laneMarksOf(
      branchIds = branchIds("a"),
      lanes = listOf(1),
      colorIndexes = listOf(1),
      centres = listOf(Offset(x = 5000f, y = 100f)),
      centreSpan = placement.centreSpanX
    ).single()

    val camera = scrubbedCameraXOf(mark.position, 1f, placement.centreSpanX, VIEWPORT, range)
    val span = viewportSpanOf(Offset(x = camera, y = 0f), 1f, placement.centreSpanX, VIEWPORT)
    val frame = widenedSpanOf(span, minWidth = 0.086f)

    assertEquals(
      trackCentreOf(position = mark.position, width = frame.width),
      trackCentreOf(position = frame.position, width = frame.width),
      1e-4f,
      "рамка обязана накрывать засечку центром ровно тогда, когда ветка в центре экрана: ради " +
        "этого движения мини-карта и существует"
    )
  }

  /**
   * История заведомо шире экрана: десять тысяч пикселей между центрами крайних плашек.
   *
   * @return раскладка, у которой есть чем панорамировать
   */
  private fun longPlacement(): GraphPlacement {
    return GraphPlacement(
      nodes = listOf(IntOffset.Zero),
      bounds = Rect(left = -100f, top = 0f, right = 10_100f, bottom = 400f),
      edges = emptyList(),
      centreSpanX = LONG_HISTORY,
      centreSpanY = 200f..200f,
      centres = listOf(Offset(x = 0f, y = 200f))
    )
  }
}

private val VIEWPORT = IntSize(width = 1000, height = 600)
private val LONG_HISTORY = 0f..10_000f

/**
 * Ветка каждого узла по её имени.
 *
 * Засечки группируются по ветке, а не по дорожке: дорожка переиспользуется после слияния, и две
 * темы, вставшие на неё по очереди, дали бы одну засечку вместо двух.
 */
private fun branchIds(vararg names: String): List<GraphBranch.Id> {
  return names.map { GraphBranch.Id(it) }
}
