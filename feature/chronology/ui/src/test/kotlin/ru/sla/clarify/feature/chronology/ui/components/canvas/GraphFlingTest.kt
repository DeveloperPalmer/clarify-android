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
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranch
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranchStatus
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeRole
import ru.sla.clarify.feature.chronology.ui.entity.TimeGap

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
  private fun GraphCanvasState.flingTrail(scope: TestScope, velocity: Velocity): List<Offset> {
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
  private fun laidOut(): GraphCanvasState {
    val state = GraphCanvasState()
    val nodes = List(12) { index ->
      GraphNode(
        id = GraphNode.Id("n$index"),
        // Пять веток вместо пяти дорожек: дорожку теперь назначает раскраска, и вертикальный
        // диапазон камеры обязан появиться из данных, а не из литерала.
        branchId = GraphBranch.Id("b${index % 5}"),
        role = GraphNodeRole.Episode,
        gap = TimeGap.Long
      )
    }
    state.layout(
      level = GraphLevel.Episodes,
      nodes = nodes,
      lanes = graphLanesOf(nodes, FIVE_BRANCHES),
      branches = FIVE_BRANCHES,
      viewportSize = IntSize(width = 400, height = 200),
      nodeSizes = List(nodes.size) { IntSize(width = 120, height = 28) },
      density = Density(density = 1f),
      statusBar = 0f,
      navigationBar = 0f
    )
    return state
  }
}

private const val FRAME_NANOS = 16_666_666L

/** Четыре живые ветки плюс магистраль: ровно то, из чего берётся вертикальный ход камеры. */
private val FIVE_BRANCHES = (1..4).map { index ->
  GraphBranch(
    id = GraphBranch.Id("b$index"),
    colorIndex = index,
    forkedFrom = null,
    mergedAt = null,
    status = GraphBranchStatus.Alive
  )
}
