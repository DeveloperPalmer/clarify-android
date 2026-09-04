package ru.sla.atlas.ui

import androidx.compose.animation.core.tween
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.atlas.camera.cameraAimedAt
import ru.sla.atlas.camera.cameraRangeOf
import ru.sla.atlas.camera.flightScaleOf
import ru.sla.atlas.entity.Graph
import ru.sla.atlas.entity.MockNode
import ru.sla.atlas.entity.Placement
import ru.sla.atlas.entity.mockBranch
import ru.sla.atlas.entity.mockNode
import ru.sla.atlas.lod.MockLevel
import ru.sla.atlas.lod.MockLevels

/**
 * Перелёт камеры по кнопке: третий источник её движения после жеста и затухания.
 *
 * Проверяется на управляемых часах и без Compose-рантайма, тем же способом, что и инерция:
 * `BroadcastFrameClock` лежит в runtime, кадры шлёт тест.
 *
 * Сторожит три вещи, каждая из которых ломается молча. Перелёт обязан **доводить** камеру до
 * наведения на цель, а не останавливаться где-то рядом. Он обязан **отдавать** камеру пальцу — и
 * пальцу на полотне, и скрабу мини-карты, — потому что делит с затуханием один job, и стоит завести
 * ему свой, как отмена начнёт работать через раз. И он не должен пытаться лететь там, где графа ещё
 * нет: пустая раскладка — обычное состояние экрана до первого измерения.
 */
class CameraFlightTest {

  @Test
  fun `a flight lands on the front of the conversation`() = runTest {
    val state = laidOut()
    val before = state.offset.value

    state.fly(this, FRONT)

    assertNotEquals(before, state.offset.value, "перелёт обязан сдвинуть камеру")
    assertEquals(
      aimedAt(state, FRONT),
      state.offset.value,
      "и довести её до наведения на последний узел, а не остановиться рядом"
    )
  }

  @Test
  fun `a flight home lands where the camera rests`() = runTest {
    val state = laidOut()
    state.pan(Offset(x = -3000f, y = 0f))

    state.fly(this, START)

    assertEquals(
      aimedAt(state, START),
      state.offset.value,
      "«к началу» приводит туда же, где камера стоит в покое: определение наведения одно на оба"
    )
  }

  @Test
  fun `a flight moves the camera gradually instead of teleporting it`() = runTest {
    val state = laidOut()
    val trail = state.flightTrail(this, FRONT)

    assertTrue(
      trail.size > 4,
      "перелёт обязан занять несколько кадров: камера, прыгнувшая через всю историю, не оставляет " +
        "зрителю ничего, из чего понять, куда он попал"
    )
    assertTrue(
      trail.zipWithNext().all { (previous, current) -> current.x <= previous.x },
      "и вести камеру в одну сторону, без рывков назад"
    )
  }

  @Test
  fun `a touch takes the camera away from a running flight`() = runTest {
    val state = laidOut()
    val clock = BroadcastFrameClock()
    state.flyTo(this + clock, tween(durationMillis = 400), FRONT)
    runCurrent()
    clock.sendFrame(FRAME_NANOS)
    runCurrent()

    state.pan(Offset(x = 5f, y = 0f))
    runCurrent()
    val afterTouch = state.offset.value
    clock.sendFrame(2 * FRAME_NANOS)
    runCurrent()

    assertEquals(afterTouch, state.offset.value, "палец отбирает камеру и у перелёта")
    state.stopMotion()
  }

  @Test
  fun `a flight takes the camera away from a running fling`() = runTest {
    val state = laidOut()
    val clock = BroadcastFrameClock()
    state.fling(this + clock, Velocity(x = -1200f, y = -700f), androidx.compose.animation.core.exponentialDecay())
    runCurrent()
    clock.sendFrame(FRAME_NANOS)
    runCurrent()

    // Оба живут в одном job, поэтому отмена достаётся даром: заведи перелёту свой, и здесь два
    // источника начали бы писать камеру наперегонки.
    state.fly(this, START)

    assertEquals(
      aimedAt(state, START),
      state.offset.value,
      "перелёт обязан начаться с того, что оборвёт инерцию"
    )
  }

