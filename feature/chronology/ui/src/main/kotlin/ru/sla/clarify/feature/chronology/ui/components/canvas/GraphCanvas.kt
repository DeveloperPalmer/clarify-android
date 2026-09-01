package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.CanvasMargins
import ru.sla.atlas.entity.Edge
import ru.sla.atlas.entity.EdgeRole
import ru.sla.atlas.entity.Graph
import ru.sla.atlas.entity.Lanes
import ru.sla.atlas.entity.NodeAccent
import ru.sla.atlas.layout.lanesOf
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel
import ru.sla.clarify.feature.chronology.ui.entity.MergeCeremonyFrame
import ru.sla.clarify.feature.chronology.ui.entity.Node
import ru.sla.clarify.feature.chronology.ui.mapper.toBranchColors
import ru.sla.clarify.uikit.theme.AppColors
import ru.sla.clarify.uikit.theme.AppTheme
import kotlin.math.abs

/**
 * Полотно хронологии: фон, узлы графа и связи между ними, по которому можно панорамировать.
 *
 * Композабл здесь ничего не считает — только композирует, принимает жест и рисует. Где узлы стоят,
 * считает [placementOf]; камеру и результат раскладки держит [GraphCanvasState]; события
 * пальцев разбирает [detectCameraGestures].
 *
 * Масштаб — свойство камеры, а не раскладки: он применяется слоем и потому не стоит ни измерения,
 * ни рекомпозиции. **Уровень детализации** — свойство камеры тоже, но раскладку он меняет: на обзоре
 * зазоры и дорожки вчетверо теснее, а плашка вырождается в глиф. Поэтому уровень снимается в
 * композиции один раз и уходит и в содержимое узлов, и в измерение — тем же порядком, что и сам
 * список узлов, и по той же причине.
 *
 * На кадре перехода раскладка **уже целевая**, а уходящее представление рисуется поверх неё, места
 * не занимая, см. [GraphLevelCrossfade].
 *
 * @param state камера полотна и результат его последней раскладки
 * @param ceremony церемония слияния: полотно её не запускает, но рисует и учитывает в бегущем
 *   пунктире — слитая ветка на время церемонии показывается так, будто снова готова к слиянию
 * @param modifier модификатор корня полотна
 * @param overlay что нарисовать поверх полотна: панель, мини-карта, что угодно. Слот получает
 *   обработчик, которым содержимое объявляет занятую им зону под своим ключом, — жест, начатый в
 *   любой из объявленных зон, до камеры не доходит. Зона объявляется в координатах корня, а пустой
 *   прямоугольник её снимает. Полотно при этом не знает, что именно там лежит
 * @param blocked взято ли полотно целиком: пока `true`, жест не начинается вовсе. Та же зона, что
 *   объявляет содержимое слота, только во весь вьюпорт и объявленная снаружи — потому что накрывший
 *   полотно скрим обязан накрыть и шапку экрана, а значит внутри слота лежать не может. Читается в
 *   момент касания, поэтому лямбда, а не значение: иначе полотно рекомпоновалось бы на каждое
 *   открытие карточки
 * @param node содержимое узла на заданном уровне детализации; обязано выпускать ровно один элемент
 *   раскладки — полотно ставит плашки по одной на узел и считает их по позиции, а не по
 *   идентификатору
 */
