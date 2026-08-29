package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranch
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeRole
import ru.sla.clarify.feature.chronology.ui.entity.GraphPlacement
import ru.sla.clarify.feature.chronology.ui.entity.TimeGap

/**
 * Держатель проверяется без Compose: `mutableStateOf` и `derivedStateOf` работают и вне композиции,
 * а всё, что нужно [GraphCanvasState.layout], — плотность и размеры, то есть обычные числа.
 *
 * Тест сторожит дефект, который довёл камеру до телепорта: сдвиг накапливался без границ, а кламп
 * стоял только на чтении. Пока это было так, упор в стенку банковал мёртвую зону, жест обратно
 * сначала выбирал её вхолостую, а следующая раскладка отдавала накопленное одним прыжком.
 */
class GraphCanvasStateTest {

  @Test
  fun `a rejected delta cannot be banked for later`() {
    val state = GraphCanvasState()
    state.place(count = 3)

    state.pan(Offset(x = -4000f, y = 0f))
    val atTheWall = state.offset.value.x
    val consumed = state.pan(Offset(x = 100f, y = 0f))

    assertEquals(atTheWall + 100f, state.offset.value.x, "картинка обязана пойти за пальцем сразу")
    assertEquals(100f, consumed.x, "потреблённое — это то, на что камера действительно сдвинулась")
  }

  @Test
  fun `a pan into the wall consumes nothing`() {
    val state = GraphCanvasState()
    state.place(count = 3)

    state.pan(Offset(x = -4000f, y = 0f))
    val consumed = state.pan(Offset(x = -120f, y = 0f))

    assertEquals(Offset.Zero, consumed, "у границы нулевое потребление и есть признак упора")
  }

  @Test
  fun `banked overshoot cannot outlive a layout pass`() {
    val state = GraphCanvasState()
    state.place(count = 3)
    state.pan(Offset(x = -4000f, y = 0f))

    // Ветка исчезла: узлов стало меньше, диапазон сузился.
    val narrowed = state.place(count = 1)

    val range = cameraRangeOf(narrowed, VIEWPORT, scale = 1f)
    assertEquals(
      range.clamp(state.offset.value),
      state.offset.value,
      "камера обязана сойтись с новым диапазоном в том же кадре, а не уехать телепортом позже"
    )
    assertEquals(
      100f,
      state.pan(Offset(x = 100f, y = 0f)).x,
      "и следующий жест обязан двигать картинку сразу, а не выбирать накопленный банк"
    )
  }

  @Test
  fun `an untouched camera follows new nodes`() {
    val state = GraphCanvasState()
    state.place(count = 1)
    val alone = state.offset.value

    val grown = state.place(count = 3)

    assertEquals(restOf(grown), state.offset.value)
    assertEquals(alone, state.offset.value, "покой наводится на первый узел, а он не сдвинулся")
  }

  @Test
  fun `a touched camera stays where the user left it`() {
    val state = GraphCanvasState()
    state.place(count = 3)

    state.pan(Offset(x = -100f, y = 0f))
    val left = state.offset.value
    state.place(count = 3)

    assertEquals(left, state.offset.value, "раскладка не отнимает камеру у того, кто её взял")
  }

  @Test
  fun `a layout pass does not subscribe to what the gesture writes`() {
    val state = GraphCanvasState()
    state.place(count = 3)
    state.pan(Offset(x = -100f, y = 0f))

    val readWhileMeasuring = mutableSetOf<Any>()
    Snapshot.observe(readObserver = { readWhileMeasuring += it }) {
      state.place(count = 3)
    }
    val writtenByGesture = mutableSetOf<Any>()
    Snapshot.observe(writeObserver = { writtenByGesture += it }) {
      state.pan(Offset(x = -10f, y = 0f))
    }

    assertTrue(
      readWhileMeasuring.intersect(writtenByGesture).isEmpty(),
      "камера пишется каждый кадр движения: подписав на неё измерение, полотно пере-измеряется " +
        "всю дорогу вместо того, чтобы двигать слой"
    )
  }

