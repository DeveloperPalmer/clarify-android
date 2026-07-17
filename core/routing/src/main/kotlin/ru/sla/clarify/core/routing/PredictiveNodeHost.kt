package ru.sla.clarify.core.routing

import android.os.Build
import android.os.SystemClock
import android.view.RoundedCorner
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.kode.way.Event
import ru.kode.way.NavigationService
import ru.kode.way.NavigationState
import ru.kode.way.Node
import ru.kode.way.Path
import ru.kode.way.Region
import ru.kode.way.compose.ComposableNode
import ru.kode.way.compose.LocalNavigationService
import ru.kode.way.compose.LocalNodePath
import ru.kode.way.compose.NodeWithPath
import ru.kode.way.compose.defaultTransitionSpec
import ru.kode.way.toStepsReversed

/**
 * Drop-in замена для `ru.kode.way.compose.NodeHost`, добавляющая внутри приложения анимацию predictive back
 * в стиле системных настроек Android («full-screen surface»):
 *
 * - во время протяжки жеста «назад» на не-корневом экране активный (верхний) экран ужимается, скругляет углы и
 *   сдвигается вправо (жест всегда анимируется как свайп от правого края, без зеркальности); экран на уровень выше
 *   в стеке навигации ужимается синхронно, но стоит смещённым за экран влево, под scrim;
 * - при выполнении «назад» верхний экран исчезает фейдом, scrim снимается, а раскрытый экран выныривает — выезжает
 *   к центру и восстанавливает полный размер, — после чего вызывается [onDismissRequest] (ожидается [Event.Back]);
 * - на отмене верхний экран и scrim анимируются обратно в покой.
 *
 * На корневом экране приложения (раскрывать снизу нечего, см. [findBelowScreen]) обработчик отключён,
 * поэтому ОС играет свою собственную анимацию predictive back-to-home.
 *
 * «Экран снизу» — это ближайший предок-[ComposableNode] в активном регионе. Это опирается на принятое в приложении
 * соглашение навигации: каждый пункт назначения вложен под экран, из которого он открыт, поэтому цепочка предков
 * активного пути совпадает с визуальным back-стеком (и дефолтная back-навигация Way делает pop ровно к нему).
 *
 * Экраны-назначения должны рисовать непрозрачный фон (каждый экран на `ScreenScaffold` уже это делает): иначе
 * ещё непрозрачный уходящий экран будет просвечивать сквозь дыры экрана-назначения во время подмены.
 *
 * Predictive-прогресс отдаётся платформой только на Android 14+ (API 34); на более старых версиях жест
 * деградирует до мгновенного back без анимации.
 *
 * Активный узел рендерится через [AnimatedContent] с ключом по [Path] (см. [ActiveNode] — почему это важно
 * для памяти), состояние экранов живёт в [SaveableStateHolder], экранам предоставляются
 * [LocalNavigationService] и [LocalNodePath]. Не поддерживается только parallel-flow корень
 * (в приложении корень — обычный flow, рендерится активный узел единственного региона).
 *
 * @param service навигационный сервис Way, чьё активное состояние отображается и анимируется.
 * @param onDismissRequest вызывается, как только жест подтверждён (доведён до конца).
 * @param transitionSpec обычный (не жестовый) переход между экранами; при выполнении «назад» подавляется
 *   (см. [PredictiveBackController]).
 */
