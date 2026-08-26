package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranch
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranchStatus
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdge
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdgeRole
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeAccent
import ru.sla.clarify.feature.chronology.ui.mapper.toBranchColor
import ru.sla.clarify.uikit.theme.AppColors
import ru.sla.clarify.uikit.theme.AppTheme
import kotlin.math.abs

/**
 * Полотно хронологии: фон, узлы графа и связи между ними, по которому можно панорамировать.
 *
 * Композабл здесь ничего не считает — только композирует, принимает жест и рисует. Где узлы стоят,
 * считает [graphPlacementOf]; камеру и результат раскладки держит [GraphCanvasState]; события
 * пальцев разбирает [detectCameraGestures].
 *
 * Масштаб — свойство камеры, а не раскладки: он применяется слоем и потому не стоит ни измерения,
 * ни рекомпозиции. Переключения уровней детализации по порогам масштаба пока нет — узлы на всех
 * масштабах рисуются одни и те же, и на нижней границе диапазона текст становится нечитаемым по
 * построению.
 *
 * @param state камера полотна и результат его последней раскладки
 * @param modifier модификатор корня полотна
 * @param overlay что нарисовать поверх полотна: панель, мини-карта, что угодно. Слот получает
 *   обработчик, которым содержимое объявляет занятую им зону под своим ключом, — жест, начатый в
 *   любой из объявленных зон, до камеры не доходит. Зона объявляется в координатах корня, а пустой
 *   прямоугольник её снимает. Полотно при этом не знает, что именно там лежит
 * @param node содержимое узла; обязано выпускать ровно один элемент раскладки — полотно ставит
 *   плашки по одной на узел и считает их по позиции, а не по идентификатору
 */
