package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import ru.sla.clarify.feature.chronology.ui.entity.GraphDebugInfo
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdge
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.GraphPlacement

/**
 * Состояние полотна, живущее весь срок экрана.
 *
 * Новый набор узлов не пересоздаёт состояние, а подменяется в нём: иначе каждое входящее сообщение
 * сбрасывало бы камеру в исходную позицию и отменяло бы жест под пальцем.
 *
 * @param nodes узлы в хронологическом порядке
 * @return состояние, живущее до выхода с экрана
 */
@Composable
internal fun rememberGraphCanvasState(nodes: List<GraphNode>): GraphCanvasState {
  val state = remember { GraphCanvasState() }
  // SideEffect, а не запись в теле: отброшенная композиция не должна была подменять узлы.
  SideEffect { state.setNodes(nodes) }
  return state
}

/**
 * Камера полотна и результат его последней раскладки.
 *
 * Разделение обязанностей: [graphPlacementOf] считает, где узлы стоят на полотне, состояние держит
 * камеру и результат раскладки, композабл не считает ничего.
 *
 * Наружу отдаются [State], а не готовые значения. Это не оформление: значение заставило бы читателя
 * подписаться там, где он его получил, а `State` можно передать дальше, не читая, и прочитать ровно
 * в той фазе, которой оно нужно. Промах на один уровень — чтение в теле полотна вместо фазы
 * рисования — уже приводил к бесконечному циклу измерения.
 *
 * [offset] и [edges] читать внутри `graphicsLayer` и `drawBehind`, [debugInfo] — в листовой панели.
 * Тогда кадр панорамирования обновляет только свойства слоя: ни рекомпозиции, ни повторного
 * измерения.
 */
@Stable
internal class GraphCanvasState {

  private var graphNodes by mutableStateOf(emptyList<GraphNode>())

  // Сдвиг камеры без клампа: кламп накладывается на чтении. Хранить уже ограниченное значение
  // означало бы потерять то, что понадобится для оттяжки за край и для затухания инерции.
  private var rawOffsetX by mutableFloatStateOf(0f)
  private var rawOffsetY by mutableFloatStateOf(0f)

  // Флаг, а не сравнение сдвига с границей: значение, выведенное из сдвига, подписало бы читателя
  // на покадровые изменения.
  private var isMoved by mutableStateOf(false)

  private var viewport by mutableStateOf(IntSize.Zero)
  private var placement by mutableStateOf(GraphPlacement.Empty)

  private var telemetryOn by mutableStateOf(false)
  private var dragCount by mutableIntStateOf(0)
  private var lastDrag by mutableStateOf(Offset.Zero)

  /** Узлы графа в хронологическом порядке. */
  val nodes: List<GraphNode>
    get() = graphNodes

  /**
   * Сдвиг содержимого относительно экрана, уже ограниченный содержимым.
   *
   * Пока камеру не двигали, она стоит вплотную к началу истории.
   */
  val offset: State<Offset> = derivedStateOf {
    val bounds = placement.bounds
    // По времени камера ходит от «первый узел в центре» до «последний узел в центре»; по дорожкам —
    // от края до края, потому что вертикаль надо видеть целиком, а не наводить на неё.
    val rangeX = if (placement.isEmpty) {
      0f..0f
    } else {
      timelinePanRangeOf(placement.centreSpanX, viewport.width.toFloat())
    }
    val rangeY = panRangeOf(bounds.top, bounds.bottom, viewport.height.toFloat())
    Offset(
      x = if (isMoved) rawOffsetX.coerceIn(rangeX) else rangeX.endInclusive,
      y = if (isMoved) rawOffsetY.coerceIn(rangeY) else rangeY.endInclusive
    )
  }

  /** Связи между соседними узлами каждой дорожки, в координатах полотна. */
  val edges: State<List<GraphEdge>> = derivedStateOf { placement.edges }

  /** Снимок камеры и последней раскладки для отладочной панели. */
  val debugInfo: State<GraphDebugInfo> = derivedStateOf {
    GraphDebugInfo(
      viewportWidth = viewport.width,
      viewportHeight = viewport.height,
      contentBounds = placement.bounds,
      camera = offset.value,
      isCameraMoved = isMoved,
      nodeCount = graphNodes.size,
      edgeCount = placement.edges.size,
      dragCount = dragCount,
      lastDrag = lastDrag
    )
  }

  /**
   * Двигает камеру на [delta].
   *
   * @param delta сдвиг в пикселях экрана
   */
  fun pan(delta: Offset) {
    if (!isMoved) {
      val resting = offset.value
      rawOffsetX = resting.x
      rawOffsetY = resting.y
      isMoved = true
    }
    rawOffsetX += delta.x
    rawOffsetY += delta.y
    if (telemetryOn) {
      dragCount++
      lastDrag = delta
    }
  }

  /**
   * Раскладывает граф по результатам измерения и запоминает раскладку.
   *
   * Результат возвращается вызывающему, а не забирается потом отдельным запросом: фаза размещения
   * получает то же значение, что посчитала фаза измерения, и рассинхронизировать их нечем.
   *
   * @param viewportSize размер видимой области
   * @param nodeSizes измеренные размеры узлов, в порядке [nodes]
   * @param density плотность экрана для перевода координат полотна в пиксели
   * @return раскладка графа
   */
  fun layout(viewportSize: IntSize, nodeSizes: List<IntSize>, density: Density): GraphPlacement {
    val lanes = graphNodes.map { it.lane }
    val geometry = GraphGeometry(topLaneOf(lanes))
    val result = with(density) {
      graphPlacementOf(
        lanes = lanes,
        gaps = graphNodes.map { stepWidthOf(it.gap).toPx() },
        laneYs = lanes.map { geometry.laneYOf(it).toPx() },
        sizes = nodeSizes
      )
    }
    viewport = viewportSize
    placement = result
    return result
  }

  /**
   * Включает сбор телеметрии для отладочной панели.
   *
   * @param enabled собирать ли покадровые счётчики жеста
   */
  fun setTelemetryEnabled(enabled: Boolean) {
    telemetryOn = enabled
  }

  /**
   * Подменяет набор узлов.
   *
   * @param nodes узлы в хронологическом порядке
   */
  fun setNodes(nodes: List<GraphNode>) {
    graphNodes = nodes
  }
}
