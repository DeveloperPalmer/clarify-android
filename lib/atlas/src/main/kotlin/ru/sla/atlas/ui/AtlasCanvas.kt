package ru.sla.atlas.ui

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.CanvasMargins
import ru.sla.atlas.entity.Graph
import ru.sla.atlas.entity.Lanes
import ru.sla.atlas.entity.Node
import ru.sla.atlas.entity.NodeAccent
import ru.sla.atlas.layout.lanesOf
import ru.sla.atlas.lod.counterScaleOf

/**
 * Полотно графа: фон, узлы и связи между ними, по которому можно панорамировать.
 *
 * Композабл здесь ничего не считает — только композирует, принимает жест и рисует. Где узлы стоят,
 * считает раскладка; камеру и её результат держит [AtlasCanvasState]; события пальцев разбирает
 * [detectCameraGestures].
 *
 * **Чем нарисованы фон и связи, полотно не знает.** И то, и другое — слоты: фон встаёт под слоем
 * камеры, связи рисуются внутри него, а какого они цвета, толщины и штриха, решает вызывающий.
 * Полотно отвечает за то, где они окажутся, и за то, что рисование не стоит ни измерения, ни
 * рекомпозиции.
 *
 * Масштаб — свойство камеры, а не раскладки: он применяется слоем и потому не стоит ни измерения,
 * ни рекомпозиции. **Уровень детализации** — свойство камеры тоже, но раскладку он меняет: на обзоре
 * зазоры и дорожки вчетверо теснее, а узел вырождается в глиф. Поэтому уровень снимается в
 * композиции один раз и уходит и в содержимое узлов, и в измерение — тем же порядком, что и сам
 * список узлов, и по той же причине.
 *
 * На кадре перехода раскладка **уже целевая**, а уходящее представление рисуется поверх неё, места
 * не занимая, см. [LevelCrossfade].
 *
 * @param state камера полотна и результат его последней раскладки
 * @param branchColors цвет каждой ветки графа, магистраль включая: палитру полотно не читает, а
 *   получает готовой
 * @param flingDecay кривая затухания броска
 * @param crossfadeSpec кривая кроссфейда при смене уровня детализации; мгновенная подмена — это
 *   `snap`, и решает это вызывающий: полотно не знает, отказался ли зритель от движения
 * @param modifier модификатор корня полотна
 * @param accentOwnerOf за какую ветку говорит узел: у обычного — за свою, у узла ветвления и узла
 *   слияния — за ту, что от него ушла или в него вернулась. Рода узлов знает только вызывающий
 * @param background что нарисовать под слоем камеры
 * @param drawEdges чем нарисовать связи; зовётся внутри слоя камеры, поэтому линии масштабируются
 *   вместе с узлами
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
 *   раскладки — полотно ставит их по одному на узел и считает по позиции, а не по идентификатору
 */