@Composable
internal fun GraphCanvas(
  state: GraphCanvasState,
  ceremony: MergeCeremonyState,
  modifier: Modifier = Modifier,
  blocked: () -> Boolean = { false },
  node: @Composable (node: Node, accent: NodeAccent, level: GraphLevel) -> Unit,
  overlay: @Composable BoxScope.(onBoundsChanged: (key: Any, bounds: Rect) -> Unit) -> Unit = { }
) {
  val telemetry = state.telemetry
  // Жест не пересоздаётся при смене состояния: ключ `Unit` держит обработчик живым, а свежий
  // экземпляр приходит через rememberUpdatedState. Иначе первое же входящее сообщение отменяло бы
  // драг под пальцем.
  val currentState by rememberUpdatedState(state)
  // Спека тоже обновляется через rememberUpdatedState: `pointerInput(Unit)` не пересоздаётся, и
  // смена плотности иначе заморозила бы внутри жеста кривую от старого экрана.
  val currentDecay by rememberUpdatedState(AppTheme.motion.flingDecay<Float>())
  // По той же причине: обработчик жеста живёт дольше любой отдельной композиции, и захваченная им
  // лямбда обязана обновляться, а не застывать той, что была при открытии экрана.
  val currentBlocked by rememberUpdatedState(blocked)
  // Затухание доигрывает после того, как корутина жеста уже отменена, поэтому scope нужен свой.
  val flingScope = rememberCoroutineScope()
  // Зоны, занятые тем, что лежит поверх полотна: полотно ловит жест на всём вьюпорте и по ним
  // отличает палец, положенный на инструмент, от пальца на графе. Каждую зону объявляет и снимает
  // само содержимое слота — полотно их не вычисляет и о их природе не знает.
  //
  // Зон несколько, а не одна: у нижнего края живут и мини-карта, и отладочная панель над ней.
  // Объединять их в один прямоугольник нельзя — объединение захватывает полосу графа между
  // инструментами и **работает случайно**: пока они смежны, ложь незаметна, а первый же отступ
  // между ними отдаёт графу жесты, которых тот брать не должен.
  //
  // Поглощать жесты внутри панели нельзя, хотя это выглядело бы проще: `clickable` потребляет лишь
  // нажатие с отпусканием, а камеру двигает протяжка, и полотно принимает даже потреблённое
  // нажатие. Потреблять же сами движения — значит убить листание панели: детекторы жестов
  // проверяют потребление ещё и в Final-проходе, и вложенный пейджер отменяется.
  val overlayZones = remember { mutableStateMapOf<Any, Rect>() }
  // Зоны приходят в координатах корня, а касание — в координатах этого Box. Пока инструмент был
  // один и лежал прямым ребёнком полотна, обе системы совпадали, и `boundsInParent` работал; стоило
  // положить инструменты в общую колонку, как зона уехала бы на её смещение — молча, без единого
  // упавшего теста. Поэтому системе координат здесь одно определение на обе стороны.
  var canvasOrigin by remember { mutableStateOf(Offset.Zero) }
  val currentCanvasOrigin by rememberUpdatedState(canvasOrigin)

  SideEffect { telemetry.onCanvasComposition() }
  // Узлы снимаются один раз и уходят и в содержимое, и в измерение. Читать их в measure заново
  // нельзя: состояние подменяется из `SideEffect`, то есть уже после этой композиции, но ещё до
  // измерения того же кадра, — и измерение получило бы новый список к старым measurable'ам.
  val colors = AppTheme.colors
  val graph = state.graph
  val nodes = graph.nodes
  // Дорожки считаются здесь, а не в измерении, и это не оптимизация. Точке ветвления нужен цвет и
  // направление **уходящей** ветки, а рисуется она в композиции — то есть до того, как измерение
  // что-либо посчитает. Один и тот же результат уходит и в содержимое, и в `layout`, поэтому
  // разъехаться им нечем.
  // Цвета веток резолвятся здесь и один раз: дальше ни раскладка, ни рёбра, ни мини-карта темы не
  // знают — они получают готовый цвет. Палитра стоит в ключе, потому что смена темы меняет цвета,
  // не трогая граф.
  val branchColors = remember(graph, colors) { graph.toBranchColors(colors) }
  val lanes = remember(graph, branchColors) {
    lanesOf(graph, branchColors) { graphNode, own -> graph.accentOwnerOf(graphNode, own) }
  }
  Box(
    // Жест висит на всём вьюпорте, а не на слое узлов: полотно не всегда достаёт до края экрана,
    // и панорамирование не работало бы там, где его нет.
    modifier = modifier
      .clipToBounds()
      .onGloballyPositioned { canvasOrigin = it.boundsInRoot().topLeft }
      .pointerInput(Unit) {
        detectCameraGestures(
          isBlocked = { position ->
            val inRoot = position + currentCanvasOrigin
            currentBlocked() || overlayZones.values.any { zone -> zone.contains(inRoot) }
          },
          onTouch = { currentState.stopMotion() },
          onTransform = { focus, pan, zoom ->
            // Масштаб ложится первым: он меняет и границы камеры, и то, куда попадёт та же точка
            // экрана, — а сдвиг центроида поверх этого уже обычный шаг протяжки. Два клампа за
            // событие вместо одного здесь ничего не стоят: незажатый сдвиг никуда не копится,
            // потому что камера хранится уже зажатой.
            if (zoom != 1f) {
              currentState.zoom(focus = focus, change = zoom)
            }
            if (pan != Offset.Zero) {
              currentState.pan(pan)
            }
          },
          onRelease = { velocity ->
            currentState.fling(scope = flingScope, velocity = velocity, decay = currentDecay)
          },
          // Двойной тап живёт только на фоне: тап по плашке жест полотна не досматривает вовсе —
          // узел потребляет отпускание своим `clickable`, и детектор выходит на потреблённом
          // событии, не запомнив его.
          onDoubleTap = { currentState.fitAll() }
        )
      }
  ) {
    GraphBackdrop(
      modifier = Modifier.fillMaxSize(),
      state = state
    )
    GraphNodesLayer(
      state = state,
      graph = graph,
      lanes = lanes,
      branchColors = branchColors,
      ceremony = ceremony,
      node = node
    )
    overlay { key, bounds ->
      // Пустой прямоугольник — это и есть «зоны больше нет»: инструмент, уходящий с экрана, обязан
      // снять её за собой, иначе полотно продолжит обходить стороной пустое место.
      if (bounds.isEmpty) overlayZones.remove(key) else overlayZones[key] = bounds
    }
  }
}