  @Test
  fun `a flight brings a zoomed-in camera back to the default scale`() = runTest {
    val state = laidOut()
    state.zoom(focus = Offset(x = 200f, y = 100f), change = 2.5f)

    state.fly(this, FRONT)

    assertEquals(
      1f,
      state.scale.value,
      1e-4f,
      "прилетев на приближённом масштабе, зритель видит такой же кусок истории, только другой"
    )
  }

  @Test
  fun `a flight leaves a zoomed-out camera at its own scale`() = runTest {
    val state = laidOut()
    // Внутри полосы уровня: щипок за её край — это уже переход, а не отдаление, и проверялось бы
    // тогда не то.
    state.zoom(focus = Offset(x = 200f, y = 100f), change = 0.6f)
    val zoomedOut = state.scale.value

    state.fly(this, START)

    assertEquals(
      zoomedOut,
      state.scale.value,
      "отдалившийся смотрит обзорно намеренно: он просил сменить место, а не масштаб"
    )
  }

  @Test
  fun `the scale returns to the default gradually, without a jump at the end`() = runTest {
    val state = laidOut()
    state.zoom(focus = Offset(x = 200f, y = 100f), change = 2.5f)

    val trail = state.scaleTrail(this, FRONT)

    assertTrue(trail.size > 4, "сброс обязан занять те же кадры, что и перелёт")
    assertTrue(
      trail.zipWithNext().all { (previous, current) -> current <= previous + 1e-4f },
      "масштаб идёт вниз монотонно, без рывка назад"
    )
    assertEquals(1f, trail.last(), 1e-4f, "и заканчивается ровно на единице вместе с перелётом")
  }

  @Test
  fun `the default scale is only restored from above`() {
    val fineRest = MockLevels.restScaleOf(MockLevel.Fine, MockLevels.bandOf(MockLevel.Fine, fitScale = 0.05f))

    assertEquals(1f, flightScaleOf(2.5f, fineRest))
    assertEquals(1f, flightScaleOf(1.0001f, fineRest))
    assertEquals(1f, flightScaleOf(1f, fineRest), "на самой единице сбрасывать нечего")
    assertEquals(0.4f, flightScaleOf(0.4f, fineRest), "ниже покоя перелёт масштаб не трогает")
  }

  @Test
  fun `a coarse level returns to its own rest, not to the unit scale`() {
    val coarseRest = MockLevels.restScaleOf(MockLevel.Coarse, MockLevels.bandOf(MockLevel.Coarse, fitScale = 0.305f))

    assertEquals(
      0.305f,
      flightScaleOf(2.3f, coarseRest),
      1e-4f,
      "покой обзорного уровня — это «видно всё», а единица там не значит ничего"
    )
    assertEquals(
      0.305f,
      flightScaleOf(0.305f, coarseRest),
      1e-4f,
      "на самом покое сбрасывать нечего"
    )
  }

  @Test
  fun `a flight over an empty graph does nothing`() = runTest {
    val state = mockCanvasState()
    val before = state.offset.value

    state.fly(this, FRONT)

    assertEquals(before, state.offset.value, "до первого измерения лететь некуда")
  }

  /**
   * Где обязана оказаться камера после перелёта к [target].
   *
   * @param state состояние с уже выполненной раскладкой
   * @param target куда лететь
   * @return положение камеры, зажатое её диапазоном
   */
  private fun aimedAt(state: AtlasCanvasState<MockNode, MockLevel>, target: Placement.() -> Offset): Offset {
    val placement = state.layout(
      level = MockLevel.Fine,
      graph = state.graph,
      lanes = state.graph.mockLanes(),
      branchColors = state.graph.mockColors(),
      viewportSize = VIEWPORT,
      nodeSizes = List(state.graph.nodes.size) { NODE_SIZE },
      density = Density(density = 1f),
      margins = mockCanvasMargins()
    )
    val point = placement.target()
    return cameraAimedAt(
      point = point,
      viewport = VIEWPORT,
      range = cameraRangeOf(placement, VIEWPORT, scale = 1f),
      scale = 1f
    )
  }