@Suppress("LongParameterList")
@Composable
fun <N : Node, L> AtlasCanvas(
  state: AtlasCanvasState<N, L>,
  branchColors: Map<Branch.Id, Color>,
  flingDecay: DecayAnimationSpec<Float>,
  crossfadeSpec: FiniteAnimationSpec<Float>,
  modifier: Modifier = Modifier,
  blocked: () -> Boolean = { false },
  accentOwnerOf: (graph: Graph<N>, node: N, own: Branch.Id) -> Branch.Id = { _, _, own -> own },
  background: @Composable () -> Unit = { },
  drawEdges: DrawScope.() -> Unit = { },
  node: @Composable (node: N, accent: NodeAccent, level: L) -> Unit,
  overlay: @Composable BoxScope.(onBoundsChanged: (key: Any, bounds: Rect) -> Unit) -> Unit = { }
) {
  val telemetry = state.telemetry
  // Жест не пересоздаётся при смене состояния: ключ `Unit` держит обработчик живым, а свежий
  // экземпляр приходит через rememberUpdatedState. Иначе первое же входящее сообщение отменяло бы
  // драг под пальцем.
  val currentState by rememberUpdatedState(state)
  // Спека обновляется через rememberUpdatedState: `pointerInput(Unit)` не пересоздаётся, и смена
  // плотности иначе заморозила бы внутри жеста кривую от старого экрана.
  val currentDecay by rememberUpdatedState(flingDecay)
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
  val graph = state.graph
  val nodes = graph.nodes
  // Дорожки считаются здесь, а не в измерении, и это не оптимизация. Точке ветвления нужен цвет и
  // направление **уходящей** ветки, а рисуется она в композиции — то есть до того, как измерение
  // что-либо посчитает. Один и тот же результат уходит и в содержимое, и в `layout`, поэтому
  // разъехаться им нечем.
  val lanes = remember(graph, branchColors) {
    lanesOf(graph, branchColors) { graphNode, own -> accentOwnerOf(graph, graphNode, own) }
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
          // Двойной тап живёт только на фоне: тап по узлу жест полотна не досматривает вовсе —
          // узел потребляет отпускание своим `clickable`, и детектор выходит на потреблённом
          // событии, не запомнив его.
          onDoubleTap = { currentState.fitAll() }
        )
      }
  ) {
    background()
    AtlasNodesLayer(
      state = state,
      graph = graph,
      lanes = lanes,
      branchColors = branchColors,
      crossfadeSpec = crossfadeSpec,
      drawEdges = drawEdges,
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
 * Слой узлов: узлы, связи между ними и камера, двигающая их все разом.
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
 * @param crossfadeSpec кривая кроссфейда при смене уровня
 * @param drawEdges чем нарисовать связи
 * @param modifier модификатор слоя
 * @param node содержимое узла на заданном уровне детализации
 */
@Suppress("LongParameterList")
@Composable
private fun <N : Node, L> AtlasNodesLayer(
  state: AtlasCanvasState<N, L>,
  graph: Graph<N>,
  lanes: Lanes,
  branchColors: Map<Branch.Id, Color>,
  crossfadeSpec: FiniteAnimationSpec<Float>,
  drawEdges: DrawScope.() -> Unit,
  modifier: Modifier = Modifier,
  node: @Composable (node: N, accent: NodeAccent, level: L) -> Unit
) {
  val telemetry = state.telemetry
  val statusBar = WindowInsets.statusBars
  val navigationBar = WindowInsets.navigationBars
  // Уровень снимается один раз и уходит и в содержимое, и в измерение: прочитанный в measure заново,
  // он пришёл бы к узел, построенным другим уровнем, — ровно та же ловушка, что и со списком
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
  // Доля заводится на время перехода и привязана к **уходящему** уровню, а не к текущей цели.
  // Привязка к цели — это `if (frameLevel == level)`, где `level` и есть `targetState`: условие
  // истинно на любом кадре, целевым значением анимации выходит постоянная единица, и гаснуть
  // уходящему представлению не из чего. Так и было — кроссфейд не рисовался ни разу, а удвоенное
  // дерево узлов полотно держало все двести пятьдесят миллисекунд честно.
  //
  // Анимация, заведённая в этой точке, берёт начальное значение у `currentState`, то есть у
  // уходящего уровня, и получает единицу, а целевое — у цели, и получает ноль. Ровно это и нужно.
  val exit = previousLevel?.let { outgoingLevel ->
    LevelExit(
      level = outgoingLevel,
      alpha = transition.animateFloat(
        transitionSpec = { crossfadeSpec },
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
        // Связи рисуются внутри того же слоя, поэтому масштабируются вместе с узлами: на 0.4×
        // линия истончается до 0.8 dp, на 2.5× толстеет до пяти, а штрих пунктира растягивается
        // вместе с ней. Одно правило на весь слой проще двух; если тонкая линия потеряется на
        // устройстве, обратный ход — поделить толщину на масштаб прямо здесь.
        telemetry.onEdgeDraw()
        drawEdges()
      },
    content = {
      graph.nodes.fastForEachIndexed { index, graphNode ->
        key(graphNode.id.value) {
          SideEffect { telemetry.onNodeComposition() }
          LevelCrossfade(
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
      // под системными барами, и увести из-под них узлы обязаны поля — но чем эти бары высоки,
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
 * Кроссфейд представлений одного узла при смене уровня детализации (уровня детализации).
 *
 * Коробку задаёт **целевое** представление, поэтому раскладка на кадре перехода уже целевая: кадра,
 * где зазоры одного уровня, а узлы другого, не существует вовсе. Уходящее представление рисуется
 * поверх и места в раскладке не занимает — то же правило, по которому здесь живут гало
 * непрочитанного, подпись фронта и чип слияния.
 *
 * Готовый `Crossfade` для этого не годится, и это проверено по его исходникам: он кладёт оба
 * состояния в один `Box`, то есть коробка узла все 250 мс равна большему из двух — зазоры уже сжаты,
 * а узел ещё во всю ширину, и в конце анимации раскладка защёлкивается. Кроссфейд, заведённый
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
 * перехода: слой камеры к этому кадру уже приземлился на новый масштаб, и узел внутри него
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
private fun <L> LevelCrossfade(
  exit: LevelExit<L>?,
  exitScale: State<Float>,
  scale: State<Float>,
  incoming: @Composable () -> Unit,
  outgoing: @Composable (level: L) -> Unit
) {
  Box(contentAlignment = Alignment.Center) {
    incoming()
    if (exit != null) {
      Box(
        modifier = Modifier
          // Коробку узла задаёт целевое представление, а уходящее только рисуется поверх.
          .drawnOnly()
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
