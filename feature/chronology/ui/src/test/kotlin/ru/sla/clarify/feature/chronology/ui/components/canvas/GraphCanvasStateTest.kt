package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
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

    // Ветка исчезла: узлов стало меньше, диапазон сузился до одной точки.
    val narrowed = state.place(count = 1)

    assertEquals(
      restOf(narrowed),
      state.offset.value,
      "камера обязана сойтись с новым диапазоном в том же кадре, а не уехать телепортом позже"
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

  /**
   * Где камера обязана стоять в покое при этой раскладке.
   *
   * @param placement раскладка
   * @return положение покоя
   */
  private fun restOf(placement: GraphPlacement): Offset {
    return cameraRestOf(placement, VIEWPORT, cameraRangeOf(placement, VIEWPORT))
  }

  /**
   * Раскладывает граф из [count] одинаковых узлов на одной дорожке.
   *
   * @param count сколько узлов в графе
   * @return раскладка, которую держатель только что запомнил
   */
  private fun GraphCanvasState.place(count: Int): GraphPlacement {
    val nodes = List(count) { index ->
      GraphNode(id = GraphNode.Id("n$index"), lane = 0, gap = TimeGap.Hour)
    }
    return layout(
      nodes = nodes,
      viewportSize = VIEWPORT,
      nodeSizes = List(count) { IntSize(width = 120, height = 28) },
      density = Density(density = 1f)
    )
  }
}

private val VIEWPORT = IntSize(width = 1000, height = 600)