@Composable
fun PredictiveNodeHost(
  service: NavigationService<*>,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  transitionSpec: AnimatedContentTransitionScope<Path?>.() -> ContentTransform = defaultTransitionSpec
) {
  LaunchedEffect(service) {
    if (!service.isStarted()) {
      service.start()
    }
  }

  val scope = rememberCoroutineScope()
  val controller = remember { PredictiveBackController() }
  val deviceCornerPx = rememberDeviceCornerRadiusPx()

  val regionState = collectActiveRegion(service)
  val region = regionState.value

  val belowNode = remember(region) { region?.let(::findBelowScreen) }
  val activeNode = remember(region) { region?.let { NodeWithPath(it.active, it.activeNode) } }
  val activePath = region?.active

  // Верхний экран всё ещё тот, на котором начали жест (до того как «назад» подменит активный узел на нижний).
  // derivedStateOf: пересчитывается только при смене активного узла или флагов жеста, но НЕ на каждом кадре
  // протяжки (scale/touchY/backProgress читаются лишь в draw-фазе слоёв и сюда не входят).
  val isRevealing by remember(activePath, controller) {
    derivedStateOf { controller.isGestureActive && activePath != null && activePath == controller.draggedPath }
  }

  PredictiveBackHandler(belowNode != null) { events ->
    controller.beginGesture(activePath)
    try {
      events.collect { controller.applyDrag(it.progress, it.touchY) }
      if (controller.hasDragged) {
        controller.performBack(scope, onDismissRequest)
      } else {
        controller.performInstantBack(onDismissRequest)
      }
    } catch (cancellation: CancellationException) {
      controller.cancel(scope)
      throw cancellation
    }
  }

  // «Назад» выполнен и сменил активный узел на нижний экран — возвращаем контроллер в покой.
  LaunchedEffect(activePath) {
    if (controller.isNavigatingBack) {
      controller.settle()
    }
  }

  val saveableStateHolder = rememberSaveableStateHolder()
  CompositionLocalProvider(LocalNavigationService provides service) {
    Box(modifier = modifier.fillMaxSize()) {
      if (isRevealing && belowNode != null) {
        RevealedNode(
          below = belowNode,
          controller = controller,
          deviceCornerPx = deviceCornerPx
        )
        Scrim(
          controller = controller
        )
      }
      ActiveNode(
        active = activeNode,
        controller = controller,
        isRevealing = isRevealing,
        deviceCornerPx = deviceCornerPx,
        saveableStateHolder = saveableStateHolder,
        transitionSpec = transitionSpec
      )
    }
  }
}

/**
 * Раскрытая снизу нода — та, на которую вернёмся. Во время протяжки ужимается синхронно с верхним и стоит
 * смещённой за левый край; при выполнении «назад» выезжает к центру и восстанавливает полный размер
 * (см. [applyRevealedTransform]).
 *
 * graphicsLayer вешается на обёртку-[Box], а НЕ передаётся в `Content`: [ComposableNode.Content] в этом
 * проекте игнорирует переданный модификатор (экран сам задаёт свой размер), поэтому трансформация должна жить
 * на реальной обёртке, иначе не применится.
 *
 * Превью намеренно рендерится БЕЗ [SaveableStateHolder]: оно живёт одновременно с контентом [ActiveNode],
 * а [SaveableStateHolder] запрещает одновременную регистрацию одного ключа — на кадре подмены активного узла
 * общий ключ пути мог бы схлестнуться с записью холдера. Состояние превью и не нужно: когда экран станет
 * активным, [AnimatedContent] скомпонует его заново уже под холдером.
 */
@Composable
private fun RevealedNode(
  below: NodeWithPath,
  controller: PredictiveBackController,
  deviceCornerPx: Float,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .graphicsLayer { applyRevealedTransform(controller, deviceCornerPx) }
  ) {
    val node = below.node
    if (node is ComposableNode) {
      CompositionLocalProvider(LocalNodePath provides below.path) {
        node.Content(Modifier.fillMaxSize())
      }
    }
  }
}

/**
 * Затемнение поверх раскрытого экрана.
 * Держится постоянным во время протяжки и снимается до прозрачности при выполнении «назад»,
 * синхронно с фейдом верхнего экрана — так оба перетекают друг в друга через cross-fade.
 */
@Composable
private fun Scrim(
  controller: PredictiveBackController,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .graphicsLayer {
        alpha = MAX_SCRIM_ALPHA * (1f - controller.backProgress.value.coerceIn(0f, 1f))
      }
      .background(Color.Black)
  )
}

/**
 * Активная (верхняя) нода. graphicsLayer применяется к стабильному контейнеру [AnimatedContent] (через его`modifier`,
 * а не внутри content-слота), поэтому трансформация надёжно следует за прогрессом жеста и при этом
 * сохраняется собственная composition/state экрана.
 * При выполнении «назад» обычный переход подавляется ([noTransition]) — cross-fade уже рисуют слои выше,
 * а активный узел подменяется мгновенно под ним.
 *
 * [AnimatedContent] ключуется по [Path] — лёгкому значению, — а НЕ по [NodeWithPath].
 * Compose `Transition` удерживает предыдущее состояние в `segment.initialState`
 * до следующего перехода, поэтому ключ-[NodeWithPath] оставлял бы весь граф уже ушедшей ноды (DI-scope,
 * вьюмодели, дочерние ноды) в SlotTable композиции на всё время жизни хоста. Ноды для отрисовки разрешаются
 * через [nodeCache]; когда контент ключа окончательно покинул [AnimatedContent] (exit-анимация доиграла)
 * и ключ не активен, его кэш и запись [SaveableStateHolder] вычищаются — ушедшая нода становится доступной GC.
 */
