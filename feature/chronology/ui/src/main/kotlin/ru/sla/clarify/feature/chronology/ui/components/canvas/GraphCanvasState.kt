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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.sla.atlas.entity.BasicNode
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.Graph
import ru.sla.atlas.layout.CanvasMargins
import ru.sla.atlas.layout.Edge
import ru.sla.atlas.layout.LaneGeometry
import ru.sla.atlas.layout.Lanes
import ru.sla.atlas.layout.Placement
import ru.sla.atlas.layout.edgesOf
import ru.sla.atlas.layout.nearestCentreIndexOf
import ru.sla.atlas.layout.placementOf
import ru.sla.atlas.layout.screenRectOf
import ru.sla.atlas.layout.topLaneOf
import ru.sla.clarify.feature.chronology.ui.entity.GraphAnchor
import ru.sla.clarify.feature.chronology.ui.entity.GraphCameraPose
import ru.sla.clarify.feature.chronology.ui.entity.GraphCameraRange
import ru.sla.clarify.feature.chronology.ui.entity.GraphDebugInfo
import ru.sla.clarify.feature.chronology.ui.entity.GraphLaneMark
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevelBand
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevelSwitch
import ru.sla.clarify.feature.chronology.ui.entity.GraphPanStep
import ru.sla.clarify.feature.chronology.ui.entity.GraphViewportSpan
import ru.sla.clarify.feature.chronology.ui.entity.GraphZoomStep
import ru.sla.clarify.feature.chronology.ui.entity.Node
import ru.sla.clarify.feature.chronology.ui.mapper.toLaneStep
import ru.sla.clarify.feature.chronology.ui.mapper.toStepWidth

/**
 * Состояние полотна, живущее весь срок экрана.
 *
 * Новый набор узлов не пересоздаёт состояние, а подменяется в нём: иначе каждое входящее сообщение
 * сбрасывало бы камеру в исходную позицию и отменяло бы жест под пальцем.
 *
 * @param graph граф: порядок узлов и состав веток
 * @return состояние, живущее до выхода с экрана
 */
@Composable
internal fun rememberGraphCanvasState(graph: Graph<Node>): GraphCanvasState {
  val state = remember { GraphCanvasState() }
  // SideEffect, а не запись в теле: отброшенная композиция не должна была подменять граф.
  SideEffect { state.setGraph(graph) }
  return state
}