/**
 * Слой узлов: плашки, связи между ними и камера, двигающая их все разом.
 *
 * Слой равен вьюпорту, а не полотну: `requiredSize` центрирует содержимое шире входящих
 * ограничений, и полотно уезжало бы мимо камеры. Узлы выходят за границы слоя — слой не обрезает,
 * обрезает вьюпорт снаружи.
 *
 * Жест сюда не приходит: он висит на всём вьюпорте, потому что полотно не всегда достаёт до края
 * экрана и панорамирование не работало бы там, где узлов нет.
 *
 * @param state камера полотна и результат его последней раскладки
 * @param graph граф, из которого строится содержимое; приходит параметром, а не читается из [state],
 *   чтобы композиция и измерение одного кадра видели один и тот же список
 * @param lanes дорожки и акценты того же кадра, посчитанные один раз на оба потребителя
 * @param ceremony церемония слияния этого кадра
 * @param modifier модификатор слоя
 * @param node содержимое узла на заданном уровне детализации
 */
@Composable
private fun GraphNodesLayer(
  state: GraphCanvasState,
  graph: Graph<Node>,
  lanes: Lanes,
  branchColors: Map<Branch.Id, Color>,
  ceremony: MergeCeremonyState,
  modifier: Modifier = Modifier,
  node: @Composable (node: Node, accent: NodeAccent, level: GraphLevel) -> Unit
) {
  val telemetry = state.telemetry
  val colors = AppTheme.colors
  // Путь один на все рёбра и чистится `rewind()`, а не создаётся заново: он держит выделенную
  // память между вызовами, и сотня рёбер иначе рождала бы сотню нативных объектов на каждый проход.
  val edgePath = remember { Path() }
  // Второй путь и мерка — только для кадра 4: маршрут возврата рисуется не целиком, а до отметки
  // `reach`. Оба переиспользуются между кадрами по той же причине, что и сам `edgePath`.
  val reachedPath = remember { Path() }
  val pathMeasure = remember { PathMeasure() }
  val density = LocalDensity.current
  // Ключ — плотность: спека, замороженная от старого экрана, уже однажды стоила фиче дефекта.
  val dashIntervals = remember(density) {
    with(density) { floatArrayOf(6.dp.toPx(), 4.dp.toPx()) }
  }
  // Неподвижный пунктир кэшируется, потому что `AndroidPathEffect` не имеет `equals`: `Stroke`
  // сравнивает его по ссылке, и новый экземпляр на каждом кадре означал бы новый нативный
  // `DashPathEffect` и вызов `Paint.setPathEffect` на каждое ребро.
  val dashEffect = remember(dashIntervals) { PathEffect.dashPathEffect(dashIntervals) }
  // Бежит пунктир только у веток, готовых к слиянию (§7), а таких на графе может не быть вовсе.
  // Фаза заводится ровно тогда, когда есть чему бежать: бесконечная анимация запрашивает кадр,
  // пока жива, а её чтение в рисовании перерисовывает слой связей каждый кадр — на графе без
  // готовых веток это был бы вечный кадр ни для чего.
  val hasRunningEdge = remember(state, ceremony) {
    derivedStateOf { isDashRunning(state.edges.value, ceremony.playing.value) }
  }
  val dashPhase = rememberDashPhase(period = dashIntervals.sum(), isRunning = hasRunningEdge.value)
  val cornerRadius = with(density) { 8.dp.toPx() }
  val hopRadius = with(density) { 6.dp.toPx() }
  val fadeLength = with(density) { 40.dp.toPx() }
  // Кадр 6 расходится на 120 dp в каждую сторону — три ширины точки слияния. §12 говорит «короткая
  // вспышка», числа не даёт; это число выбрано здесь и ждёт взгляда на устройстве.
  val waveSpread = with(density) { 120.dp.toPx() }
  val statusBar = WindowInsets.statusBars
  val navigationBar = WindowInsets.navigationBars
  // Уровень снимается один раз и уходит и в содержимое, и в измерение: прочитанный в measure заново,
  // он пришёл бы к плашкам, построенным другим уровнем, — ровно та же ловушка, что и со списком
  // узлов. Переход держит `Transition`: `currentState` — уходящий уровень, и своего держателя со
  // своим scope переходу поэтому не нужно.
  //
  // Целевой уровень берётся у держателя, а не у перехода. `Transition.targetState` — то же самое
  // значение, но записанное **во время композиции**, изнутри `updateTransition`: читатель такой
  // записи получает вторую рекомпозицию на том же кадре, а вместе с ней и второй проход измерения.
  // Сама библиотека закрывается от этого `derivedStateOf` внутри `animateValue` и оставляет там об
  // этом комментарий; снаружи обход недоступен — зато доступен источник, из которого `targetState`
  // и получен.
  val level = state.level.value
  val transition = updateTransition(targetState = level, label = "lod")
  val previousLevel = transition.currentState.takeIf { it != level }
  // О конце перехода полотно сообщает держателю само: длительность живёт в теме и обращается в ноль
  // под reduced motion, а держатель камеры не знает ни про тему, ни про то, что его уровень кто-то
  // показывает кроссфейдом. Пока не сообщено, второго перехода поверх этого не начинается.
  SideEffect { if (previousLevel == null) state.onLevelSettled() }
  // Под reduced motion кроссфейда нет вовсе: уровень подменяется мгновенно. Гаптику при этом зовёт
  // экран — отклик это не движение, и глушить его вместе с анимацией нельзя (§14).
  val fadeSpec = if (rememberReducedMotion()) snap<Float>() else AppTheme.motion.mediumTween()
  // Доля заводится на время перехода и привязана к **уходящему** уровню, а не к текущей цели.
  // Привязка к цели — это `if (frameLevel == level)`, где `level` и есть `targetState`: условие
  // истинно на любом кадре, целевым значением анимации выходит постоянная единица, и гаснуть
  // уходящему представлению не из чего. Так и было — кроссфейд не рисовался ни разу, а удвоенное
  // дерево узлов полотно держало все двести пятьдесят миллисекунд честно.
  //
  // Анимация, заведённая в этой точке, берёт начальное значение у `currentState`, то есть у
  // уходящего уровня, и получает единицу, а целевое — у цели, и получает ноль. Ровно это и нужно.
  val exit = previousLevel?.let { outgoingLevel ->
    GraphLevelExit(
      level = outgoingLevel,
      alpha = transition.animateFloat(
        transitionSpec = { fadeSpec },
        label = "exit"
      ) { frameLevel ->
        if (frameLevel == outgoingLevel) 1f else 0f
      }
    )
  }
  Layout(
    modifier = modifier
      .fillMaxSize()
      .graphicsLayer {
        // Камера и масштаб читаются здесь, а не в композиции: кадр панорамирования и кадр пинча
        // обновляют только свойства слоя — ни рекомпозиции, ни повторного измерения, ни новых
        // модификаторов. Узлы при этом не перевёрстываются: масштаб применяется к готовому списку
        // команд рисования, поэтому текст перерисовывается в конечном размере и остаётся резким.
        telemetry.onLayerUpdate()
        // Начало координат — угол вьюпорта, а не его центр, и это не оформление: только так
        // выполняется `экран = полотно · scale + камера`, из которого выведены и диапазон камеры,
        // и удержание точки под пальцами. С центром пришлось бы вносить размер вьюпорта в обе
        // формулы дважды.
        transformOrigin = TransformOrigin(0f, 0f)
        val scale = state.scale.value
        scaleX = scale
        scaleY = scale
        val camera = state.offset.value
        translationX = camera.x
        translationY = camera.y
      }
      .drawBehind {
        // Связи рисуются внутри того же слоя, поэтому масштабируются вместе с плашками: на 0.4×
        // линия истончается до 0.8 dp, на 2.5× толстеет до пяти, а штрих пунктира растягивается
        // вместе с ней. Одно правило на весь слой проще двух; если тонкая линия потеряется на
        // устройстве, обратный ход — поделить толщину на масштаб прямо здесь.
        telemetry.onEdgeDraw()
        // Фаза одна на все рёбра, поэтому и нативный объект на кадр создаётся один, а не по одному
        // на ребро. Кэшировать его, как неподвижный, нельзя: в фазе и состоит весь бег.
        val frame = ceremony.frame.value
        val ceremonyBranch = ceremony.branch.value
        val edges = state.edges.value
        val runningDashEffect = dashPhase?.let { phase ->
          // Разгон кадра 1 — это множитель на фазу: узор периодичен, и фаза, идущая втрое дальше за
          // тот же цикл, и есть тот же бег втрое быстрее. Отдельной анимации разгону не нужно.
          PathEffect.dashPathEffect(dashIntervals, phase.value * (frame?.dashSpeed ?: 1f))
        }
        // Точку слияния берём у самого маршрута возврата: его последняя точка и лежит на магистрали.
        // Спрашивать её у раскладки значило бы завести второй источник того же числа.
        val mergePoint = frame?.let {
          edges.firstOrNull { edge ->
            edge.role == EdgeRole.Merge && isCeremonyEdge(edge, ceremonyBranch)
          }?.points?.last()
        }
        edges.fastForEach { edge ->
          drawGraphEdge(
            edge = edge,
            path = edgePath,
            colors = colors,
            dashEffect = dashEffect,
            runningDashEffect = runningDashEffect,
            cornerRadius = cornerRadius,
            hopRadius = hopRadius,
            fadeLength = fadeLength,
            frame = frame.takeIf { isCeremonyEdge(edge, ceremonyBranch) },
            reachedPath = reachedPath,
            pathMeasure = pathMeasure
          )
        }
        if (frame != null && mergePoint != null) {
          drawMergeWave(
            edges = edges,
            path = edgePath,
            colors = colors,
            frame = frame,
            mergePoint = mergePoint,
            cornerRadius = cornerRadius,
            hopRadius = hopRadius,
            spread = waveSpread
          )
        }
      },
    content = {
      graph.nodes.fastForEachIndexed { index, graphNode ->
        key(graphNode.id.value) {
          SideEffect { telemetry.onNodeComposition() }
          GraphLevelCrossfade(
            exit = exit,
            exitScale = state.exitScale,
            scale = state.scale,
            incoming = { node(graphNode, lanes.accents[index], level) },
            outgoing = { outgoingLevel -> node(graphNode, lanes.accents[index], outgoingLevel) }
          )
        }
      }
    }
  ) { measurables, constraints ->
    // Узел меряется свободно: ширину он ограничивает сам, а вьюпорт ему не указ — узел может
    // стоять далеко за правым краем экрана.
    val placeables = measurables.fastMap { it.measure(Constraints()) }
    val placement = state.layout(
      level = level,
      lanes = lanes,
      branchColors = branchColors,
      density = this,
      // Врезки складываются с отступом здесь, а не в измерении полотна: полотно занимает весь экран
      // под системными барами, и увести из-под них плашки обязаны поля — но чем эти бары высоки,
      // знает окно, а не граф.
      margins = CanvasMargins(
        left = 64.dp.toPx(),
        top = 64.dp.toPx() + statusBar.getTop(this),
        right = 64.dp.toPx(),
        bottom = 64.dp.toPx() + navigationBar.getBottom(this)
      ),
      viewportSize = IntSize(constraints.maxWidth, constraints.maxHeight),
      graph = graph,
      nodeSizes = placeables.fastMap { IntSize(it.width, it.height) }
    )
    layout(constraints.maxWidth, constraints.maxHeight) {
      telemetry.onPlacement()
      placeables.fastForEachIndexed { index, placeable -> placeable.place(placement.nodes[index]) }
    }
  }
}