@Composable
private fun ActiveNode(
  active: NodeWithPath?,
  controller: PredictiveBackController,
  isRevealing: Boolean,
  deviceCornerPx: Float,
  saveableStateHolder: SaveableStateHolder,
  transitionSpec: AnimatedContentTransitionScope<Path?>.() -> ContentTransform,
  modifier: Modifier = Modifier
) {
  // key → Node для состояний, которые AnimatedContent ещё может отрисовать (активная нода плюс уходящие).
  // Snapshot-backed, чтобы запись была видна чтению в content-лямбде.
  val nodeCache = remember { mutableStateMapOf<String, Node>() }
  // Ключи, смонтированные AnimatedContent прямо сейчас: добавляются на входе, убираются когда контент
  // (включая exit-анимацию) ушёл. Snapshot-backed, чтобы эффект очистки перезапускался при изменении.
  val mountedKeys = remember { mutableStateListOf<String>() }
  // Ключи, отданные SaveableStateHolder, — чтобы эффект очистки знал, что вычищать. Обычный (не snapshot)
  // set: читается только императивно внутри эффекта, не в композиции.
  val trackedKeys = remember { mutableSetOf<String>() }

  val activePath = active?.path
  val activeKey = activePath?.toSaveableKey()
  if (active != null && activeKey != null && nodeCache[activeKey] !== active.node) {
    // Guard, чтобы неизменившаяся нода не записывала лишнюю snapshot-мутацию на каждой рекомпозиции.
    nodeCache[activeKey] = active.node
  }

  AnimatedContent(
    modifier = modifier
      .fillMaxSize()
      .graphicsLayer { if (isRevealing) applyActiveTransform(controller, deviceCornerPx) },
    targetState = activePath,
    contentKey = { it?.toSaveableKey() },
    transitionSpec = { if (controller.isNavigatingBack) noTransition() else transitionSpec() },
    label = "PredictiveNodeHost"
  ) { path ->
    val key = path?.toSaveableKey()
    val node = key?.let { nodeCache[it] }
    if (path != null && key != null && node is ComposableNode) {
      DisposableEffect(key) {
        trackedKeys.add(key)
        mountedKeys.add(key)
        onDispose {
          mountedKeys.remove(key)
          nodeCache.remove(key)
        }
      }
      ComposableNodeContent(
        node = node,
        path = path,
        saveableStateHolder = saveableStateHolder
      )
    } else {
      // Flow-узел без собственного содержимого (или уже вычищенная нода) — пустой placeholder.
      Box(Modifier.fillMaxSize())
    }
  }

  // Чистим SaveableStateHolder для ключей, чей контент полностью покинул AnimatedContent и которые больше
  // не активны (сам Node из кэша уже сброшен в onDispose выше; здесь — ещё и подстраховка для него).
  // Именно СНАРУЖИ SaveableStateProvider: removeState изнутри content был бы отменён — sibling-эффекты
  // диспозятся child-before-parent, и dispose-time saveState() провайдера тут же вернул бы запись.
  LaunchedEffect(mountedKeys.toList(), activeKey) {
    trackedKeys.toList()
      .filter { it != activeKey && it !in mountedKeys }
      .forEach { key ->
        saveableStateHolder.removeState(key)
        nodeCache.remove(key)
        trackedKeys.remove(key)
      }
  }
}

/**
 * Рендерит [ComposableNode.Content] под записью [SaveableStateHolder] с ключом пути, отдавая путь
 * через [LocalNodePath].
 *
 * Внимание: переданный в `Content` модификатор экраны этого проекта фактически игнорируют (экран сам задаёт
 * свой размер, см. `BasicWiredComposableScreen.Content`). Любые трансформации экрана вешайте на внешнюю
 * обёртку, а не сюда.
 */
@Composable
private fun ComposableNodeContent(
  node: ComposableNode,
  path: Path,
  saveableStateHolder: SaveableStateHolder,
  modifier: Modifier = Modifier
) {
  saveableStateHolder.SaveableStateProvider(path.toSaveableKey()) {
    CompositionLocalProvider(LocalNodePath provides path) {
      node.Content(modifier.fillMaxSize())
    }
  }
}