/**
 * Камера полотна и результат его последней раскладки.
 *
 * Разделение обязанностей: [placementOf] считает, где узлы стоят на полотне, состояние держит
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

  private var currentGraph: Graph<Node> by mutableStateOf(Graph.Empty)

  private var graphEdges by mutableStateOf(emptyList<Edge>())

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
  private var placement by mutableStateOf(Placement.Empty)

  // Уровень детализации — снапшотное: его читает композиция, чтобы выбрать представление узла.
  // Измерение получает уровень **параметром**, ровно как узлы, и по той же причине: прочитав его
  // здесь, оно померило бы зазоры одного уровня по measurable'ам, порождённым другим.
  private var graphLevel by mutableStateOf(GraphLevel.Episodes)

  // Масштаб, на котором полотно ушло с прошлого уровня. Снапшотное: из него в фазе рисования
  // считается встречный масштаб уходящего представления, см. `counterScaleOf`.
  private var levelExitScale by mutableFloatStateOf(1f)

  // Идёт ли смена уровня прямо сейчас. Обычное поле: за ним никто не рисует — его ставит жест, а
  // снимает полотно, когда кроссфейд доигран.
  //
  // Существует затем, чтобы переход нельзя было начать поверх незаконченного. Порог сравнивается с
  // масштабом, запрошенным **одним событием жеста**, а посадка отходит от края полосы на восемь
  // процентов — при девяноста событиях в секунду быстрый щипок проходит этот запас за пару событий,
  // и уровень начинает мигать от дрожания пальца. Мигание стоит дорого вдвойне: пока переход жив,
  // полотно держит на каждый узел по два представления, а каждый новый отсчитывает свои двести
  // пятьдесят миллисекунд заново.
  //
  // Запас в полосе при этом не увеличен, и это осознанно: он и есть то, чем стык уровней держится
  // бесшовным. Посадка дальше от края означала бы скачок охвата ровно на кадре перехода.
  private var isLevelSettling = false

  // Заявка на посадку после смены уровня. Обычное поле, как `motionJob`: за ней никто не рисует —
  // её кладёт жест и разбирает ближайшее измерение. Раньше разобрать нельзя: охват нового уровня
  // выводится из его раскладки, а раскладку считает измерение.
  private var pendingSwitch: GraphLevelSwitch? = null

  // Заявка «вписать всё», отложенная по той же причине и ровно в тех случаях, когда вписывание
  // меняет уровень: без смены уровня новой раскладки не будет, и заявке негде разрешиться.
  private var pendingFit = false

  // Куда возвращает повторный двойной тап; `null` — возвращаться некуда.
  private var restorePose: GraphCameraPose? = null

  // Засечки считаются там же, где раскладка, и по тем же узлам. Выводить их из `currentGraph` и
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

  /** Граф, который полотно сейчас показывает: порядок узлов и состав веток. */
  val graph: Graph<Node>
    get() = currentGraph

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
   * Отдаётся наружу отдельным [State] не ради полноты API: значение, прочитанное в композиции,
   * подписало бы на покадровые изменения зума весь экран, а `State` читается в фазе слоя.
   *
   * Прежде здесь стояло, что уровень детализации выводит из масштаба тот, кто рисует. С решением §5
   * брифа — «уровень меняет **раскладку**, а не только вид узлов» — это перестало быть верным:
   * зазоры считает измерение, и уровень, выведенный в отрисовке, пришёл бы к нему кадром позже.
   * Уровень живёт в [level], меняется вместе с масштабом и попадает в измерение параметром.
   */
  val scale: State<Float> = derivedStateOf { cameraScale }

  /**
   * Уровень детализации (§5 брифа): он выбирает и представление узла, и зазоры раскладки.
   *
   * Меняется редко — только когда масштаб выходит за полосу уровня, — поэтому подписка композиции на
   * него не стоит ничего: узлы всё равно обязаны перекомпоноваться, у них меняется представление.
   */
  val level: State<GraphLevel> = derivedStateOf { graphLevel }

  /**
   * Масштаб, на котором полотно ушло с прошлого уровня.
   *
   * Из него в фазе рисования считается встречный масштаб уходящего представления: слой камеры к
   * моменту кроссфейда уже приземлился на новый масштаб, и плашка внутри него растянулась бы.
   */
  val exitScale: State<Float> = derivedStateOf { levelExitScale }

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

  /** Рёбра графа в координатах полотна: горизонтали дорожек, уходы, возвраты и хвосты. */
  val edges: State<List<Edge>> = derivedStateOf { graphEdges }

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
   * Отдаётся самим узлом, а не индексом: индекс — деталь раскладки, и снаружи по нему ничего не
   * найти, а искать узел по идентификатору вызывающему пришлось бы в том же списке, из которого он
   * здесь и взят. `derivedStateOf` тут не украшение — он гасит покадровые изменения камеры до
   * редких смен узла, поэтому подпись рекомпонуется в разы реже, чем движется картинка.
   *
   * Список узлов и раскладка расходятся не более чем на кадр — узлы подменяются из `SideEffect`, а
   * раскладка считается при измерении, — поэтому индекс берётся безопасно: на этом кадре подпись
   * пузыря либо отстанет на один узел, либо не покажется вовсе, и оба исхода дешевле падения.
   */
  val centralNode: State<Node?> = derivedStateOf {
    val index = nearestCentreIndexOf(
      centres = placement.centres,
      x = centreXOf(camera = offset.value, scale = cameraScale, viewport = viewport)
    )
    currentGraph.nodes.getOrNull(index)
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
      level = graphLevel,
      levelBand = levelBandOf(placement, viewport),
      centreSpanX = placement.centreSpanX,
      nodeCount = currentGraph.nodes.size,
      edgeCount = graphEdges.size
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
   * Пределы — не литерал и не константа, а полоса **уровня**: масштаб, вышедший за неё, означает не
   * упор, а переход на соседний уровень детализации (§5 брифа). Тогда ни масштаб, ни камера здесь не
   * трогаются вовсе — их поставит ближайшее измерение, когда посчитает новую раскладку.
   *
   * @param focus точка экрана, которую жест держит на месте
   * @param change множитель масштаба, пришедший от жеста
   * @return новый масштаб, признак упора и сдвиг, получившийся под ним
   */
  fun zoom(focus: Offset, change: Float): GraphZoomStep {
    stopMotion()
    // Щипок — то же движение камеры, что и протяжка, и позу для возврата он снимает по той же
    // причине, см. `applyPan`.
    restorePose = null
    val previous = cameraScale
    val band = levelBandOf(placement, viewport)
    val requested = previous * change
    val next = graphLevelSwitchOf(level = graphLevel, requestedScale = requested, band = band)
    // Пока прошлый переход не доигран, полоса уровня работает обычной стенкой: щипок упирается в её
    // край и остаётся на месте. Иначе тот же щипок увёл бы полотно обратно, не дав первому переходу
    // ни закончиться, ни показаться.
    if (next != null && !placement.isEmpty && !isLevelSettling) {
      switchLevel(to = next, focus = focus)
      telemetry.onZoom()
      // Шаг отдаётся неотвергнутым, хотя запрошенного масштаба на этом уровне и не бывает: упора
      // здесь нет — есть переход. `isRejected` заведён под тактильную отдачу на пределе зума (§11.3),
      // и объявив переход упором, полотно позвало бы её вдобавок к отклику самого перехода.
      return GraphZoomStep(
        requestedScale = previous,
        scale = previous,
        camera = cameraAt(cameraRangeOf(placement, viewport, previous))
      )
    }
    val updated = scaleStepOf(
      scale = previous,
      change = change,
      range = band.min..band.max
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

  /**
   * Двойной тап по фону: уйти в обзор и вписать всё, а повторным — вернуться туда, откуда ушли
   * (§11.1 брифа).
   *
   * «Вписать всё» здесь означает «в масштаб уровня» дословно: нижний край полосы обзора и есть
   * вписанный граф, пока переписка достаточно коротка, а на длинной вписывается ровно столько,
   * сколько этот край позволяет.
   *
   * Когда уровень при этом меняется, вписывание откладывается до ближайшего измерения — раскладки
   * обзора ещё не существует. Когда не меняется, откладывать нельзя вовсе: рекомпозиции не будет,
   * измерения тоже, и заявке негде разрешиться.
   */
  fun fitAll() {
    stopMotion()
    if (placement.isEmpty) {
      return
    }
    val pose = restorePose
    if (pose != null) {
      restore(pose)
      return
    }
    // Поза снимается **до** вписывания и переживает только его само: любое движение камеры после
    // этого её снимает, см. `applyPan`.
    restorePose = GraphCameraPose(
      level = graphLevel,
      scale = cameraScale,
      camera = camera,
      isMoved = isMoved
    )
    pendingSwitch = null
    levelExitScale = cameraScale
    if (graphLevel == GraphLevel.Overview) {
      applyFit(placement, viewport)
    } else {
      graphLevel = GraphLevel.Overview
      pendingFit = true
    }
  }

  /** Обрывает движение камеры: новое касание отбирает её и у затухания, и у перелёта. */
  fun stopMotion() {
    motionJob?.cancel()
    motionJob = null
  }

  /**
   * Полоса масштаба текущего уровня.
   *
   * Считается по месту, а не хранится: «вписать всё» выводится из границ содержимого, а они меняются
   * с каждой раскладкой. Хранимая полоса была бы вторым источником того же числа и разошлась бы с
   * первым на первом же входящем сообщении.
   *
   * @param placement раскладка, по которой считается вписывание
   * @param viewport размер видимой области
   * @return пределы, за которыми уровень сменяется соседним
   */
  private fun levelBandOf(placement: Placement, viewport: IntSize): GraphLevelBand {
    return graphLevelBandOf(graphLevel, fitScaleOf(placement.bounds, viewport))
  }

  /**
   * Уводит полотно на соседний уровень детализации.
   *
   * Масштаб и камера здесь **не** трогаются: охват нового уровня выводится из его раскладки, а её
   * ещё нет. Кладётся заявка, которую разберёт ближайшее измерение, — а оно случится в этом же
   * кадре, потому что уровень снапшотный и композиция узлов от него зависит.
   *
   * Якорем берётся узел под пальцами, а не под центром экрана: смотрят туда, где щиплют. Индекс
   * узла валиден и на новом уровне — список узлов уровень не меняет.
   *
   * @param to уровень, на который уходим
   * @param focus точка экрана, под которой жест держит содержимое
   */
  private fun switchLevel(to: GraphLevel, focus: Offset) {
    val current = cameraAt(cameraRangeOf(placement, viewport, cameraScale))
    val point = (focus - current) / cameraScale
    pendingSwitch = GraphLevelSwitch(
      anchorIndex = nearestCentreIndexOf(placement.centres, point.x),
      anchorScreen = focus,
      spanBefore = placement.centreSpanX.endInclusive - placement.centreSpanX.start,
      scaleBefore = cameraScale
    )
    pendingFit = false
    levelExitScale = cameraScale
    isLevelSettling = true
    graphLevel = to
  }

  /**
   * Сообщает, что кроссфейд смены уровня доигран и переход можно начинать снова.
   *
   * Зовётся полотном, а не отсчитывается здесь по таймеру: длительность перехода живёт в теме и
   * обращается в ноль под reduced motion, а держатель не знает ни того, ни другого. Второй отсчёт
   * того же времени разошёлся бы с первым — и разошёлся бы молча.
   */
  fun onLevelSettled() {
    isLevelSettling = false
  }

  /**
   * Сажает камеру на новом уровне: тот же охват и тот же узел под тем же местом экрана.
   *
   * @param switch заявка, оставленная жестом
   * @param placement раскладка нового уровня
   * @param viewport размер видимой области
   */
  private fun land(switch: GraphLevelSwitch, placement: Placement, viewport: IntSize) {
    val landing = levelLandingOf(
      scaleBefore = switch.scaleBefore,
      spanBefore = switch.spanBefore,
      spanAfter = placement.centreSpanX.endInclusive - placement.centreSpanX.start,
      band = levelBandOf(placement, viewport),
      // Восемь процентов полосы — тот запас, который пинчу надо пройти осознанно, чтобы вернуться.
      // Посадка ровно на порог, с которого только что ушли, и есть мигание уровня на границе.
      margin = 0.08f
    )
    cameraScale = landing
    val range = cameraRangeOf(placement, viewport, landing)
    val anchor = placement.centres.getOrNull(switch.anchorIndex)
    camera = if (anchor == null) {
      range.clamp(camera)
    } else {
      cameraPuttingPointAt(point = anchor, screen = switch.anchorScreen, range = range, scale = landing)
    }
    isMoved = true
  }

  /**
   * Вписывает содержимое целиком и наводит камеру на его середину.
   *
   * @param placement раскладка, по которой вписываем
   * @param viewport размер видимой области
   */
  private fun applyFit(placement: Placement, viewport: IntSize) {
    val fit = fitScaleOf(placement.bounds, viewport)
    val band = graphLevelBandOf(graphLevel, fit)
    val fitted = fit.coerceIn(band.min, band.max)
    cameraScale = fitted
    camera = cameraAimedAt(
      point = placement.bounds.center,
      viewport = viewport,
      range = cameraRangeOf(placement, viewport, fitted),
      scale = fitted
    )
    isMoved = true
  }

  /**
   * Возвращает полотно в позу, снятую перед вписыванием.
   *
   * Камера восстанавливается вместе с уровнем и масштабом одним движением: она записана в пикселях
   * экрана и верна ровно для той раскладки, при которой снималась.
   *
   * @param pose поза до вписывания
   */
  private fun restore(pose: GraphCameraPose) {
    restorePose = null
    pendingSwitch = null
    pendingFit = false
    levelExitScale = cameraScale
    graphLevel = pose.level
    cameraScale = pose.scale
    camera = pose.camera
    isMoved = pose.isMoved
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
    val targetScale = flightScaleOf(
      scale = startScale,
      level = graphLevel,
      band = levelBandOf(placement, viewport)
    )
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

  /**
   * Где узел нарисован на экране прямо сейчас.
   *
   * Спрашивают отсюда, а не у самого узла: узел живёт внутри слоя камеры, и его собственные
   * координаты в дереве Compose — не то, из чего можно построить общий элемент. Общий элемент берёт
   * границы у lookahead-позиции, а та складывает только позиции размещения и преобразований слоя не
   * видит вовсе; морф, поставленный на узел напрямую, стартовал бы из мировой координаты — на
   * демо-графе это до двадцати экранов правее.
   *
   * Ответ — значение, а не [State]: его берут в момент тапа и замораживают. Живой [State] пришлось
   * бы читать при измерении якоря, то есть подписать измерение на масштаб — ровно то, чего полотно
   * избегает у себя.
   *
   * Список узлов и раскладка расходятся не более чем на кадр, поэтому индекс берётся безопасно:
   * узел, которого в раскладке ещё нет, просто не найдётся.
   *
   * @param id узел, о котором спрашивают
   * @return прямоугольник в координатах вьюпорта или `null`, если такого узла нет
   */
  fun nodeRectOf(id: BasicNode.Id): Rect? {
    val index = currentGraph.nodes.indexOfFirst { it.id == id }
    if (index < 0 || index > placement.nodes.lastIndex) {
      return null
    }
    return screenRectOf(
      topLeft = placement.nodes[index],
      size = placement.sizes[index],
      camera = offset.value,
      scale = cameraScale
    )
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
    // Камеру повели — возвращаться больше некуда: «повторный тап» из §11.1 это тап сразу следующий,
    // а не когда-нибудь потом. Сохранённая поза, пережившая панорамирование, вернула бы к месту,
    // которого пользователь уже не помнит.
    restorePose = null
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
   * Граф приходит параметром, а не берётся из [graph], и это не украшение сигнатуры. [setGraph]
   * вызывается из `SideEffect`, то есть между композицией и измерением того же кадра: прочитав
   * состояние здесь, измерение получило бы новый список узлов к measurable'ам, порождённым старой
   * композицией, — а это разные длины и индекс за границей списка. Передавая граф снаружи, полотно
   * меряет ровно тот набор, который само же и скомпоновало.
   *
   * Уровень детализации приходит параметром по той же причине, что и узлы: его меняет жест, то есть
   * между композицией и измерением того же кадра. Прочитав уровень здесь, измерение взяло бы зазоры
   * обзора к плашкам, которые композиция успела построить эпизодами.
   *
   * Дорожки приходят параметром по той же причине в третий раз — и заодно перестают считаться
   * каждым измерением. Композиция их уже посчитала: акцент нужен точке ветвления до всякого
   * измерения, а результат кэширован по узлам и веткам. Считая их здесь заново, измерение платило
   * бы за индекс узлов, отрезки занятости и жадную раскраску на каждый свой проход — включая все
   * проходы кадра смены уровня, где меняется раскладка, а дорожки те же самые.
   *
   * @param level уровень детализации той же композиции: он задаёт зазоры и шаг дорожки
   * @param graph граф, из которого построено содержимое этой композиции: из него же считаются рёбра
   * @param lanes дорожки и акценты той же композиции, см. [lanesOf]
   * @param branchColors цвет каждой ветки графа: рёбра красятся здесь, а палитру измерение не читает
   * @param viewportSize размер видимой области
   * @param nodeSizes измеренные размеры узлов, в порядке узлов графа
   * @param density плотность экрана для перевода координат полотна в пиксели
   * @param margins поля полотна вокруг содержимого, уже в пикселях. Приходят готовыми, потому что
   *   собраны из системных врезок: высота строки состояния — свойство окна Android, а не графа, и
   *   измерению полотна о ней знать неоткуда
   * @return раскладка графа
   */
  fun layout(
    level: GraphLevel,
    graph: Graph<Node>,
    lanes: Lanes,
    branchColors: Map<Branch.Id, Color>,
    viewportSize: IntSize,
    nodeSizes: List<IntSize>,
    density: Density,
    margins: CanvasMargins
  ): Placement {
    // Дорожки берутся из параметров, а не читаются готовыми из состояния: граф подменяется из
    // `SideEffect`, то есть между композицией и измерением того же кадра, и раскраска по
    // снапшот-полю разошлась бы с measurable'ами от старой композиции.
    val branchIds = graph.branchIds
    val nodeLanes = lanes.lanes
    val geometry = LaneGeometry(topLane = topLaneOf(nodeLanes), laneStep = level.toLaneStep())
    // Высоты дорожек считаются один раз на оба потребителя: раскладке и рёбрам нужны одни и те же
    // числа, и второй проход по узлам за тем же результатом измерение делало бы каждый свой кадр.
    val laneYs = with(density) { nodeLanes.map { geometry.laneYOf(it).toPx() } }
    val result = with(density) {
      placementOf(
        lanes = nodeLanes,
        gaps = graph.nodes.map { it.gap.toStepWidth(level).toPx() },
        laneYs = laneYs,
        sizes = nodeSizes,
        margins = margins
      )
    }
    viewport = viewportSize
    placement = result
    graphEdges = with(density) {
      edgesOf(
        graph = graph,
        branchColors = branchColors,
        laneYs = laneYs,
        positions = result.nodes,
        sizes = nodeSizes,
        // Хвост идёт до правого края содержимого, а не до края видимой области, как просит §6.8:
        // вьюпорта раскладка не знает и знать не должна. Поле полотна из `bounds` вычтено — оно
        // отступ, а не история; вычитается именно правое, то самое, которое `bounds` и раздуло.
        contentRight = result.bounds.right - margins.right,
        // Мостик, прижатый к концу отрезка, съедается скруглением угла: между ними должно
        // остаться место и на радиус 8 dp, и на полухорду мостика 6 dp.
        hopClearance = 16.dp.toPx()
      )
    }
    marks = laneMarksOf(
      branchIds = branchIds,
      lanes = nodeLanes,
      nodeColors = lanes.accents.map { it.color },
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
    //
    // Здесь же разбираются заявки, оставленные жестом: посадка после смены уровня и вписывание.
    // Раньше их разобрать нечем — обе выводятся из раскладки, которая только что посчиталась, — а
    // позже уже поздно: кадр уйдёт на отрисовку со сжатой раскладкой в старом масштабе.
    Snapshot.withoutReadObservation {
      val switch = pendingSwitch
      when {
        switch != null -> {
          pendingSwitch = null
          land(switch, result, viewportSize)
        }
        pendingFit -> {
          pendingFit = false
          applyFit(result, viewportSize)
        }
        isMoved -> {
          val range = cameraRangeOf(result, viewportSize, cameraScale)
          val clamped = panStepOf(camera, Offset.Zero, range).camera
          if (clamped != camera) {
            camera = clamped
          }
        }
      }
    }
    telemetry.onMeasure()
    return result
  }

  /**
   * Подменяет граф.
   *
   * Одним значением, а не списками узлов и веток порознь: занятость дорожек выводится из индексов
   * узлов по идентификаторам развилки и слияния, и список веток, разъехавшийся с узлами хотя бы на
   * кадр, дал бы раскраску по чужим индексам — **молча**, потому что `check` в раскладке сверяет
   * длины поузловых списков, а они остались бы равными.
   *
   * @param graph граф: порядок узлов и состав веток
   */
  fun setGraph(graph: Graph<Node>) {
    currentGraph = graph
  }
}

// Инерция — физика жеста, а не переход, поэтому системный множитель длительности анимаций к ней не
// применяется. Без этого «Animator duration scale» из опций разработчика менял бы пролёт броска, а
// при значении «Off» весь путь приезжал бы одной дельтой. Платформа оборачивает свой fling так же.
private val FlingDurationScale = object : MotionDurationScale {
  override val scaleFactor: Float
    get() = 1f
}
