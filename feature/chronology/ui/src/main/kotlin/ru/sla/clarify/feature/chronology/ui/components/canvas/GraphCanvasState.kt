package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.sla.clarify.feature.chronology.ui.entity.GraphCameraRange
import ru.sla.clarify.feature.chronology.ui.entity.GraphDebugInfo
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdge
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.GraphPanStep
import ru.sla.clarify.feature.chronology.ui.entity.GraphPlacement
import ru.sla.clarify.feature.chronology.ui.mapper.toStepWidth

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

  // Камера хранится уже зажатой. Незажатый сдвиг заводился ради оттяжки за край и затухания —
  // обоим он оказался не нужен: оттяжка держит своё состояние сама, а затуханию нужен признак
  // отказа, а не банк перерегулирования. Банк же обходился дорого, см. KDoc `panStepOf`.
  private var camera by mutableStateOf(Offset.Zero)

  // Флаг, а не сравнение сдвига с границей: значение, выведенное из сдвига, подписало бы читателя
  // на покадровые изменения.
  private var isMoved by mutableStateOf(false)

  private var viewport by mutableStateOf(IntSize.Zero)
  private var placement by mutableStateOf(GraphPlacement.Empty)

  // Обычное поле, не снапшот: за «идёт ли затухание» никто не рисует, а панель снимает по таймеру.
  private var flingJob: Job? = null

  /** Счётчики проходов Compose по полотну: обычные поля, снимаются по таймеру. */
  val telemetry = GraphCanvasTelemetry()

  /** Узлы графа в хронологическом порядке. */
  val nodes: List<GraphNode>
    get() = graphNodes

  /**
   * Сдвиг содержимого относительно экрана, уже ограниченный содержимым.
   *
   * Пока камеру не двигали, она стоит вплотную к началу истории.
   */
  val offset: State<Offset> = derivedStateOf {
    if (isMoved) camera else restingCamera()
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
      centreSpanX = placement.centreSpanX,
      nodeCount = graphNodes.size,
      edgeCount = placement.edges.size
    )
  }

  /**
   * Двигает камеру на [delta].
   *
   * Чтение [placement] и [viewport] здесь ничего не подписывает: вызывают отсюда из корутины жеста,
   * а не из фазы Compose.
   *
   * @param delta сдвиг в пикселях экрана
   * @return часть [delta], которую камера отработала; меньше запрошенного — значит упёрлись
   */
  fun pan(delta: Offset): Offset {
    stopFling()
    val step = applyPan(delta, cameraRangeOf(placement, viewport))
    telemetry.onPan(delta)
    return step.consumed
  }

  /**
   * Доигрывает инерцию после отпускания.
   *
   * Scope приходит параметром и держателю не принадлежит — как у `PredictiveBackController`:
   * корутина жеста отменяется в момент отпускания, а затухать надо уже после неё.
   *
   * Спека тоже приходит снаружи: она зависит от плотности экрана, о которой держатель не знает, а в
   * тесте подменяется на ту, что не тянет за собой Android-фреймворк.
   *
   * @param scope scope, переживающий жест
   * @param velocity скорость отпускания в пикселях в секунду
   * @param decay кривая затухания, см. `AppMotion.flingDecay`
   */
  fun fling(scope: CoroutineScope, velocity: Velocity, decay: DecayAnimationSpec<Float>) {
    stopFling()
    val direction = FlingDirection(velocity)
    // Барьер платформенный и он не про UX: кривая берёт логарифм скорости и ниже единицы отдаёт
    // NaN. Слабый бросок гасить не нужно — она сама делает его невидимым, 137 dp/s пролетают 3 px.
    if (direction.magnitude.isNaN() || direction.magnitude <= 1f) {
      return
    }
    flingJob = scope.launch { runFling(direction, decay) }
  }

  /** Обрывает инерцию: новое касание отбирает камеру у анимации. */
  fun stopFling() {
    flingJob?.cancel()
    flingJob = null
  }

  private suspend fun runFling(direction: FlingDirection, decay: DecayAnimationSpec<Float>) {
    withContext(FlingDurationScale) {
      var travelled = 0f
      AnimationState(initialValue = 0f, initialVelocity = direction.magnitude)
        .animateDecay(decay) {
          // Анимируется путь вдоль броска, а не сама камера: камеру пере-зажимает раскладка, и
          // анимация, владеющая ею напрямую, разъехалась бы с этим на первом же входящем сообщении.
          // Диапазон перечитывается каждый кадр, а не снимается на старте: пришло сообщение,
          // раскладка сузила границы — затухание узнает об этом сразу, а не доиграет мимо них.
          // Один раз за кадр: шаг и признак упора обязаны судить по одним и тем же границам.
          val range = cameraRangeOf(placement, viewport)
          val delta = direction.offsetOf(value - travelled)
          travelled = value
          telemetry.onFlingStep(applyPan(delta, range).isRejected)
          if (isCameraStuck(camera, direction.vector, range)) {
            cancelAnimation()
          }
        }
    }
  }

  private fun restingCamera(): Offset {
    return cameraRestOf(placement, viewport, cameraRangeOf(placement, viewport))
  }

  private fun applyPan(delta: Offset, range: GraphCameraRange): GraphPanStep {
    // Первое движение стартует от того места, где камера стояла в покое, а не от нуля: иначе
    // полотно прыгнуло бы к началу координат под первым же пальцем.
    val from = if (isMoved) camera else cameraRestOf(placement, viewport, range)
    val step = panStepOf(camera = from, delta = delta, range = range)
    camera = step.camera
    isMoved = true
    return step
  }

  /**
   * Раскладывает граф по результатам измерения и запоминает раскладку.
   *
   * Результат возвращается вызывающему, а не забирается потом отдельным запросом: фаза размещения
   * получает то же значение, что посчитала фаза измерения, и рассинхронизировать их нечем.
   *
   * Узлы приходят параметром, а не берутся из [nodes], и это не украшение сигнатуры. [setNodes]
   * вызывается из `SideEffect`, то есть между композицией и измерением того же кадра: прочитав
   * состояние здесь, измерение получило бы новый список узлов к measurable'ам, порождённым старой
   * композицией, — а это разные длины и индекс за границей списка. Передавая узлы снаружи, полотно
   * меряет ровно тот набор, который само же и скомпоновало.
   *
   * @param nodes узлы, из которых построено содержимое этой композиции
   * @param viewportSize размер видимой области
   * @param nodeSizes измеренные размеры узлов, в порядке [nodes]
   * @param density плотность экрана для перевода координат полотна в пиксели
   * @return раскладка графа
   */
  fun layout(
    nodes: List<GraphNode>,
    viewportSize: IntSize,
    nodeSizes: List<IntSize>,
    density: Density
  ): GraphPlacement {
    val lanes = nodes.map { it.lane }
    val geometry = GraphGeometry(topLaneOf(lanes))
    val result = with(density) {
      graphPlacementOf(
        lanes = lanes,
        gaps = nodes.map { it.gap.toStepWidth().toPx() },
        laneYs = lanes.map { geometry.laneYOf(it).toPx() },
        sizes = nodeSizes,
        edgePadding = CANVAS_PADDING.toPx()
      )
    }
    viewport = viewportSize
    placement = result
    // Диапазон только что изменился, и хранимая камера обязана сойтись с ним в этом же кадре.
    // Диапазон считается от аргументов, а не от полей выше, по той же причине, по которой узлы
    // приходят параметром: измерение не должно читать состояние, которое само же и пишет.
    //
    // Камеру читаем без подписки, и это не оптимизация, а условие работоспособности. Обычное чтение
    // здесь подписало бы **измерение** на камеру — а её пишет каждый кадр жеста и затухания, то есть
    // полотно пере-измерялось бы всю дорогу вместо того, чтобы двигать слой. Панель показывала это
    // как measure и placement, тикающие вдвое чаще кадров.
    Snapshot.withoutReadObservation {
      if (isMoved) {
        val clamped = panStepOf(camera, Offset.Zero, cameraRangeOf(result, viewportSize)).camera
        if (clamped != camera) {
          camera = clamped
        }
      }
    }
    telemetry.onMeasure()
    return result
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

// Инерция — физика жеста, а не переход, поэтому системный множитель длительности анимаций к ней не
// применяется. Без этого «Animator duration scale» из опций разработчика менял бы пролёт броска, а
// при значении «Off» весь путь приезжал бы одной дельтой. Платформа оборачивает свой fling так же.
private val FlingDurationScale = object : MotionDurationScale {
  override val scaleFactor: Float
    get() = 1f
}
