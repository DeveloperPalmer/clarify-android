package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.animation.core.exponentialDecay
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.atlas.camera.FlingDirection
import ru.sla.atlas.entity.BasicNode
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.Graph
import ru.sla.atlas.entity.TimeGap
import ru.sla.atlas.ui.AtlasCanvasState
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel
import ru.sla.clarify.feature.chronology.ui.entity.Node

/**
 * Цикл затухания целиком, на управляемых часах и без Compose-рантайма: `BroadcastFrameClock` лежит в
 * runtime, а кадры шлёт тест, поэтому анимация проигрывается за миллисекунды и детерминированно.
 *
 * Кривая здесь **не** платформенная: `splineBasedDecay` читает `ViewConfiguration.getScrollFriction`,
 * то есть статику Android-фреймворка, которой в юнит-тесте нет. Проверяется поэтому структура
 * движения — прямизна, останов, скольжение вдоль стенки, — а совпадение пролёта с таблицей спеки
 * сверяется на устройстве. Для структуры кривая безразлична: направление задаёт [FlingDirection],
 * а не спека.
 */
class GraphFlingTest {

  @Test
  fun `a diagonal fling keeps its direction`() = runTest {
    val state = laidOut()
    val start = state.offset.value

    val trail = state.flingTrail(this, Velocity(x = -1200f, y = -700f))

    // Первый кадр анимации приходит с нулевым временем и ничего не двигает — по этой же причине
    // правило остановки смотрит на диапазон, а не на то, сколько взяла последняя дельта.
    val travelled = trail.map { it - start }.filter { it != Offset.Zero }
    assertTrue(travelled.size > 4, "бросок обязан занять несколько кадров, иначе проверять нечего")
    travelled.forEach { step ->
      assertEquals(
        700f / 1200f,
        step.y / step.x,
        1e-3f,
        "отношение смещений держится на всём интервале, а не только в конце"
      )
    }
  }

  @Test
  fun `a fling stops at the boundary instead of hammering it`() = runTest {
    val state = laidOut()

    // Скорость заведомо больше того, что помещается в диапазон: бросок обязан упереться.
    state.flingTrail(this, Velocity(x = -90_000f, y = -90_000f))

    assertTrue(
      state.telemetry.read().flingStalls <= 1,
      "у стенки затухание обрывается, а не доигрывает свою длительность за границей"
    )
  }

  @Test
  fun `a new touch cancels the running fling`() = runTest {
    val state = laidOut()
    val clock = BroadcastFrameClock()
    state.fling(this + clock, Velocity(x = -1200f, y = -700f), exponentialDecay())
    runCurrent()
    clock.sendFrame(FRAME_NANOS)
    runCurrent()

    state.pan(Offset(x = 5f, y = 0f))
    runCurrent()
    val afterTouch = state.offset.value
    clock.sendFrame(2 * FRAME_NANOS)
    runCurrent()

    assertEquals(afterTouch, state.offset.value, "палец отбирает камеру у анимации")
    state.stopMotion()
  }

  @Test
  fun `a scrub cancels the running fling too`() = runTest {
    val state = laidOut()
    val clock = BroadcastFrameClock()
    state.fling(this + clock, Velocity(x = -1200f, y = -700f), exponentialDecay())
    runCurrent()
    clock.sendFrame(FRAME_NANOS)
    runCurrent()

    // Палец, положенный на мини-карту, до полотна не доходит вовсе: жест, начатый в зоне
    // инструмента, отбрасывается ещё в детекторе. Обрывать инерцию поэтому обязан сам скраб — и
    // обрывает он её тем, что идёт через `pan`, а не собственной записью камеры.
    state.scrubTo(fraction = 0.5f)
    runCurrent()
    val afterScrub = state.offset.value
    clock.sendFrame(2 * FRAME_NANOS)
    runCurrent()

    assertEquals(afterScrub, state.offset.value, "скраб отбирает камеру у затухания")
    state.stopMotion()
  }

  @Test
  fun `a slow release does not fling at all`() = runTest {
    val state = laidOut()
    val before = state.offset.value

    state.flingTrail(this, Velocity(x = -0.5f, y = -0.5f))

    assertEquals(before, state.offset.value, "ниже платформенного барьера бросок не начинается")
  }

  /**
   * Проигрывает затухание до конца, снимая камеру на каждом кадре.
   *
   * `runCurrent` перед циклом обязателен: на `StandardTestDispatcher` корутина до него не стартует,
   * ждущих у часов ещё нет, и цикл завершился бы, не начавшись.
   *
   * @param scope scope теста
   * @param velocity скорость отпускания
   * @return камера на каждом кадре затухания
   */
  private fun AtlasCanvasState<Node, GraphLevel>.flingTrail(scope: TestScope, velocity: Velocity): List<Offset> {
    val clock = BroadcastFrameClock()
    fling(scope + clock, velocity, exponentialDecay())
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
   * Полотно, у которого есть запас хода по обеим осям: узлы стоят в ряд и занимают несколько дорожек.
   *
   * @return состояние с уже выполненной раскладкой
   */
  private fun laidOut(): AtlasCanvasState<Node, GraphLevel> {
    val state = mockCanvasState()
    val graph = fiveBranchGraph()
    state.setGraph(graph)
    state.layout(
      level = GraphLevel.Episodes,
      graph = graph,
      lanes = graph.mockLanes(),
      branchColors = graph.mockBranchColors(),
      viewportSize = IntSize(width = 400, height = 200),
      nodeSizes = List(graph.nodes.size) { IntSize(width = 120, height = 28) },
      density = Density(density = 1f),
      margins = mockCanvasMargins()
    )
    return state
  }
}

private const val FRAME_NANOS = 16_666_666L

/**
 * Граф на пять веток: узлы идут по кругу, магистраль — нулевая ветка.
 *
 * Пять веток вместо пяти дорожек: дорожку теперь назначает раскраска, и вертикальный диапазон
 * камеры обязан появиться из данных, а не из литерала.
 *
 * @return граф из двенадцати узлов, разобранных ветками без остатка
 */
private fun fiveBranchGraph(): Graph<Node> {
  val nodes = List(12) { index ->
    Node.Episode(
      id = BasicNode.Id("n$index"),
      gap = TimeGap.Long,
      time = "6 мар, 10:00",
      count = 1,
      snippet = "",
      myShare = 0f,
      unreadCount = 0,
      dim = false
    )
  }
  val nodeIdsByBranch = nodes.withIndex().groupBy({ it.index % 5 }, { it.value.id })
  return Graph(
    nodes = nodes,
    baseline = Branch(
      id = Branch.Id("b0"),
      nodeIds = nodeIdsByBranch.getValue(0),
      colorIndex = 0,
      forkedFrom = null,
      mergedAt = null,
      status = Branch.Status.Alive
    ),
    branches = (1..4).map { index ->
      Branch(
        id = Branch.Id("b$index"),
        nodeIds = nodeIdsByBranch.getValue(index),
        colorIndex = index,
        forkedFrom = null,
        mergedAt = null,
        status = Branch.Status.Alive
      )
    }
  )
}