  /**
   * Проигрывает перелёт до конца.
   *
   * @param scope scope теста
   * @param target куда лететь
   */
  private fun AtlasCanvasState<MockNode, MockLevel>.fly(scope: TestScope, target: Placement.() -> Offset) {
    flightTrail(scope, target)
  }

  /**
   * Проигрывает перелёт до конца, снимая масштаб на каждом кадре.
   *
   * @param scope scope теста
   * @param target куда лететь
   * @return масштаб на каждом кадре перелёта
   */
  private fun AtlasCanvasState<MockNode, MockLevel>.scaleTrail(
    scope: TestScope,
    target: Placement.() -> Offset
  ): List<Float> {
    val clock = BroadcastFrameClock()
    flyTo(scope + clock, tween(durationMillis = 400), target)
    scope.runCurrent()
    val trail = mutableListOf<Float>()
    var nanos = 0L
    while (clock.hasAwaiters) {
      nanos += FRAME_NANOS
      clock.sendFrame(nanos)
      scope.runCurrent()
      trail += scale.value
    }
    return trail
  }

  /**
   * Проигрывает перелёт до конца, снимая камеру на каждом кадре.
   *
   * `runCurrent` перед циклом обязателен: на `StandardTestDispatcher` корутина до него не стартует,
   * ждущих у часов ещё нет, и цикл завершился бы, не начавшись.
   *
   * @param scope scope теста
   * @param target куда лететь
   * @return камера на каждом кадре перелёта
   */
  private fun AtlasCanvasState<MockNode, MockLevel>.flightTrail(
    scope: TestScope,
    target: Placement.() -> Offset
  ): List<Offset> {
    val clock = BroadcastFrameClock()
    flyTo(scope + clock, tween(durationMillis = 400), target)
    scope.runCurrent()
    val trail = mutableListOf<Offset>()
    var nanos = 0L
    while (clock.hasAwaiters) {
      nanos += FRAME_NANOS
      clock.sendFrame(nanos)
      scope.runCurrent()
      trail += offset.value
    }
    return trail
  }

  /**
   * Полотно шире экрана: перелёту есть куда вести камеру.
   *
   * @return состояние с уже выполненной раскладкой
   */
  private fun laidOut(): AtlasCanvasState<MockNode, MockLevel> {
    val state = mockCanvasState()
    val graph = fiveBranchGraph()
    state.setGraph(graph)
    state.layout(
      level = MockLevel.Fine,
      graph = graph,
      lanes = graph.mockLanes(),
      branchColors = graph.mockColors(),
      viewportSize = VIEWPORT,
      nodeSizes = List(graph.nodes.size) { NODE_SIZE },
      density = Density(density = 1f),
      margins = mockCanvasMargins()
    )
    return state
  }
}

private const val FRAME_NANOS = 16_666_666L
private val VIEWPORT = IntSize(width = 400, height = 200)
private val NODE_SIZE = IntSize(width = 120, height = 28)

/**
 * Граф на пять веток: узлы идут по кругу, магистраль — нулевая ветка.
 *
 * Пять веток вместо пяти дорожек: дорожку теперь назначает раскраска, и вертикальный диапазон
 * камеры обязан появиться из данных, а не из литерала.
 *
 * @return граф из двенадцати узлов, разобранных ветками без остатка
 */
private fun fiveBranchGraph(): Graph<MockNode> {
  val nodes = List(12) { index ->
    mockNode("n$index")
  }
  val nodeIdsByBranch = nodes.withIndex().groupBy({ it.index % 5 }, { it.value.id })
  return Graph(
    nodes = nodes,
    baseline = mockBranch(
      id = "b0",
      nodes = nodeIdsByBranch.getValue(0).map { it.value }
    ),
    branches = (1..4).map { index ->
      mockBranch(
        id = "b$index",
        nodes = nodeIdsByBranch.getValue(index).map { it.value }
      )
    }
  )
}

// Куда летит камера в тесте: начало истории и её фронт — те же два места, что показывает экран.
private val START: Placement.() -> Offset = { centres.first() }
private val FRONT: Placement.() -> Offset = { centres.last() }
