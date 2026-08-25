package ru.sla.clarify.feature.chronology.ui.components.canvas

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
import ru.sla.clarify.feature.chronology.ui.entity.GraphAnchor
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.TimeGap

/**
 * Перелёт камеры по кнопке: третий источник её движения после жеста и затухания.
 *
 * Проверяется на управляемых часах и без Compose-рантайма, тем же способом, что и инерция:
 * `BroadcastFrameClock` лежит в runtime, кадры шлёт тест.
 *
 * Сторожит три вещи, каждая из которых ломается молча. Перелёт обязан **доводить** камеру до
 * наведения на якорь, а не останавливаться где-то рядом. Он обязан **отдавать** камеру пальцу — и
 * пальцу на полотне, и скрабу мини-карты, — потому что делит с затуханием один job, и стоит завести
 * ему свой, как отмена начнёт работать через раз. И он не должен пытаться лететь там, где графа ещё
 * нет: пустая раскладка — обычное состояние экрана до первого измерения.
 */
class GraphFlightTest {

  @Test
  fun `a flight lands on the front of the conversation`() = runTest {
    val state = laidOut()
    val before = state.offset.value

    state.fly(this, GraphAnchor.Front)

    assertNotEquals(before, state.offset.value, "перелёт обязан сдвинуть камеру")
    assertEquals(
      aimedAt(state, GraphAnchor.Front),
      state.offset.value,
      "и довести её до наведения на последний эпизод, а не остановиться рядом"
    )
  }

  @Test
  fun `a flight home lands where the camera rests`() = runTest {
    val state = laidOut()
    state.pan(Offset(x = -3000f, y = 0f))

    state.fly(this, GraphAnchor.Start)

    assertEquals(
      aimedAt(state, GraphAnchor.Start),
      state.offset.value,
      "«к началу» приводит туда же, где камера стоит в покое: определение наведения одно на оба"
    )
  }

  @Test
  fun `a flight moves the camera gradually instead of teleporting it`() = runTest {
    val state = laidOut()
    val trail = state.flightTrail(this, GraphAnchor.Front)

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
    state.flyTo(this + clock, tween(durationMillis = 400), GraphAnchor.Front)
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
    state.fly(this, GraphAnchor.Start)

    assertEquals(
      aimedAt(state, GraphAnchor.Start),
      state.offset.value,
      "перелёт обязан начаться с того, что оборвёт инерцию"
    )
  }

  @Test
  fun `a flight brings a zoomed-in camera back to the default scale`() = runTest {
    val state = laidOut()
    state.zoom(focus = Offset(x = 200f, y = 100f), change = 2.5f)

    state.fly(this, GraphAnchor.Front)

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
    state.zoom(focus = Offset(x = 200f, y = 100f), change = 0.4f)
    val zoomedOut = state.scale.value

    state.fly(this, GraphAnchor.Start)

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

    val trail = state.scaleTrail(this, GraphAnchor.Front)

    assertTrue(trail.size > 4, "сброс обязан занять те же кадры, что и перелёт")
    assertTrue(
      trail.zipWithNext().all { (previous, current) -> current <= previous + 1e-4f },
      "масштаб идёт вниз монотонно, без рывка назад"
    )
    assertEquals(1f, trail.last(), 1e-4f, "и заканчивается ровно на единице вместе с перелётом")
  }

  @Test
  fun `the default scale is only restored from above`() {
    assertEquals(1f, flightScaleOf(2.5f))
    assertEquals(1f, flightScaleOf(1.0001f))
    assertEquals(1f, flightScaleOf(1f), "на самой единице сбрасывать нечего")
    assertEquals(0.4f, flightScaleOf(0.4f))
  }

  @Test
  fun `a flight over an empty graph does nothing`() = runTest {
    val state = GraphCanvasState()
    val before = state.offset.value

    state.fly(this, GraphAnchor.Front)

    assertEquals(before, state.offset.value, "до первого измерения лететь некуда")
  }

  /**
   * Где обязана оказаться камера после перелёта к [anchor].
   *
   * @param state состояние с уже выполненной раскладкой
   * @param anchor якорь перелёта
   * @return положение камеры, зажатое её диапазоном
   */
  private fun aimedAt(state: GraphCanvasState, anchor: GraphAnchor): Offset {
    val placement = state.layout(
      nodes = state.nodes,
      viewportSize = VIEWPORT,
      nodeSizes = List(state.nodes.size) { NODE_SIZE },
      density = Density(density = 1f),
      statusBar = 0f,
      navigationBar = 0f
    )
    val point = when (anchor) {
      GraphAnchor.Start -> placement.centres.first()
      GraphAnchor.Front -> placement.centres.last()
    }
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
   * @param anchor якорь перелёта
   */
  private fun GraphCanvasState.fly(scope: TestScope, anchor: GraphAnchor) {
    flightTrail(scope, anchor)
  }

  /**
   * Проигрывает перелёт до конца, снимая масштаб на каждом кадре.
   *
   * @param scope scope теста
   * @param anchor якорь перелёта
   * @return масштаб на каждом кадре перелёта
   */
  private fun GraphCanvasState.scaleTrail(scope: TestScope, anchor: GraphAnchor): List<Float> {
    val clock = BroadcastFrameClock()
    flyTo(scope + clock, tween(durationMillis = 400), anchor)
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
   * @param anchor якорь перелёта
   * @return камера на каждом кадре перелёта
   */
  private fun GraphCanvasState.flightTrail(scope: TestScope, anchor: GraphAnchor): List<Offset> {
    val clock = BroadcastFrameClock()
    flyTo(scope + clock, tween(durationMillis = 400), anchor)
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
  private fun laidOut(): GraphCanvasState {
    val state = GraphCanvasState()
    val nodes = List(12) { index ->
      GraphNode(id = GraphNode.Id("n$index"), lane = index % 5, gap = TimeGap.Long)
    }
    state.setNodes(nodes)
    state.layout(
      nodes = nodes,
      viewportSize = VIEWPORT,
      nodeSizes = List(nodes.size) { NODE_SIZE },
      density = Density(density = 1f),
      statusBar = 0f,
      navigationBar = 0f
    )
    return state
  }
}

private const val FRAME_NANOS = 16_666_666L
private val VIEWPORT = IntSize(width = 400, height = 200)
private val NODE_SIZE = IntSize(width = 120, height = 28)