/**
 * Инъективный ключ [SaveableStateHolder] для пути узла: полный [ru.kode.way.Segment.id] каждого сегмента,
 * а НЕ [Path.toString] — тот склеивает `Segment.name` и отбрасывает дизамбигуатор `@graphId:file`, из-за чего
 * два разных кросс-модульных пути схлопнулись бы в один ключ.
 */
private fun Path.toSaveableKey(): String = segments.joinToString(".") { it.id }

/**
 * Держит всё анимируемое состояние жеста predictive back и инкапсулирует его жизненный цикл — протяжку,
 * выполнение «назад», отмену и возврат в покой. Хост лишь дёргает методы из обработчика жеста и читает значения
 * для отрисовки слоёв.
 *
 * Выполнение «назад» и отмена запускаются на внешнем [CoroutineScope] хоста, а не на корутине самого жеста:
 * её платформа отменяет в момент релиза, а доиграть финальную анимацию нужно уже после этого.
 */
@Stable
private class PredictiveBackController {

  /** Scale-прогресс [0;1]: им управляет палец во время протяжки, при выполнении «назад» дожимается до 1. */
  val scaleProgress = Animatable(0f)

  /** Прогресс перехода «назад» [0;1] (cross-fade): 0 во время протяжки, 1 после релиза за порогом. */
  val backProgress = Animatable(0f)

  /** Вертикальная позиция пальца в пикселях — за ней следуют оба экрана. */
  var touchY: Float by mutableFloatStateOf(0f)
    private set

  /** Идёт ли predictive-жест: взводится на первом кадре протяжки и держится до конца финальной анимации. */
  var isGestureActive: Boolean by mutableStateOf(false)
    private set

  /** Пришёл ли хоть один кадр протяжки. false → «назад» был мгновенным (кнопка навбара / API < 34). */
  var hasDragged: Boolean by mutableStateOf(false)
    private set

  /** Идёт ли выполнение «назад» (активный узел вот-вот сменится на нижний экран). */
  var isNavigatingBack: Boolean by mutableStateOf(false)
    private set

  /** Путь экрана, с которого стартовал жест, — чтобы отличать «до» и «после» подмены активного узла. */
  var draggedPath: Path? by mutableStateOf(null)
    private set

  /** Момент старта текущего жеста (uptime, мс) — от него отмеряется задержка активации predictive-слоёв. */
  private var gestureStartMs: Long = 0L

  /**
   * Сырой `progress` платформы в момент активации жеста. Из-за задержки активации к этому моменту палец уже
   * немного протянул (progress ~0.05–0.1), поэтому прогресс ремапится в диапазон [activationProgress; 1] → [0; 1]:
   * scale стартует ровно с 0 (экран полноразмерный, без скачка) и плавно растёт по мере дальнейшей протяжки.
   */
  private var activationProgress: Float = 0f

  /**
   * Готовит контроллер к новому жесту: запоминает экран старта, момент старта и сбрасывает прогресс.
   * [isGestureActive] здесь НЕ взводится — predictive-слои включаются лишь когда жест окажется осознанной
   * протяжкой (см. [applyDrag]), иначе мгновенный «назад» кнопкой или быстрый флик ужимали бы экраны зря.
   */
  suspend fun beginGesture(path: Path?) {
    isNavigatingBack = false
    hasDragged = false
    draggedPath = path
    gestureStartMs = SystemClock.uptimeMillis()
    scaleProgress.snapTo(0f)
    backProgress.snapTo(0f)
  }

  /**
   * Применяет текущий кадр протяжки. Жест активируется (включает predictive-слои) лишь когда он оказывается
   * осознанной протяжкой — продлился дольше [GESTURE_ACTIVATION_DELAY_MS] И набрал прогресс выше
   * [DRAG_ACTIVATION_THRESHOLD]. Так отсекаются оба не-predictive случая:
   * - системная кнопка «назад» (3-кнопочная навигация) шлёт один кадр `onBackStarted` с `progress == 0`;
   * - быстрый флик-свайп края длится ~30–40 мс и завершается мгновенным back.
   * Реальная протяжка длится ~200 мс и набирает заметный прогресс, поэтому активирует жест и анимируется.
   */
  suspend fun applyDrag(progress: Float, touchY: Float) {
    val dragProgress = progress.coerceIn(0f, 1f)
    if (!hasDragged) {
      val elapsedMs = SystemClock.uptimeMillis() - gestureStartMs
      if (elapsedMs < GESTURE_ACTIVATION_DELAY_MS || dragProgress <= DRAG_ACTIVATION_THRESHOLD) {
        return
      }
      hasDragged = true
      isGestureActive = true
      activationProgress = dragProgress
    }
    this.touchY = touchY
    val span = 1f - activationProgress
    val remapped = if (span > 0f) {
      ((dragProgress - activationProgress) / span).coerceIn(0f, 1f)
    } else {
      1f
    }
    scaleProgress.snapTo(DECELERATE.transform(remapped))
  }