/**
 * Кроссфейд представлений одного узла при смене уровня детализации (§5 брифа).
 *
 * Коробку задаёт **целевое** представление, поэтому раскладка на кадре перехода уже целевая: кадра,
 * где зазоры одного уровня, а плашки другого, не существует вовсе. Уходящее представление рисуется
 * поверх и места в раскладке не занимает — то же правило, по которому здесь живут гало
 * непрочитанного, подпись фронта и чип слияния.
 *
 * Готовый `Crossfade` для этого не годится, и это проверено по его исходникам: он кладёт оба
 * состояния в один `Box`, то есть коробка узла все 250 мс равна большему из двух — зазоры уже сжаты,
 * а плашка ещё во всю ширину, и в конце анимации раскладка защёлкивается. Кроссфейд, заведённый
 * скрыть скачок, сам бы его и создал.
 *
 * **Гаснет только уходящее.** Оно лежит поверх целевого и до конца перехода закрывает его собой,
 * поэтому проявление целевого — это и есть угасание уходящего, и встречной доли второму слою не
 * нужно. Симметричная пара к тому же провалилась бы в середине перехода: две полупрозрачности не
 * складываются в непрозрачность, и на середине сквозь узел просвечивал бы фон.
 *
 * Коробка стоит **всегда**, а не заводится на время перехода, и это не небрежность. `if`,
 * выбирающий между «узел» и «узел в коробке», — две разные точки вызова, то есть две разные группы
 * композиции: и на входе в переход, и на выходе из него поддерево узла выбрасывалось бы и строилось
 * заново целиком. Постоянная коробка стоит одного элемента раскладки на узел и ни одного слоя, а
 * уходящее появляется и исчезает внутри неё — там, где оно и обязано появляться и исчезать.
 *
 * Встречный масштаб оставляет уходящему представлению тот размер, каким оно было на экране в момент
 * перехода: слой камеры к этому кадру уже приземлился на новый масштаб, и плашка внутри него
 * растянулась бы в несколько раз. Обе доли читаются в `graphicsLayer`, то есть в фазе слоя: чтение в
 * композиции пересобирало бы узлы каждый кадр перехода.
 *
 * @param exit уходящее представление или `null`, когда перехода нет
 * @param exitScale масштаб, на котором полотно ушло с прошлого уровня
 * @param scale масштаб прямо сейчас
 * @param incoming представление целевого уровня
 * @param outgoing представление уровня, который гаснет
 */