  @Test
  fun `a pinch keeps the focused point under the fingers`() {
    val state = GraphCanvasState()
    state.place(count = 6)
    // Камеру уводят с покоя намеренно: в покое она стоит у самой границы диапазона, и кламп там
    // отобрал бы у пинча ровно то, что проверяет этот тест.
    state.pan(Offset(x = -500f, y = 0f))
    val focus = Offset(x = 400f, y = 300f)
    val before = state.offset.value
    // Точка полотна, оказавшаяся под пальцами до пинча: масштаб ещё единичный.
    val point = focus - before

    state.zoom(focus = focus, change = 1.8f)

    val after = point * state.scale.value + state.offset.value
    assertEquals(focus.x, after.x, 0.5f, "пинч обязан приближать к пальцам, а не к углу экрана")
    assertEquals(focus.y, after.y, 0.5f)
  }

  @Test
  fun `a pinch out re-clamps the camera in the same step`() {
    val state = GraphCanvasState()
    state.place(count = 6)
    state.pan(Offset(x = -4000f, y = 0f))

    val step = state.zoom(focus = Offset(x = 500f, y = 300f), change = 0.5f)

    assertEquals(
      step.camera,
      state.offset.value,
      "камера обязана сойтись с диапазоном нового масштаба в том же шаге, а не всплыть телепортом"
    )
    assertTrue(
      state.offset.value.x >= -4000f,
      "уменьшив содержимое, полотно не может оставить камеру там, где её больше нет"
    )
  }

  @Test
  fun `the scale cannot leave the brief's range`() {
    val state = GraphCanvasState()
    state.place(count = 6)
    val focus = Offset(x = 500f, y = 300f)

    state.zoom(focus = focus, change = 100f)
    val zoomedIn = state.zoom(focus = focus, change = 2f)

    assertEquals(2.5f, zoomedIn.scale)
    assertTrue(zoomedIn.isRejected, "упор в предел — это свойство шага, а не догадка вызывающего")

    state.zoom(focus = focus, change = 0.001f)
    assertEquals(0.4f, state.scale.value)
  }

  @Test
  fun `the backdrop lags behind the camera`() {
    val state = GraphCanvasState()
    state.place(count = 6)

    state.pan(Offset(x = -500f, y = 0f))

    assertEquals(
      -150f,
      state.backdropOffset.value.x,
      "фон отстаёт на треть — это и есть та глубина, ради которой он нарисован"
    )
  }

  @Test
  fun `a wall stops the backdrop together with the graph`() {
    val state = GraphCanvasState()
    state.place(count = 6)
    state.pan(Offset(x = -9000f, y = 0f))
    val atTheWall = state.backdropOffset.value

    state.pan(Offset(x = -500f, y = 0f))

    assertEquals(
      atTheWall,
      state.backdropOffset.value,
      "у стенки граф стоит, и узор обязан стоять вместе с ним, а не ползти под плашками"
    )
  }

  @Test
  fun `a pinch keeps the backdrop point under the fingers too`() {
    val state = GraphCanvasState()
    state.place(count = 6)
    state.pan(Offset(x = -500f, y = 0f))
    val focus = Offset(x = 400f, y = 300f)
    val point = focus - state.backdropOffset.value

    state.zoom(focus = focus, change = 1.8f)

    val after = point * state.backdropScale.value + state.backdropOffset.value
    assertEquals(focus.x, after.x, 0.5f, "фон обязан зумиться вокруг пальцев, а не улетать за камерой")
    assertEquals(focus.y, after.y, 0.5f)
  }

  @Test
  fun `a layout pass does not subscribe to the scale either`() {
    val state = GraphCanvasState()
    state.place(count = 3)
    state.pan(Offset(x = -100f, y = 0f))

    val readWhileMeasuring = mutableSetOf<Any>()
    Snapshot.observe(readObserver = { readWhileMeasuring += it }) {
      state.place(count = 3)
    }
    val writtenByPinch = mutableSetOf<Any>()
    Snapshot.observe(writeObserver = { writtenByPinch += it }) {
      state.zoom(focus = Offset(x = 500f, y = 300f), change = 1.1f)
    }

    assertTrue(
      readWhileMeasuring.intersect(writtenByPinch).isEmpty(),
      "масштаб пишется каждый кадр пинча ровно так же, как камера, и подписанное на него " +
        "измерение перемеряет граф всю дорогу вместо того, чтобы двигать слой"
    )
  }

  @Test
  fun `a scrub puts the asked fraction under the centre of the screen`() {
    val state = GraphCanvasState()
    state.place(count = 12)

    state.scrubTo(fraction = 0.5f)

    assertEquals(
      0.5f,
      state.viewportSpan.value.position,
      1e-3f,
      "полоса и камера обязаны сходиться: у мини-карты это единственная связь с графом"
    )
  }

