package ru.sla.atlas.ui

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.atlas.camera.cameraRangeOf
import ru.sla.atlas.camera.cameraRestOf
import ru.sla.atlas.entity.MockNode
import ru.sla.atlas.entity.Node
import ru.sla.atlas.entity.Placement
import ru.sla.atlas.layout.nearestCentreIndexOf
import ru.sla.atlas.lod.MockLevel
import ru.sla.atlas.lod.MockLevels
import ru.sla.atlas.lod.fitScaleOf

/**
 * Держатель проверяется без Compose: `mutableStateOf` и `derivedStateOf` работают и вне композиции,
 * а всё, что нужно [AtlasCanvasState.layout], — плотность и размеры, то есть обычные числа.
 *
 * Тест сторожит дефект, который довёл камеру до телепорта: сдвиг накапливался без границ, а кламп
 * стоял только на чтении. Пока это было так, упор в стенку банковал мёртвую зону, жест обратно
 * сначала выбирал её вхолостую, а следующая раскладка отдавала накопленное одним прыжком.
 */
class AtlasCanvasStateTest {

  @Test
  fun `a rejected delta cannot be banked for later`() {
    val state = mockCanvasState()
    state.place(count = 3)

    state.pan(Offset(x = -4000f, y = 0f))
    val atTheWall = state.offset.value.x
    val consumed = state.pan(Offset(x = 100f, y = 0f))

    assertEquals(atTheWall + 100f, state.offset.value.x, "картинка обязана пойти за пальцем сразу")
    assertEquals(100f, consumed.x, "потреблённое — это то, на что камера действительно сдвинулась")
  }

  @Test
  fun `a pan into the wall consumes nothing`() {
    val state = mockCanvasState()
    state.place(count = 3)

    state.pan(Offset(x = -4000f, y = 0f))
    val consumed = state.pan(Offset(x = -120f, y = 0f))

    assertEquals(Offset.Zero, consumed, "у границы нулевое потребление и есть признак упора")
  }

  @Test
  fun `banked overshoot cannot outlive a layout pass`() {
    val state = mockCanvasState()
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
    val state = mockCanvasState()
    state.place(count = 1)
    val alone = state.offset.value

    val grown = state.place(count = 3)

    assertEquals(restOf(grown), state.offset.value)
    assertEquals(alone, state.offset.value, "покой наводится на первый узел, а он не сдвинулся")
  }

  @Test
  fun `a touched camera stays where the user left it`() {
    val state = mockCanvasState()
    state.place(count = 3)

    state.pan(Offset(x = -100f, y = 0f))
    val left = state.offset.value
    state.place(count = 3)

    assertEquals(left, state.offset.value, "раскладка не отнимает камеру у того, кто её взял")
  }