@Composable
private fun GraphLevelCrossfade(
  exit: GraphLevelExit?,
  exitScale: State<Float>,
  scale: State<Float>,
  incoming: @Composable () -> Unit,
  outgoing: @Composable (level: GraphLevel) -> Unit
) {
  Box(contentAlignment = Alignment.Center) {
    incoming()
    if (exit != null) {
      Box(
        modifier = Modifier
          .layout { measurable, _ ->
            // Ноль вместо размера: коробку узла задаёт целевое представление, а уходящее только
            // рисуется. Родитель выравнивает нулевой размер по центру, поэтому смещение на половину
            // ставит уходящее центром в центр целевого.
            val placeable = measurable.measure(Constraints())
            layout(width = 0, height = 0) {
              placeable.place(x = -placeable.width / 2, y = -placeable.height / 2)
            }
          }
          .graphicsLayer {
            alpha = exit.alpha.value
            val counter = counterScaleOf(from = exitScale.value, to = scale.value)
            scaleX = counter
            scaleY = counter
          }
      ) {
        outgoing(exit.level)
      }
    }
  }
}

/**
 * Фаза бегущего пунктира — сдвиг узора вдоль линии, в пикселях полотна.
 *
 * Фаза идёт **вниз**, от нуля к минус периоду, и это не описка: фаза сдвигает узор назад по пути,
 * поэтому вперёд — от развилки к слиянию — пунктир бежит при убывающей. На стыке итераций значение
 * прыгает с минус периода в ноль, но узор периодичен, и прыжок этот невидим по построению.
 *
 * Бег живёт в координатах полотна, а не экрана: слой масштабируется целиком, поэтому на 2.5× вместе
 * со штрихом растягивается и скорость. Это то же правило, по которому там же толстеет сама линия.
 *
 * @param period длина одного повтора узора: штрих плюс пробел, в пикселях
 * @param isRunning есть ли на графе ветка, готовая к слиянию
 * @return фаза или `null`, если бежать нечему
 */