  /**
   * Подтверждает жест и выполняет «назад»: параллельно дожимает scale и проигрывает cross-fade (верхний фейдит,
   * scrim снимается), затем вызывает [onBack], чтобы смена активного узла произошла уже за завершённой анимацией.
   */
  fun performBack(scope: CoroutineScope, onBack: () -> Unit) {
    scope.launch {
      val animationSpec = tween<Float>(
        easing = DECELERATE,
        durationMillis = BACK_ANIM_MS
      )
      launch { scaleProgress.animateTo(1f, animationSpec) }
      backProgress.animateTo(1f, animationSpec)
      isNavigatingBack = true
      onBack()
    }
  }

  /**
   * Мгновенный «назад» без predictive-анимации: протяжки не было ([hasDragged] == false), predictive-слои не
   * показывались и [isGestureActive] остался false, поэтому экраны не трансформированы. Просто отдаём навигацию —
   * обычный переход доиграет [AnimatedContent], как в штатном `NodeHost` (размеры экранов не меняются).
   */
  fun performInstantBack(onBack: () -> Unit) {
    onBack()
  }

  /** Отменяет жест: возвращает scale в покой; навигация при этом не происходит. */
  fun cancel(scope: CoroutineScope) {
    scope.launch {
      scaleProgress.animateTo(targetValue = 0f, animationSpec = tween(CANCEL_ANIM_MS))
      isGestureActive = false
    }
  }

  /** Сбрасывает контроллер в покой после того, как «назад» сменил активный узел на нижний экран. */
  suspend fun settle() {
    isNavigatingBack = false
    isGestureActive = false
    scaleProgress.snapTo(0f)
    backProgress.snapTo(0f)
  }
}

/**
 * Находит ближайшего предка-[ComposableNode] активного узла — экран, который раскрылся бы при back-навигации.
 * Возвращает `null`, когда активный экран — корень приложения (выше только flow-узлы); в этом случае back-жест
 * нужно отдать системе (back-to-home).
 */
private fun findBelowScreen(region: Region): NodeWithPath? {
  return region.active.toStepsReversed()
    .drop(1) // пропускаем сам активный узел
    .firstOrNull { region.nodes[it] is ComposableNode }
    ?.let { path -> NodeWithPath(path, region.nodes.getValue(path)) }
}

/**
 * Трансформация активного (верхнего) экрана: по прогрессу протяжки ужимает (1 → [TOP_MIN_SCALE]), скругляет углы
 * (от фактического радиуса дисплея [deviceCornerPx] до [MAX_CORNER_DP], без скачка на старте) и сдвигает к правому
 * краю, тянет за пальцем по вертикали; по прогрессу «назад» делает фейд. Во время протяжки (backProgress == 0)
 * экран остаётся полностью непрозрачным — меняются только scale и позиция.
 */
private fun GraphicsLayerScope.applyActiveTransform(
  controller: PredictiveBackController,
  deviceCornerPx: Float
) {
  val dragProgress = controller.scaleProgress.value.coerceIn(0f, 1f)
  val backProgress = controller.backProgress.value.coerceIn(0f, 1f)
  val scale = lerp(1f, TOP_MIN_SCALE, dragProgress)
  scaleX = scale
  scaleY = scale
  translationX = topShiftPx() * dragProgress
  translationY = verticalFraction(controller.touchY) * topShiftYPx() * dragProgress
  alpha = 1f - backProgress
  shape = RoundedCornerShape(lerp(deviceCornerPx, MAX_CORNER_DP.dp.toPx(), dragProgress))
  clip = true
}

/**
 * Трансформация раскрытого (нижнего) экрана: во время протяжки ужимается синхронно с верхним и стоит за левым
 * краем; при выполнении «назад» ([backProgress] 0 → 1) восстанавливает scale до 1, выезжает к центру и
 * распрямляет углы — до фактического радиуса дисплея [deviceCornerPx], а не до острых углов.
 */