  @Test
  fun `a layout pass does not subscribe to what the gesture writes`() {
    val state = mockCanvasState()
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
    val state = mockCanvasState()
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
    val state = mockCanvasState()
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
  fun `the scale cannot leave the band of its level`() {
    val state = mockCanvasState()
    state.place(count = 6)
    val focus = Offset(x = 500f, y = 300f)

    state.zoom(focus = focus, change = 100f)
    val zoomedIn = state.zoom(focus = focus, change = 2f)

    assertEquals(
      MockLevels.bandOf(MockLevel.Fine, fitScale = 1f).max,
      zoomedIn.scale,
      "потолок общий у обоих уровней: выше самого подробного уровня нет"
    )
    assertTrue(zoomedIn.isRejected, "упор в предел — это свойство шага, а не догадка вызывающего")

    // Вниз с подробного уровня масштаб не упирается, а уводит на обзорный: это проверяют тесты
    // перехода. Ниже самого обзорного уровня нет, и вот там упор настоящий.
    state.fitAll()
    val overview = state.place(count = 6, level = state.level.value)
    state.zoom(focus = focus, change = 0.001f)

    assertEquals(
      MockLevels.bandOf(MockLevel.Coarse, fitScaleOf(overview.bounds, VIEWPORT)).min,
      state.scale.value,
      "ниже самого обзорного уровня нет: там масштаб упирается в нижний край его полосы"
    )
  }

  @Test
  fun `the backdrop lags behind the camera`() {
    val state = mockCanvasState()
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
    val state = mockCanvasState()
    state.place(count = 6)
    state.pan(Offset(x = -9000f, y = 0f))
    val atTheWall = state.backdropOffset.value

    state.pan(Offset(x = -500f, y = 0f))

    assertEquals(
      atTheWall,
      state.backdropOffset.value,
      "у стенки граф стоит, и узор обязан стоять вместе с ним, а не ползти под узлами"
    )
  }

  @Test
  fun `a pinch keeps the backdrop point under the fingers too`() {
    val state = mockCanvasState()
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
    val state = mockCanvasState()
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
    val state = mockCanvasState()
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
    val state = mockCanvasState()
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
    val state = mockCanvasState()
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
  private fun restOf(placement: Placement): Offset {
    return cameraRestOf(placement, VIEWPORT, cameraRangeOf(placement, VIEWPORT, scale = 1f), scale = 1f)
  }

  /**
   * Смена уровня меняет раскладку скачком: зазоры и дорожки сжимаются, узел мельчает.
   * Единственное, что удерживает переход от телепорта, — якорь: узел под пальцами обязан остаться под
   * пальцами, а охват — прежним.
   */
  @Test
  fun `a level switch keeps the anchor node under the same screen point`() {
    val state = mockCanvasState()
    val before = state.fill(count = 9)
    state.pan(Offset(x = -800f, y = 0f))
    val focus = Offset(x = 700f, y = 300f)
    val anchor = nearestCentreIndexOf(
      centres = before.centres,
      x = (focus.x - state.offset.value.x) / state.scale.value
    )

    state.zoom(focus = focus, change = 0.3f)
    state.fill(count = 9, level = state.level.value)

    assertEquals(MockLevel.Coarse, state.level.value, "щипок ниже полосы уводит на обзорный уровень")
    val landed = state.nodeRectOf(Node.Id("n$anchor"))
    assertNotNull(landed, "якорный узел обязан найтись и на новом уровне: список узлов уровень не меняет")
    assertEquals(
      focus.x,
      landed!!.center.x,
      1f,
      "узел под пальцами обязан остаться под пальцами: иначе переход читается телепортом"
    )
  }

  @Test
  fun `a level switch lands the scale inside the new band`() {
    val state = mockCanvasState()
    state.fill(count = 9)
    state.pan(Offset(x = -800f, y = 0f))

    state.zoom(focus = Offset(x = 700f, y = 300f), change = 0.3f)
    val placement = state.fill(count = 9, level = state.level.value)

    val band = MockLevels.bandOf(MockLevel.Coarse, fitScaleOf(placement.bounds, VIEWPORT))
    assertTrue(
      state.scale.value >= band.min && state.scale.value <= band.max,
      "масштаб посадки обязан лежать в полосе нового уровня, а не в полосе покинутого"
    )
    assertTrue(
      state.scale.value <= band.max * (1f - 0.08f) + 1e-4f,
      "и держаться от её края: посадка на самом потолке возвращала бы обратно от дрожания пальца"
    )
  }

  /**
   * Порог сравнивается с масштабом, запрошенным одним событием жеста, а посадка отходит от края
   * полосы на восемь процентов. При девяноста событиях в секунду быстрый щипок проходит этот запас
   * за пару событий — и уровень начинает мигать, держа на каждый узел по два представления. Запас
   * увеличить нельзя: он и есть то, чем стык уровней держится бесшовным.
   */
  @Test
  fun `a second level switch waits for the crossfade of the first`() {
    val state = mockCanvasState()
    state.fill(count = 9)
    state.pan(Offset(x = -800f, y = 0f))
    val focus = Offset(x = 700f, y = 300f)

    state.zoom(focus = focus, change = 0.3f)
    state.fill(count = 9, level = state.level.value)
    // Щипка вдвое хватило бы на возврат: посадка стоит в восьми процентах от потолка полосы.
    state.zoom(focus = focus, change = 2f)

    assertEquals(
      MockLevel.Coarse,
      state.level.value,
      "пока кроссфейд не доигран, край полосы работает стенкой, а не переходом: иначе быстрый щипок " +
        "мигал бы уровнем, ни разу его не показав"
    )

    state.onLevelSettled()
    state.zoom(focus = focus, change = 2f)

    assertEquals(
      MockLevel.Fine,
      state.level.value,
      "доигранный переход отпускает уровень: тот же щипок обязан увести обратно"
    )
  }

  @Test
  fun `a layout pass does not subscribe to the level either`() {
    val state = mockCanvasState()
    state.fill(count = 3)

    val readWhileMeasuring = mutableSetOf<Any>()
    Snapshot.observe(readObserver = { readWhileMeasuring += it }) {
      state.place(count = 3, level = MockLevel.Coarse)
    }
    val writtenByGesture = mutableSetOf<Any>()
    Snapshot.observe(writeObserver = { writtenByGesture += it }) {
      state.zoom(focus = Offset(x = 500f, y = 300f), change = 0.3f)
    }

    assertTrue(
      readWhileMeasuring.intersect(writtenByGesture).isEmpty(),
      "уровень приходит в измерение параметром: прочитав его состоянием, измерение взяло бы зазоры " +
        "одного уровня к узлам, которые композиция построила по другому"
    )
  }

  @Test
  fun `a second double tap returns the camera, the scale and the level`() {
    val state = mockCanvasState()
    state.fill(count = 9)
    state.pan(Offset(x = -300f, y = 0f))
    val level = state.level.value
    val scale = state.scale.value
    val camera = state.offset.value

    state.fitAll()
    state.fill(count = 9, level = state.level.value)
    assertEquals(MockLevel.Coarse, state.level.value, "вписывание уводит на самый обзорный уровень")

    state.fitAll()
    state.fill(count = 9, level = state.level.value)

    assertEquals(level, state.level.value, "повторный тап возвращает уровень")
    assertEquals(scale, state.scale.value, "и масштаб")
    assertEquals(camera, state.offset.value, "и камеру — все три величины описывают одно положение")
  }

  @Test
  fun `a switch on an empty graph changes nothing`() {
    val state = mockCanvasState()

    state.zoom(focus = Offset(x = 500f, y = 300f), change = 0.1f)
    state.fitAll()

    assertEquals(
      MockLevel.Fine,
      state.level.value,
      "уводить некуда: на пустом полотне ни якоря, ни охвата не существует"
    )
    assertEquals(
      MockLevels.bandOf(MockLevel.Fine, fitScale = 1f).min,
      state.scale.value,
      "масштаб при этом обязан упереться в нижний край полосы"
    )
  }

  /**
   * Прямоугольник узла — то, из чего растёт превью-карточка, и берётся он у держателя, а не у самого
   * узла: узел живёт внутри слоя камеры, и общий элемент Compose этого слоя не видит.
   */
  @Test
  fun `a node rect stands where the plate is drawn`() {
    val state = mockCanvasState()
    val placement = state.fill(count = 3)

    val rect = state.nodeRectOf(Node.Id("n1"))

    assertEquals(
      placement.nodes[1].x + state.offset.value.x,
      rect?.left,
      "узел нарисован там, куда его поставила раскладка, сдвинутая камерой"
    )
    assertEquals(120f, rect?.width, "ширина — измеренная, пока масштаб единичный")
  }

  @Test
  fun `a node rect follows the camera`() {
    val state = mockCanvasState()
    state.fill(count = 3)
    val resting = state.nodeRectOf(Node.Id("n1"))

    state.pan(Offset(x = -100f, y = 0f))

    assertEquals(
      resting!!.left - 100f,
      state.nodeRectOf(Node.Id("n1"))?.left,
      "то, что растёт из узла, обязано начаться там, где он лежит сейчас, а не в его покое"
    )
  }

  @Test
  fun `an unknown node has no rect`() {
    val state = mockCanvasState()
    state.fill(count = 3)

    assertNull(
      state.nodeRectOf(Node.Id("no-such-node")),
      "узел, которого в раскладке ещё нет, обязан не находиться, а не индексироваться за конец"
    )
  }

  /**
   * Раскладка и граф подменяются порознь: первую пишет измерение, второй — `SideEffect`.
   * Тесту нужны оба, иначе узел по идентификатору не найти.
   *
   * @param count сколько узлов положить на магистраль
   * @param level уровень детализации, которым меряем
   * @return получившаяся раскладка
   */
  private fun AtlasCanvasState<MockNode, MockLevel>.fill(
    count: Int,
    level: MockLevel = MockLevel.Fine
  ): Placement {
    val placement = place(count, level)
    setGraph(mockBaselineGraph(count))
    return placement
  }

  /**
   * Раскладывает граф из [count] одинаковых узлов на одной дорожке.
   *
   * @param count сколько узлов в графе
   * @param level уровень детализации, которым меряем
   * @return раскладка, которую держатель только что запомнил
   */
  private fun AtlasCanvasState<MockNode, MockLevel>.place(
    count: Int,
    level: MockLevel = MockLevel.Fine
  ): Placement {
    val graph = mockBaselineGraph(count)
    return layout(
      level = level,
      graph = graph,
      lanes = graph.mockLanes(),
      branchColors = graph.mockColors(),
      viewportSize = VIEWPORT,
      nodeSizes = List(count) { IntSize(width = 120, height = 28) },
      density = Density(density = 1f),
      margins = mockCanvasMargins()
    )
  }
}

private val VIEWPORT = IntSize(width = 1000, height = 600)