@Composable
internal fun GraphCanvas(
  state: GraphCanvasState,
  modifier: Modifier = Modifier,
  node: @Composable (node: GraphNode, accent: GraphNodeAccent) -> Unit,
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
  val nodes = state.nodes
  val branches = state.branches
  // Дорожки считаются здесь, а не в измерении, и это не оптимизация. Точке ветвления нужен цвет и
  // направление **уходящей** ветки, а рисуется она в композиции — то есть до того, как измерение
  // что-либо посчитает. Один и тот же результат уходит и в содержимое, и в `layout`, поэтому
  // разъехаться им нечем.
  val accents = remember(nodes, branches) { graphAccentsOf(nodes, branches) }
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
            overlayZones.values.any { zone -> zone.contains(inRoot) }
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
          }
        )
      }
  ) {
    GraphBackdrop(
      modifier = Modifier.fillMaxSize(),
      state = state
    )
    GraphNodesLayer(
      state = state,
      nodes = nodes,
      branches = branches,
      accents = accents,
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
 * @param nodes узлы, из которых строится содержимое; приходят параметром, а не читаются из [state],
 *   чтобы композиция и измерение одного кадра видели один и тот же список
 * @param branches ветки того же кадра; из них считаются дорожки
 * @param accents цвет и направление каждого узла, в порядке [nodes]
 * @param modifier модификатор слоя
 * @param node содержимое узла
 */
@Composable
private fun GraphNodesLayer(
  state: GraphCanvasState,
  nodes: List<GraphNode>,
  branches: List<GraphBranch>,
  accents: List<GraphNodeAccent>,
  modifier: Modifier = Modifier,
  node: @Composable (node: GraphNode, accent: GraphNodeAccent) -> Unit
) {
  val telemetry = state.telemetry
  val colors = AppTheme.colors
  // Путь один на все рёбра и чистится `rewind()`, а не создаётся заново: он держит выделенную
  // память между вызовами, и сотня рёбер иначе рождала бы сотню нативных объектов на каждый проход.
  val edgePath = remember { Path() }
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
  val hasRunningEdge = remember(state) {
    derivedStateOf { state.edges.value.fastAny { it.status == GraphBranchStatus.Ready } }
  }
  val dashPhase = rememberDashPhase(period = dashIntervals.sum(), isRunning = hasRunningEdge.value)
  val cornerRadius = with(density) { 8.dp.toPx() }
  val hopRadius = with(density) { 6.dp.toPx() }
  val fadeLength = with(density) { 40.dp.toPx() }
  val statusBar = WindowInsets.statusBars
  val navigationBar = WindowInsets.navigationBars
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
        val runningDashEffect = dashPhase?.let { phase ->
          PathEffect.dashPathEffect(dashIntervals, phase.value)
        }
        state.edges.value.fastForEach { edge ->
          drawGraphEdge(
            edge = edge,
            path = edgePath,
            colors = colors,
            dashEffect = dashEffect,
            runningDashEffect = runningDashEffect,
            cornerRadius = cornerRadius,
            hopRadius = hopRadius,
            fadeLength = fadeLength
          )
        }
      },
    content = {
      nodes.fastForEachIndexed { index, graphNode ->
        key(graphNode.id.value) {
          SideEffect { telemetry.onNodeComposition() }
          node(graphNode, accents[index])
        }
      }
    }
  ) { measurables, constraints ->
    // Узел меряется свободно: ширину он ограничивает сам, а вьюпорт ему не указ — узел может
    // стоять далеко за правым краем экрана.
    val placeables = measurables.fastMap { it.measure(Constraints()) }
    val placement = state.layout(
      branches = branches,
      density = this,
      statusBar = statusBar.getTop(this).toFloat(),
      navigationBar = navigationBar.getBottom(this).toFloat(),
      viewportSize = IntSize(constraints.maxWidth, constraints.maxHeight),
      nodes = nodes,
      nodeSizes = placeables.fastMap { IntSize(it.width, it.height) }
    )
    layout(constraints.maxWidth, constraints.maxHeight) {
      telemetry.onPlacement()
      placeables.fastForEachIndexed { index, placeable -> placeable.place(placement.nodes[index]) }
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
 */
@Suppress("LongParameterList")
private fun DrawScope.drawGraphEdge(
  edge: GraphEdge,
  path: Path,
  colors: AppColors,
  dashEffect: PathEffect,
  runningDashEffect: PathEffect?,
  cornerRadius: Float,
  hopRadius: Float,
  fadeLength: Float
) {
  path.rewind()
  path.addGraphRoute(edge, cornerRadius, hopRadius)
  val color = edge.colorIndex.toBranchColor(colors)
  val width = when (edge.role) {
    GraphEdgeRole.Trunk -> 2.dp.toPx()
    // Слой ответов §7 рисуется 1 dp, но его здесь нет: он живёт только внутри раскрытого эпизода.
    GraphEdgeRole.Branch, GraphEdgeRole.Fork, GraphEdgeRole.Merge, GraphEdgeRole.Tail -> 1.5.dp.toPx()
  }
  val style = Stroke(
    width = width,
    // Кап тупой, а не круглый, хотя прототип берёт круглый: тот добавляет по половине толщины с
    // каждой стороны штриха, и пробел 4 dp читается как 2.5 dp, а на 0.4× пунктир сливается в
    // сплошную линию. Штрих — единственное, что отличает «живёт» от «MR открыт» помимо иконки.
    cap = StrokeCap.Butt,
    join = StrokeJoin.Round,
    pathEffect = edge.status.toPathEffect(dashEffect, runningDashEffect)
  )
  if (edge.role == GraphEdgeRole.Tail) {
    // Хвост растворяется у своего конца: линия не обрывается стеной, а «продолжается в будущее».
    val end = edge.points.last()
    drawPath(
      path = path,
      brush = Brush.horizontalGradient(
        colorStops = arrayOf(
          0f to color.copy(alpha = edge.status.toEdgeAlpha()),
          1f to color.copy(alpha = 0f)
        ),
        startX = maxOf(edge.points.first().x, end.x - fadeLength),
        endX = end.x
      ),
      style = style
    )
  } else {
    drawPath(path = path, color = color, alpha = edge.status.toEdgeAlpha(), style = style)
  }
}

/**
 * Достраивает путь по точкам излома ребра: прямые, скруглённые углы и мостики.
 *
 * @param edge ребро в координатах полотна
 * @param cornerRadius радиус скругления углов
 * @param hopRadius радиус мостика
 */
private fun Path.addGraphRoute(edge: GraphEdge, cornerRadius: Float, hopRadius: Float) {
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

/** Прозрачность линии по состоянию ветки: §7 гасит слитую до 60 %, остальные идут в полную силу. */
private fun GraphBranchStatus.toEdgeAlpha(): Float {
  return when (this) {
    GraphBranchStatus.Merged -> 0.6f
    GraphBranchStatus.Alive, GraphBranchStatus.Waiting, GraphBranchStatus.Ready -> 1f
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
private fun GraphBranchStatus.toPathEffect(dash: PathEffect, runningDash: PathEffect?): PathEffect? {
  return when (this) {
    GraphBranchStatus.Waiting -> dash
    GraphBranchStatus.Ready -> runningDash ?: dash
    GraphBranchStatus.Alive, GraphBranchStatus.Merged -> null
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
