package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animate
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
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.sla.clarify.feature.chronology.ui.entity.GraphAnchor
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranch
import ru.sla.clarify.feature.chronology.ui.entity.GraphCameraRange
import ru.sla.clarify.feature.chronology.ui.entity.GraphDebugInfo
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdge
import ru.sla.clarify.feature.chronology.ui.entity.GraphLaneMark
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.GraphPanStep
import ru.sla.clarify.feature.chronology.ui.entity.GraphPlacement
import ru.sla.clarify.feature.chronology.ui.entity.GraphViewportSpan
import ru.sla.clarify.feature.chronology.ui.entity.GraphZoomStep
import ru.sla.clarify.feature.chronology.ui.mapper.toStepWidth

/**
 * Состояние полотна, живущее весь срок экрана.
 *
 * Новый набор узлов не пересоздаёт состояние, а подменяется в нём: иначе каждое входящее сообщение
 * сбрасывало бы камеру в исходную позицию и отменяло бы жест под пальцем.
 *
 * @param nodes узлы в хронологическом порядке
 * @param branches ветки графа, кроме магистрали, в порядке ветвления
 * @return состояние, живущее до выхода с экрана
 */
@Composable
internal fun rememberGraphCanvasState(
  nodes: List<GraphNode>,
  branches: List<GraphBranch>
): GraphCanvasState {
  val state = remember { GraphCanvasState() }
  // SideEffect, а не запись в теле: отброшенная композиция не должна была подменять узлы.
  SideEffect { state.setNodes(nodes, branches) }
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

  private var graphBranches by mutableStateOf(emptyList<GraphBranch>())

  // Камера хранится уже зажатой. Незажатый сдвиг заводился ради оттяжки за край и затухания —
  // обоим он оказался не нужен: оттяжка держит своё состояние сама, а затуханию нужен признак
  // отказа, а не банк перерегулирования. Банк же обходился дорого, см. KDoc `panStepOf`.
  private var camera by mutableStateOf(Offset.Zero)

  // Флаг, а не сравнение сдвига с границей: значение, выведенное из сдвига, подписало бы читателя
  // на покадровые изменения.
  private var isMoved by mutableStateOf(false)

  // Масштаб держится отдельным числом, а не парой с камерой, и это не экономия строк. Сдвиг
  // зажимается диапазоном содержимого, масштаб — своими пределами, и порядок между ними
  // существенен; один тип на обе величины приглашает «зажать камеру» одним вызовом и не заметить,
  // что половина осталась незажатой.
  private var cameraScale by mutableStateOf(1f)

  // Камера фона: живёт своей жизнью, границ не знает вовсе — узор бесконечен, зажимать его нечем.
  private var backdrop by mutableStateOf(Offset.Zero)

  private var viewport by mutableStateOf(IntSize.Zero)
  private var placement by mutableStateOf(GraphPlacement.Empty)

  // Засечки считаются там же, где раскладка, и по тем же узлам. Выводить их из `graphNodes` и
  // `placement` по требованию нельзя: узлы подменяются из `SideEffect`, раскладка приходит из
  // измерения, и на кадре подмены длины этих списков расходятся — а засечке нужен номер дорожки из
  // одного списка и координата из другого.
  private var marks by mutableStateOf(emptyList<GraphLaneMark>())

  // Обычное поле, не снапшот: за «идёт ли затухание» никто не рисует, а панель снимает по таймеру.
  //
  // Job один на всё, что двигает камеру без пальца, — и на затухание, и на перелёт по кнопке. Свой
  // job у каждого означал бы, что каждую точку, где палец отбирает камеру — касание, протяжка,
  // пинч, скраб мини-карты, — придётся дублировать; одну из них забыли бы, и перелёт продолжал бы
  // ехать под пальцем.
  private var motionJob: Job? = null

  /** Счётчики проходов Compose по полотну: обычные поля, снимаются по таймеру. */
  val telemetry = GraphCanvasTelemetry()

  /** Узлы графа в хронологическом порядке. */
  val nodes: List<GraphNode>
    get() = graphNodes

  /** Ветки графа, кроме магистрали, в порядке ветвления. */
  val branches: List<GraphBranch>
    get() = graphBranches

  /**
   * Сдвиг содержимого относительно экрана, уже ограниченный содержимым.
   *
   * Пока камеру не двигали, она стоит вплотную к началу истории.
   */
  val offset: State<Offset> = derivedStateOf {
    if (isMoved) camera else restingCamera()
  }

  /**
   * Масштаб содержимого: `экран = полотно · scale + камера`.
   *
   * Отдаётся наружу отдельным [State] не ради полноты API: уровень детализации выводится из
   * масштаба порогами с гистерезисом, и выводить его обязан тот, кто рисует, — прочитанное здесь
   * значение подписало бы на покадровые изменения зума весь экран.
   */
  val scale: State<Float> = derivedStateOf { cameraScale }

  /**
   * Сдвиг фона: своя камера, отстающая от графа на [BACKDROP_PARALLAX].
   *
   * Отдельная величина, а не камера графа, умноженная на коэффициент. Умножение верно, пока масштаб
   * единичный, и разваливается на первом же пинче: камера графа при зуме уезжает на
   * `(focus − camera) · (1 − zoom)`, и фон, привязанный к ней множителем, пролетает треть этого
   * пути — вдали от начала истории это тысячи пикселей за жест.
   *
   * Двигается на **потреблённое**, а не на запрошенное: у стенки граф стоит, и фон обязан стоять
   * вместе с ним, иначе на упоре узор продолжает ползти под неподвижными плашками.
   */
  val backdropOffset: State<Offset> = derivedStateOf { backdrop }

  /** Масштаб фона, см. [backdropScaleOf]. */
  val backdropScale: State<Float> = derivedStateOf { backdropScaleOf(cameraScale) }

  /** Связи между соседними узлами каждой ветки, в координатах полотна. */
  val edges: State<List<GraphEdge>> = derivedStateOf { placement.edges }

  /**
   * Какая доля содержимого по времени видна сейчас.
   *
   * Величина о камере, а не о том, кто её показывает: держатель не знает ни про мини-карту, ни про
   * полосу, на которой это рисуется, — ровно как не знает про отладочную панель.
   *
   * Читать в фазе рисования: пересчитывается на каждом кадре движения.
   */
  val viewportSpan: State<GraphViewportSpan> = derivedStateOf {
    viewportSpanOf(
      camera = offset.value,
      scale = cameraScale,
      centreSpan = placement.centreSpanX,
      viewport = viewport
    )
  }

  /**
   * Начала дорожек в долях содержимого.
   *
   * Меняются только с раскладкой, поэтому чтение в фазе рисования не стоит ничего.
   */
  val laneMarks: State<List<GraphLaneMark>> = derivedStateOf { marks }

  /**
   * Узел, ближайший к центру экрана по времени: то, чем подписывается пузырь мини-карты.
   *
   * Отдаётся идентификатором, а не индексом: индекс — деталь раскладки, и снаружи по нему ничего не
   * найти. `derivedStateOf` тут не украшение — он гасит покадровые изменения камеры до редких смен
   * узла, поэтому подпись рекомпонуется в разы реже, чем движется картинка.
   *
   * Список узлов и раскладка расходятся не более чем на кадр — узлы подменяются из `SideEffect`, а
   * раскладка считается при измерении, — поэтому индекс берётся безопасно: на этом кадре подпись
   * пузыря либо отстанет на один узел, либо не покажется вовсе, и оба исхода дешевле падения.
   */
  val centralNode: State<GraphNode.Id?> = derivedStateOf {
    val index = nearestCentreIndexOf(
      centres = placement.centres,
      x = centreXOf(camera = offset.value, scale = cameraScale, viewport = viewport)
    )
    graphNodes.getOrNull(index)?.id
  }

  /** Снимок камеры и последней раскладки для отладочной панели. */
  val debugInfo: State<GraphDebugInfo> = derivedStateOf {
    GraphDebugInfo(
      viewportWidth = viewport.width,
      viewportHeight = viewport.height,
      contentBounds = placement.bounds,
      camera = offset.value,
      scale = cameraScale,
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
    stopMotion()
    val step = applyPan(delta, cameraRangeOf(placement, viewport, cameraScale))
    telemetry.onPan(delta)
    return step.consumed
  }

  /**
   * Уводит камеру в [fraction] её хода по времени.
   *
   * Скраб мини-карты идёт через [pan], а не собственной записью камеры, и это не экономия строк.
   * Оттуда даром достаются четыре вещи, каждая из которых стоила отдельной итерации: обрыв инерции
   * касанием, кламп на записи, параллакс фона на **потреблённое** и признак «камеру трогали».
   * Второй путь записи камеры разошёлся бы с этим при первой правке любой из них.
   *
   * Вертикаль скраб не трогает: полоса высотой 48 dp на четырнадцать дорожек даёт 3.4 dp на
   * дорожку — различить их там нечем, и рамка, честная по обеим осям, была бы высотой в три
   * пикселя. За вертикаль отвечает панорамирование, где она и работает.
   *
   * @param fraction доля хода камеры по времени, от нуля до единицы
   */
  fun scrubTo(fraction: Float) {
    if (placement.isEmpty) {
      return
    }
    val range = cameraRangeOf(placement, viewport, cameraScale)
    val target = scrubbedCameraXOf(
      position = fraction,
      scale = cameraScale,
      centreSpan = placement.centreSpanX,
      viewport = viewport,
      range = range
    )
    pan(Offset(x = target - cameraAt(range).x, y = 0f))
  }

  /**
   * Меняет масштаб, удерживая точку под [focus] на месте.
   *
   * Порядок внутри — не деталь реализации, а само решение. Сначала зажимается масштаб, потом под
   * зажатый пересчитывается камера, и только потом камера зажимается диапазоном **нового**
   * масштаба. Посчитав камеру под запрошенный масштаб, полотно уезжало бы из-под пальцев каждый
   * раз, когда зум упирается в границу.
   *
   * Пределы приходят литералом, а не константой: значение читается ровно здесь, а рядом с числом
   * видно, к чему оно применено.
   *
   * @param focus точка экрана, которую жест держит на месте
   * @param change множитель масштаба, пришедший от жеста
   * @return новый масштаб, признак упора и сдвиг, получившийся под ним
   */
  fun zoom(focus: Offset, change: Float): GraphZoomStep {
    stopMotion()
    val previous = cameraScale
    val updated = scaleStepOf(
      scale = previous,
      change = change,
      // §11.2 брифа: 0.4× … 2.5×.
      range = 0.4f..2.5f
    )
    val zoomed = zoomedCameraOf(
      camera = cameraAt(cameraRangeOf(placement, viewport, previous)),
      focus = focus,
      from = previous,
      to = updated
    )
    // Фон масштабируется вокруг того же фокуса, но своим масштабом: так его точка под пальцами
    // остаётся под пальцами, а глубина сохраняется — узор укрупняется втрое медленнее графа.
    backdrop = zoomedCameraOf(
      camera = backdrop,
      focus = focus,
      from = backdropScaleOf(previous),
      to = backdropScaleOf(updated)
    )
    cameraScale = updated
    val step = panStepOf(
      camera = zoomed,
      delta = Offset.Zero,
      range = cameraRangeOf(placement, viewport, updated)
    )
    camera = step.camera
    isMoved = true
    telemetry.onZoom()
    return GraphZoomStep(
      requestedScale = previous * change,
      scale = updated,
      camera = step.camera
    )
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
    stopMotion()
    // Записывается до отбраковки: «жест не отдал скорости» и «скорости не хватило на бросок» — разные
    // неисправности, а по картинке они выглядят одинаково.
    telemetry.onRelease(velocity)
    val direction = FlingDirection(velocity)
    // Барьер платформенный и он не про UX: кривая берёт логарифм скорости и ниже единицы отдаёт
    // NaN. Слабый бросок гасить не нужно — она сама делает его невидимым, 137 dp/s пролетают 3 px.
    if (direction.magnitude.isNaN() || direction.magnitude <= 1f) {
      return
    }
    motionJob = scope.launch { runFling(direction, decay) }
  }

  /**
   * Уводит камеру к [anchor] перелётом.
   *
   * Перелёт **всегда анимирован**, телепорта нет: камера, прыгнувшая через всю историю, не
   * оставляет зрителю ничего, из чего понять, куда он попал (§11.1 брифа).
   *
   * Вместе с камерой возвращается к единице и масштаб — но только если было приближено, см.
   * [flightScaleOf]. Обе величины идут по одной кривой и заканчиваются одновременно: разъехавшись,
   * они дали бы прилёт в нужное место с чужим масштабом, а потом отдельный доводочный рывок.
   *
   * В отличие от инерции, системный множитель длительности анимаций здесь действует. Инерция — это
   * физика жеста, и она обязана вести себя одинаково при любом значении «Animator duration scale»;
   * перелёт по кнопке — обычный переход, и пользователь, отключивший анимации, вправе получить его
   * мгновенным.
   *
   * Спека приходит снаружи по той же причине, что и у инерции: она берётся из темы, о которой
   * держатель не знает, а тест подменяет её на ту, что не тянет за собой Android-фреймворк.
   *
   * @param scope scope, переживающий композицию кнопки
   * @param spec кривая перелёта, см. `AppMotion.largeTween`
   * @param anchor куда лететь
   */
  fun flyTo(scope: CoroutineScope, spec: AnimationSpec<Float>, anchor: GraphAnchor) {
    stopMotion()
    if (placement.isEmpty) {
      return
    }
    motionJob = scope.launch { runFlight(spec, anchor) }
  }

  /** Обрывает движение камеры: новое касание отбирает её и у затухания, и у перелёта. */
  fun stopMotion() {
    motionJob?.cancel()
    motionJob = null
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
          // Масштаб читается тут же каждый кадр по той же причине, что и диапазон: пинч оборвал бы
          // затухание касанием, но раскладка меняет границы и без пальца на экране.
          val range = cameraRangeOf(placement, viewport, cameraScale)
          val delta = direction.offsetOf(value - travelled)
          travelled = value
          telemetry.onFlingStep(applyPan(delta, range).isRejected)
          if (isCameraStuck(camera, direction.vector, range)) {
            cancelAnimation()
          }
        }
    }
  }

  /**
   * Ведёт камеру к якорю по кривой [spec].
   *
   * Анимируется **доля пути**, а не сама камера, ровно по той же причине, что и у затухания: камеру
   * пере-зажимает раскладка, и анимация, владеющая ею напрямую, разъехалась бы с этим на первом же
   * входящем сообщении. Цель и диапазон перечитываются каждый кадр — пришло сообщение, границы
   * поехали, и перелёт доводит камеру туда, где якорь оказался **сейчас**, а не туда, где он был на
   * старте.
   *
   * Шаг кладётся через [applyPan], а не записью камеры: оттуда берутся кламп на записи и параллакс
   * фона на потреблённое. Фон обязан лететь вместе с графом, иначе узор во время перелёта стоит.
   */
  private suspend fun runFlight(spec: AnimationSpec<Float>, anchor: GraphAnchor) {
    val startScale = cameraScale
    val targetScale = flightScaleOf(startScale)
    val start = cameraAt(cameraRangeOf(placement, viewport, startScale))
    animate(initialValue = 0f, targetValue = 1f, animationSpec = spec) { fraction, _ ->
      // Масштаб ложится первым, ровно как в пинче: он меняет и границы камеры, и то, куда попадёт
      // якорь, — а цель перелёта обязана считаться под масштаб **этого** кадра, иначе последний
      // кадр придёт с промахом на разницу масштабов.
      cameraScale = startScale + (targetScale - startScale) * fraction
      val range = cameraRangeOf(placement, viewport, cameraScale)
      val target = cameraAimedAt(
        point = anchorPointOf(anchor),
        viewport = viewport,
        range = range,
        scale = cameraScale
      )
      // Шаг считается от того, где камера стоит **фактически**, а не от хранимого поля: пока её не
      // двигали, поле лежит в нуле, а показывается покой — и перелёт с нетронутой камеры уехал бы
      // на величину покоя первым же кадром.
      val step = applyPan(lerp(start, target, fraction) - cameraAt(range), range)
      // Тем же счётчиком, что и затухание: панель заведена ловить движение без пальца и зависание,
      // а не различать, чем именно оно вызвано. Понадобится различать — счётчик заводится дёшево.
      telemetry.onFlingStep(step.isRejected)
    }
  }

  /**
   * Точка полотна, к которой ведёт якорь.
   *
   * Крайние плашки берутся первой и последней по списку, а не поиском минимума: `leftOffsetsOf`
   * накапливает смещения по порядку модели, поэтому координата X монотонна по индексу **по
   * построению раскладки**, а не по свойствам данных. Перебор здесь дал бы тот же ответ дороже.
   *
   * @param anchor куда лететь
   * @return центр плашки в координатах полотна
   */
  private fun anchorPointOf(anchor: GraphAnchor): Offset {
    return when (anchor) {
      GraphAnchor.Start -> placement.centres.first()
      GraphAnchor.Front -> placement.centres.last()
    }
  }

  private fun restingCamera(): Offset {
    return cameraRestOf(
      placement = placement,
      viewport = viewport,
      range = cameraRangeOf(placement, viewport, cameraScale),
      scale = cameraScale
    )
  }

  /**
   * Откуда стартует движение камеры.
   *
   * Первое движение стартует от того места, где камера стояла в покое, а не от нуля: иначе полотно
   * прыгнуло бы к началу координат под первым же пальцем. Определение одно на протяжку и на пинч —
   * второе разъехалось бы с этим при первой правке покоя.
   *
   * @param range где камере разрешено быть при текущем масштабе
   * @return сдвиг, от которого считается шаг
   */
  private fun cameraAt(range: GraphCameraRange): Offset {
    return if (isMoved) {
      camera
    } else {
      cameraRestOf(placement = placement, viewport = viewport, range = range, scale = cameraScale)
    }
  }

  private fun applyPan(delta: Offset, range: GraphCameraRange): GraphPanStep {
    val step = panStepOf(camera = cameraAt(range), delta = delta, range = range)
    camera = step.camera
    // Фон двигает и жест, и затухание — оба приходят сюда, и второго места для этого правила нет.
    backdrop += step.consumed * BACKDROP_PARALLAX
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
   * @param branches ветки той же композиции: из них считаются дорожки
   * @param viewportSize размер видимой области
   * @param nodeSizes измеренные размеры узлов, в порядке [nodes]
   * @param density плотность экрана для перевода координат полотна в пиксели
   * @param statusBar высота строки состояния в пикселях
   * @param navigationBar высота навигационной полосы в пикселях
   * @return раскладка графа
   */
  fun layout(
    nodes: List<GraphNode>,
    branches: List<GraphBranch>,
    viewportSize: IntSize,
    nodeSizes: List<IntSize>,
    density: Density,
    statusBar: Float,
    navigationBar: Float
  ): GraphPlacement {
    // Дорожки считаются здесь, из параметров, а не читаются готовыми из состояния: узлы и ветки
    // подменяются из `SideEffect`, то есть между композицией и измерением того же кадра, и раскраска
    // по снапшот-полю разошлась бы с measurable'ами от старой композиции.
    val branchIds = nodes.map { it.branchId }
    val lanes = graphLanesOf(nodes, branches)
    val colorIndexes = graphAccentsOf(nodes, branches).map { it.colorIndex }
    val geometry = GraphGeometry(topLaneOf(lanes))
    val result = with(density) {
      graphPlacementOf(
        branchIds = branchIds,
        lanes = lanes,
        gaps = nodes.map { it.gap.toStepWidth().toPx() },
        laneYs = lanes.map { geometry.laneYOf(it).toPx() },
        sizes = nodeSizes,
        margins = canvasMarginsOf(
          base = CANVAS_PADDING.toPx(),
          statusBar = statusBar,
          navigationBar = navigationBar
        )
      )
    }
    viewport = viewportSize
    placement = result
    marks = laneMarksOf(
      branchIds = branchIds,
      lanes = lanes,
      colorIndexes = colorIndexes,
      centres = result.centres,
      centreSpan = result.centreSpanX
    )
    // Диапазон только что изменился, и хранимая камера обязана сойтись с ним в этом же кадре.
    // Диапазон считается от аргументов, а не от полей выше, по той же причине, по которой узлы
    // приходят параметром: измерение не должно читать состояние, которое само же и пишет.
    //
    // Камеру и масштаб читаем без подписки, и это не оптимизация, а условие работоспособности.
    // Обычное чтение здесь подписало бы **измерение** на камеру — а её пишет каждый кадр жеста и
    // затухания, то есть полотно пере-измерялось бы всю дорогу вместо того, чтобы двигать слой.
    // Панель показывала это как measure и placement, тикающие вдвое чаще кадров. С масштабом ровно
    // так же: его пишет каждый кадр пинча.
    Snapshot.withoutReadObservation {
      if (isMoved) {
        val range = cameraRangeOf(result, viewportSize, cameraScale)
        val clamped = panStepOf(camera, Offset.Zero, range).camera
        if (clamped != camera) {
          camera = clamped
        }
      }
    }
    telemetry.onMeasure()
    return result
  }

  /**
   * Подменяет набор узлов и веток.
   *
   * Оба списка подменяются вместе и никогда порознь: занятость дорожек выводится из индексов узлов
   * по идентификаторам развилки и слияния, и список веток, разъехавшийся с узлами хотя бы на кадр,
   * дал бы раскраску по чужим индексам — **молча**, потому что `check` в раскладке сверяет длины
   * поузловых списков, а они остались бы равными.
   *
   * @param nodes узлы в хронологическом порядке
   * @param branches ветки графа, кроме магистрали, в порядке ветвления
   */
  fun setNodes(nodes: List<GraphNode>, branches: List<GraphBranch>) {
    graphNodes = nodes
    graphBranches = branches
  }
}

// Инерция — физика жеста, а не переход, поэтому системный множитель длительности анимаций к ней не
// применяется. Без этого «Animator duration scale» из опций разработчика менял бы пролёт броска, а
// при значении «Off» весь путь приезжал бы одной дельтой. Платформа оборачивает свой fling так же.
private val FlingDurationScale = object : MotionDurationScale {
  override val scaleFactor: Float
    get() = 1f
}