private fun GraphicsLayerScope.applyRevealedTransform(
  controller: PredictiveBackController,
  deviceCornerPx: Float
) {
  val dragProgress = controller.scaleProgress.value.coerceIn(0f, 1f)
  val backProgress = controller.backProgress.value.coerceIn(0f, 1f)
  val scale = lerp(lerp(1f, TOP_MIN_SCALE, dragProgress), 1f, backProgress)
  scaleX = scale
  scaleY = scale
  translationX = -belowShiftXPx() * (1f - backProgress)
  translationY = verticalFraction(controller.touchY) * topShiftYPx() * (1f - backProgress)
  shape = RoundedCornerShape(lerp(MAX_CORNER_DP.dp.toPx(), deviceCornerPx, backProgress))
  clip = true
}

/** Горизонтальный сдвиг верхнего экрана к краю свайпа (в пикселях) на полном прогрессе протяжки. */
private fun GraphicsLayerScope.topShiftPx(): Float {
  return (size.width / 20f - 8.dp.toPx()).coerceAtLeast(0f)
}

/** Вертикальный размах следования за пальцем (в пикселях) на полном прогрессе протяжки. */
private fun GraphicsLayerScope.topShiftYPx(): Float {
  return (size.height / 20f - 8.dp.toPx()).coerceAtLeast(0f)
}

/** Горизонтальное смещение раскрытого экрана за левый край экрана (в пикселях). */
private fun GraphicsLayerScope.belowShiftXPx(): Float {
  return size.width * BELOW_SHIFT_X_FRACTION
}

/** Позиция пальца по вертикали как доля в [-1; 1] относительно центра экрана (вверх — отрицательно). */
private fun GraphicsLayerScope.verticalFraction(touchY: Float): Float {
  val half = size.height / 2f
  return if (half <= 0f) 0f else ((touchY - half) / half).coerceIn(-1f, 1f)
}

/**
 * Подписывается на активный регион навигации и отдаёт его как [State]. Приложение использует одну область
 * навигации, поэтому берётся единственный активный регион.
 */
@Composable
private fun collectActiveRegion(service: NavigationService<*>): State<Region?> {
  return produceState(initialValue = null, service) {
    val listener = { state: NavigationState ->
      value = state.regions.values.first()
    }
    service.addTransitionListener(listener)
    awaitDispose { service.removeTransitionListener(listener) }
  }
}

/**
 * Фактический радиус скругления углов дисплея устройства в пикселях — стартовое значение углов экрана,
 * чтобы в начале протяжки они совпадали с реальными углами девайса
 *
 * Берётся максимум по всем четырём углам (на некоторых устройствах верхние и нижние различаются). API
 * [RoundedCorner] доступен с API 31; на более старых версиях и на устройствах без скругления возвращается 0
 * — прежнее поведение (острые углы). Predictive-анимация и так играет лишь на API 34+.
 */
@Composable
private fun rememberDeviceCornerRadiusPx(): Float {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return 0f
  // Без remember: insets могут быть ещё не доставлены на первой композиции, а к моменту жеста хост
  // рекомпозится (isRevealing → true) уже с прикреплённым view, поэтому читаем актуальное значение.
  val insets = LocalView.current.rootWindowInsets
  return listOf(
    RoundedCorner.POSITION_TOP_LEFT,
    RoundedCorner.POSITION_TOP_RIGHT,
    RoundedCorner.POSITION_BOTTOM_LEFT,
    RoundedCorner.POSITION_BOTTOM_RIGHT
  )
    .mapNotNull { insets?.getRoundedCorner(it)?.radius }
    .maxOrNull()
    ?.toFloat()
    ?: 0f
}

// Easing STANDARD_DECELERATE, рекомендованный для прогресса predictive back (PathInterpolator(0, 0, 0, 1)).
private val DECELERATE = CubicBezierEasing(0f, 0f, 0f, 1f)

// Минимальный прогресс протяжки, с которого считаем жест реальным predictive-свайпом. Нулевой кадр
// onBackStarted от системной кнопки «назад» (3-кнопочная навигация) остаётся ниже порога и не запускает анимацию.
private const val DRAG_ACTIVATION_THRESHOLD = 0.01f

// Сколько жест должен длиться, прежде чем считать его осознанной протяжкой и включить predictive-слои.
private const val GESTURE_ACTIVATION_DELAY_MS = 100L

private const val TOP_MIN_SCALE = 0.9f
private const val BELOW_SHIFT_X_FRACTION = 0.15f
private const val MAX_SCRIM_ALPHA = 0.8f
private const val MAX_CORNER_DP = 32f
private const val BACK_ANIM_MS = 250
private const val CANCEL_ANIM_MS = 220