@Composable
private fun rememberDashPhase(period: Float, isRunning: Boolean): State<Float>? {
  if (!isRunning) {
    return null
  }
  val transition = rememberInfiniteTransition(label = "dash")
  return transition.animateFloat(
    label = "phase",
    initialValue = 0f,
    targetValue = -period,
    animationSpec = AppTheme.motion.loopTween()
  )
}

/**
 * Рисует одно ребро: собирает путь по точкам излома и кладёт на него штрих.
 *
 * `Path` приходит снаружи и чистится `rewind()`: он переиспользуется между рёбрами, иначе каждый
 * проход слоя рождал бы по нативному объекту на ребро.
 *
 * Углы скругляются вручную квадратичной Безье, а не `PathEffect.cornerPathEffect`, и это решение с
 * причиной. Эффект скругляет **все** вершины контура, включая полученные из дуги мостика, — то есть
 * портит ровно тот приём, ради которого мостик заведён; вдобавок он молча ужимает радиус до
 * половины сегмента, чего в его документации нет. Отклонение параболы от настоящей дуги при радиусе
 * 8 dp — 0.49 dp, и увидеть его нельзя.
 *
 * @param edge ребро в координатах полотна
 * @param path переиспользуемый путь
 * @param colors палитра активной темы
 * @param dashEffect кэшированный неподвижный пунктир
 * @param runningDashEffect тот же пунктир, сдвинутый на фазу этого кадра; `null`, если готовых к
 *   слиянию веток на графе нет
 * @param cornerRadius радиус скругления углов
 * @param hopRadius радиус мостика над чужой вертикалью
 * @param fadeLength длина растворения хвоста
 * @param frame кадр церемонии, если её играет **это** ребро; `null` — ребро рисуется своим статусом
 * @param reachedPath путь под обрезанный кадром 4 маршрут возврата
 * @param pathMeasure мерка для той же обрезки
 */