  @Test
  fun `a scrub to either end leaves the camera against its wall`() {
    val state = GraphCanvasState()
    val placement = state.place(count = 12)
    val range = cameraRangeOf(placement, VIEWPORT, scale = 1f)

    state.scrubTo(fraction = 1f)
    val atEnd = state.offset.value.x
    state.scrubTo(fraction = 0f)

    assertEquals(range.x.start, atEnd, "конец полосы — это упор камеры, а не «почти упор»")
    assertEquals(range.x.endInclusive, state.offset.value.x, "и начало полосы тоже")
  }

  @Test
  fun `a scrub leaves the vertical camera alone`() {
    val state = GraphCanvasState()
    state.place(count = 12)
    state.pan(Offset(x = 0f, y = -40f))
    val height = state.offset.value.y

    state.scrubTo(fraction = 0.8f)

    assertEquals(
      height,
      state.offset.value.y,
      "мини-карта водит по времени: вертикаль остаётся там, где её оставил палец"
    )
  }

  /**
   * Где камера обязана стоять в покое при этой раскладке.
   *
   * @param placement раскладка
   * @return положение покоя
   */
  private fun restOf(placement: GraphPlacement): Offset {
    return cameraRestOf(placement, VIEWPORT, cameraRangeOf(placement, VIEWPORT, scale = 1f), scale = 1f)
  }

  /**
   * Прямоугольник узла — то, из чего растёт превью-карточка, и берётся он у держателя, а не у самого
   * узла: узел живёт внутри слоя камеры, и общий элемент Compose этого слоя не видит.
   */
  @Test
  fun `a node rect stands where the plate is drawn`() {
    val state = GraphCanvasState()
    val placement = state.fill(count = 3)

    val rect = state.nodeRectOf(GraphNode.Id("n1"))

    assertEquals(
      placement.nodes[1].x + state.offset.value.x,
      rect?.left,
      "плашка нарисована там, куда её поставила раскладка, сдвинутая камерой"
    )
    assertEquals(120f, rect?.width, "ширина — измеренная, пока масштаб единичный")
  }

  @Test
  fun `a node rect follows the camera`() {
    val state = GraphCanvasState()
    state.fill(count = 3)
    val resting = state.nodeRectOf(GraphNode.Id("n1"))

    state.pan(Offset(x = -100f, y = 0f))

    assertEquals(
      resting!!.left - 100f,
      state.nodeRectOf(GraphNode.Id("n1"))?.left,
      "карточка обязана вырасти из того места, где плашка лежит сейчас, а не из места её покоя"
    )
  }

  @Test
  fun `an unknown node has no rect`() {
    val state = GraphCanvasState()
    state.fill(count = 3)

    assertNull(
      state.nodeRectOf(GraphNode.Id("no-such-node")),
      "узел, которого в раскладке ещё нет, обязан не находиться, а не индексироваться за конец"
    )
  }

  /**
   * Раскладка и список узлов подменяются порознь: первую пишет измерение, второй — `SideEffect`.
   * Тесту нужны оба, иначе узел по идентификатору не найти.
   *
   * @param count сколько узлов положить на магистраль
   * @return получившаяся раскладка
   */
  private fun GraphCanvasState.fill(count: Int): GraphPlacement {
    val placement = place(count)
    setNodes(nodes = trunkNodes(count), branches = emptyList())
    return placement
  }

  /**
   * Раскладывает граф из [count] одинаковых узлов на одной дорожке.
   *
   * @param count сколько узлов в графе
   * @return раскладка, которую держатель только что запомнил
   */
  private fun GraphCanvasState.place(count: Int): GraphPlacement {
    return layout(
      nodes = trunkNodes(count),
      branches = emptyList(),
      viewportSize = VIEWPORT,
      nodeSizes = List(count) { IntSize(width = 120, height = 28) },
      density = Density(density = 1f),
      statusBar = 0f,
      navigationBar = 0f
    )
  }
}

private val VIEWPORT = IntSize(width = 1000, height = 600)

/**
 * Цепочка узлов на магистрали.
 *
 * @param count сколько узлов нужно
 * @return узлы в хронологическом порядке
 */
private fun trunkNodes(count: Int): List<GraphNode> {
  return List(count) { index ->
    GraphNode(
      id = GraphNode.Id("n$index"),
      branchId = GraphBranch.Id("trunk"),
      role = GraphNodeRole.Episode,
      gap = TimeGap.Hour
    )
  }
}