@Suppress("LongParameterList", "CyclomaticComplexMethod")
private fun DrawScope.drawGraphEdge(
  edge: Edge,
  path: Path,
  colors: AppColors,
  dashEffect: PathEffect,
  runningDashEffect: PathEffect?,
  cornerRadius: Float,
  hopRadius: Float,
  fadeLength: Float,
  frame: MergeCeremonyFrame? = null,
  reachedPath: Path? = null,
  pathMeasure: PathMeasure? = null
) {
  // Кадр 4 рисует возврат по частям, и до его начала рисовать нечего вовсе.
  if (frame != null && edge.role == EdgeRole.Merge && frame.reach <= 0f) {
    return
  }
  path.rewind()
  path.addGraphRoute(edge, cornerRadius, hopRadius)
  val identity = edge.color
  // Кадр 1 уводит цвет в золото, кадр 7 возвращает его ветке: золото по §7 — событие, а не
  // идентичность, и линия, оставшаяся золотой, соврала бы о том, чья она.
  val color = frame?.let { lerp(identity, colors.contentGoldPrimary, it.gold) } ?: identity
  val width = when (edge.role) {
    EdgeRole.Baseline -> 2.dp.toPx()
    // Слой ответов §7 рисуется 1 dp, но его здесь нет: он живёт только внутри раскрытого эпизода.
    EdgeRole.Branch, EdgeRole.Fork, EdgeRole.Merge, EdgeRole.Tail -> 1.5.dp.toPx()
  }
  // На время церемонии ветка показывается готовой к слиянию: у слитой штрих сплошной, и кадру 1
  // нечего было бы ускорять. Покой возвращает выдох, а не конец шкалы.
  val status = if (frame != null) Branch.Status.Ready else edge.status
  val alpha = frame?.let { ceremonyEdgeAlphaOf(it) } ?: edge.status.toEdgeAlpha()
  val style = Stroke(
    width = width,
    // Кап тупой, а не круглый, хотя прототип берёт круглый: тот добавляет по половине толщины с
    // каждой стороны штриха, и пробел 4 dp читается как 2.5 dp, а на 0.4× пунктир сливается в
    // сплошную линию. Штрих — единственное, что отличает «живёт» от «MR открыт» помимо иконки.
    cap = StrokeCap.Butt,
    join = StrokeJoin.Round,
    pathEffect = status.toPathEffect(dashEffect, runningDashEffect)
  )
  if (frame != null && edge.role == EdgeRole.Merge && reachedPath != null && pathMeasure != null) {
    // Кадр 4: конец линии идёт по маршруту к кольцу. Отметка 0 у мерки лежит у последнего узла
    // ветки, длина — на магистрали, поэтому отрезок `[0, длина · reach]` и есть пройденный путь.
    pathMeasure.setPath(path, false)
    reachedPath.rewind()
    pathMeasure.getSegment(0f, pathMeasure.length * frame.reach, reachedPath, true)
    drawPath(path = reachedPath, color = color, alpha = alpha, style = style)
    return
  }
  if (edge.role == EdgeRole.Tail) {
    // Хвост растворяется у своего конца: линия не обрывается стеной, а «продолжается в будущее».
    val end = edge.points.last()
    drawPath(
      path = path,
      brush = Brush.horizontalGradient(
        colorStops = arrayOf(
          0f to color.copy(alpha = alpha),
          1f to color.copy(alpha = 0f)
        ),
        startX = maxOf(edge.points.first().x, end.x - fadeLength),
        endX = end.x
      ),
      style = style
    )
  } else {
    drawPath(path = path, color = color, alpha = alpha, style = style)
  }
}

/**
 * Кадр 6: короткая вспышка вдоль магистрали в обе стороны от точки слияния.
 *
 * Рисуется вторым проходом по тем же рёбрам магистрали, а не своей геометрией: у волны нет
 * собственного маршрута — она бежит по линии, которая уже есть, и повторять её изгибы значило бы
 * завести второй источник одной и той же ломаной.
 *
 * Расхождение и угасание идут из одной доли: волна тем шире, чем слабее. Так вспышка кончается
 * растворением, а не обрывом.
 *
 * @param edges рёбра этого кадра
 * @param path переиспользуемый путь
 * @param colors палитра активной темы
 * @param frame кадр церемонии
 * @param mergePoint точка слияния на магистрали
 * @param cornerRadius радиус скругления углов
 * @param hopRadius радиус мостика
 * @param spread наибольшее расхождение волны в каждую сторону
 */
@Suppress("LongParameterList")
private fun DrawScope.drawMergeWave(
  edges: List<Edge>,
  path: Path,
  colors: AppColors,
  frame: MergeCeremonyFrame,
  mergePoint: Offset,
  cornerRadius: Float,
  hopRadius: Float,
  spread: Float
) {
  if (frame.wave <= 0f || frame.wave >= 1f) {
    return
  }
  val reach = spread * frame.wave
  val style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Butt, join = StrokeJoin.Round)
  val brush = Brush.horizontalGradient(
    colorStops = arrayOf(
      0f to colors.contentGoldPrimary.copy(alpha = 0f),
      // 30 % в §12 — это альфа в начале кадра, и она гаснет вместе с расхождением.
      0.5f to colors.contentGoldPrimary.copy(alpha = 0.3f * (1f - frame.wave)),
      1f to colors.contentGoldPrimary.copy(alpha = 0f)
    ),
    startX = mergePoint.x - reach,
    endX = mergePoint.x + reach
  )
  edges.fastForEach { edge ->
    if (edge.role != EdgeRole.Baseline) {
      return@fastForEach
    }
    path.rewind()
    path.addGraphRoute(edge, cornerRadius, hopRadius)
    drawPath(path = path, brush = brush, style = style)
  }
}

/**
 * Достраивает путь по точкам излома ребра: прямые, скруглённые углы и мостики.
 *
 * @param edge ребро в координатах полотна
 * @param cornerRadius радиус скругления углов
 * @param hopRadius радиус мостика
 */
private fun Path.addGraphRoute(edge: Edge, cornerRadius: Float, hopRadius: Float) {
  val points = edge.points
  moveTo(points.first().x, points.first().y)
  points.indices.drop(1).forEach { index ->
    val target = points[index]
    val previous = points[index - 1]
    val isLast = index == points.lastIndex
    // Угол срезается на радиус с обеих сторон: до угла ведёт прямая, сам угол — контрольная точка
    // квадратичной Безье, ровно как в прототипе.
    val corner = if (isLast) 0f else minOf(cornerRadius, distanceTo(target, points[index + 1]) / 2f)
    val approach = shortenedTowards(previous, target, corner)
    if (previous.y == target.y) {
      addHorizontalWithHops(previous, approach, edge.hops, hopRadius)
    } else {
      lineTo(approach.x, approach.y)
    }
    if (!isLast) {
      val departure = shortenedTowards(points[index + 1], target, corner)
      quadraticTo(target.x, target.y, departure.x, departure.y)
    }
  }
}

/**
 * Ведёт горизонтальный участок, поднимая полукруглый мостик над каждой чужой вертикалью.
 *
 * Мостик — ровно полуокружность: хорда 12 dp при радиусе 6 равна двум радиусам, поэтому дуга
 * поднимается на 6 dp и возвращается на линию. Дуга идёт вверх независимо от направления линии —
 * так же, как в схемах метро и в git-графах, откуда приём и взят.
 */
private fun Path.addHorizontalWithHops(
  from: Offset,
  to: Offset,
  hops: List<Float>,
  hopRadius: Float
) {
  val forward = to.x >= from.x
  val inside = hops.filter { hop ->
    if (forward) {
      hop > from.x + hopRadius && hop < to.x - hopRadius
    } else {
      hop < from.x - hopRadius && hop > to.x + hopRadius
    }
  }
  val ordered = if (forward) inside.sorted() else inside.sortedDescending()
  ordered.forEach { hop ->
    val entry = if (forward) hop - hopRadius else hop + hopRadius
    val exit = if (forward) hop + hopRadius else hop - hopRadius
    lineTo(entry, from.y)
    arcTo(
      rect = Rect(
        left = hop - hopRadius,
        top = from.y - hopRadius,
        right = hop + hopRadius,
        bottom = from.y + hopRadius
      ),
      startAngleDegrees = if (forward) 180f else 0f,
      sweepAngleDegrees = if (forward) 180f else -180f,
      forceMoveTo = false
    )
    lineTo(exit, from.y)
  }
  lineTo(to.x, to.y)
}

/**
 * Прозрачность линии по состоянию ветки: §7 гасит слитую до 60 %, остальные идут в полную силу.
 *
 * Число берётся из [MERGED_EDGE_ALPHA], а не пишется здесь: к нему же кадром 7 приходит выдох
 * церемонии, и разойдись эти два места, конец церемонии дёрнул бы линию скачком.
 */
private fun Branch.Status.toEdgeAlpha(): Float {
  return when (this) {
    Branch.Status.Merged -> MERGED_EDGE_ALPHA
    Branch.Status.Alive, Branch.Status.Waiting, Branch.Status.Ready -> 1f
  }
}

/**
 * Штрих по состоянию ветки: §7 даёт замороженной пунктир, а готовой к слиянию — тот же пунктир, но
 * бегущий.
 *
 * Узор у обоих один, и это не экономия: «MR открыт» и «одобрен обоими» — соседние состояния одной
 * заморозки, и разный узор объявил бы их разными по природе. Движение же читается как «дело
 * доведено до конца и ждёт только нажатия».
 *
 * @param dash неподвижный пунктир
 * @param runningDash пунктир, сдвинутый на фазу кадра; `null` означает, что готовых веток на графе
 *   нет и фазу никто не считает
 * @return эффект штриха или `null` у сплошной линии
 */
private fun Branch.Status.toPathEffect(dash: PathEffect, runningDash: PathEffect?): PathEffect? {
  return when (this) {
    Branch.Status.Waiting -> dash
    Branch.Status.Ready -> runningDash ?: dash
    Branch.Status.Alive, Branch.Status.Merged -> null
  }
}

/** Точка на отрезке `from → to`, отступающая от `to` на `distance`. */
private fun shortenedTowards(from: Offset, to: Offset, distance: Float): Offset {
  return when {
    from.x == to.x -> Offset(to.x, to.y + distance * if (from.y > to.y) 1f else -1f)
    else -> Offset(to.x + distance * if (from.x > to.x) 1f else -1f, to.y)
  }
}

/** Длина отрезка между соседними точками ортогональной ломаной. */
private fun distanceTo(from: Offset, to: Offset): Float {
  return abs(to.x - from.x) + abs(to.y - from.y)
}
