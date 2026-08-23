# Камера полотна: проекты и судейство

Рабочие материалы фазы проектирования инерции камеры. Выводы сведены в
[chronology-camera-fling.md](chronology-camera-fling.md) — читать надо его; этот файл нужен, когда
понадобилось **основание** вывода, а не сам вывод.

Как это делалось. Три подхода спроектированы независимо друг от друга, каждый по одной и той же
разведке и с требованием подкреплять любое утверждение об API строкой из распакованных исходников, а
не правдоподобием. Каждый обязан был сам назвать свои слабые места. Затем три судьи — по линзе на
судью, каждый видел все три проекта — искали, чем подход **опровергнуть**, а не за что похвалить:
комплимент без доказательства не засчитывался, найденный дефект с воспроизведением засчитывался.
Синтез сделан по правилу вето: блокирующий дефект по любой линзе снимает подход с дистанции
независимо от суммы баллов.

Подход C заведён **заведомо слабым** — не для победы, а чтобы разница между подходами была измерена,
а не заявлена. Он свою роль отработал: его численные таблицы оказались самой аккуратной частью
раунда и исправили четыре места в разведке.

Итог: 25 против 20.5 и 12.5, победила своя скалярная физика (подход B). Блокирующих дефектов у B нет
ни по одной линзе, у A два, у C три.

**Что здесь не надо искать.** Это снимок рассуждения, а не спецификация. Где проект и вердикт
расходятся — прав вердикт; где вердикт расходится с
[chronology-camera-fling.md](chronology-camera-fling.md) — прав основной документ, он писался
последним. Проекты A и C проиграли и приводятся целиком намеренно: без них не видно, чем именно
заплачено за выбор, и следующий, кому покажется, что «надо было взять `scrollable2D`», прочитает
здесь, что с ним не так на этом экране.

## Оглавление

**Часть I. Проекты**

- Подход A — `scrollable2D` + платформенный `FlingBehavior`
- Подход B — своя скалярная физика *(победитель)*
- Подход C — покомпонентное затухание *(foil)*

**Часть II. Вердикты судей**

- Линза «физика» — держится ли направление, что на границе, в углу, при вырожденном диапазоне
- Линза «риск» — доказаны ли API, что при апдейте библиотеки, что ломается молча
- Линза «конвенции» — тестируемость, фазовая дисциплина, раскладка модуля, дом-стиль

---

# Часть I. Проекты

## Подход A: `scrollable2D` + `Scrollable2DState` + платформенный `FlingBehavior`

Всё про API прочитано в распакованном foundation **1.10.0** (`scratchpad/f1100/`), ссылки — файл:строка.

### 1. Арифметика

Новый файл `components/canvas/GraphCamera.kt`. Отход от разведки: она предлагала положить это в
`GraphGeometry.kt`, но там «где стоят узлы», а тут «где может быть камера и как на неё ложится
дельта» — другая тема и другой тест. `panRangeOf`/`timelinePanRangeOf` остаются на месте и
вызываются отсюда, `GraphGeometryTest` не трогается.

Три сущности в `entity/` (по файлу на класс):

```kotlin
// entity/CameraRange.kt
@Immutable
data class CameraRange(
  val x: ClosedFloatingPointRange<Float>,
  val y: ClosedFloatingPointRange<Float>
) {
  /** Положение камеры в покое: начало истории в центре. */
  val resting: Offset get() = Offset(x.endInclusive, y.endInclusive)

  fun clamp(camera: Offset): Offset =
    Offset(camera.x.coerceIn(x), camera.y.coerceIn(y))
}

// entity/CameraStep.kt
@Immutable
data class CameraStep(val camera: Offset, val consumed: Offset)

// entity/CameraDrive.kt
/** Кто ведёт камеру: палец или затухание. Правило потребления у них разное. */
enum class CameraDrive { Gesture, Fling }
```

Две чистые функции:

```kotlin
internal fun cameraRangeOf(placement: GraphPlacement, viewport: IntSize): CameraRange {
  if (placement.isEmpty) return CameraRange(0f..0f, 0f..0f)
  return CameraRange(
    x = timelinePanRangeOf(placement.centreSpanX, viewport.width.toFloat()),
    y = panRangeOf(placement.bounds.top, placement.bounds.bottom, viewport.height.toFloat())
  )
}

internal fun cameraStepOf(
  camera: Offset,
  delta: Offset,
  range: CameraRange,
  drive: CameraDrive
): CameraStep {
  val moved = range.clamp(camera + delta)
  val applied = moved - camera
  if (drive == CameraDrive.Gesture) return CameraStep(camera = moved, consumed = applied)
  if (applied.x == 0f && applied.y == 0f) return CameraStep(camera = moved, consumed = Offset.Zero)
  return CameraStep(camera = moved, consumed = delta)
}
```

Семантика возврата опирается на два места платформы: `Scroll2DScope.scrollBy` обязан вернуть
потреблённое (`Scrollable2DState.kt:84`), а гашение — единственное, по сравнению модуля с запросом:
`return consumedOffset.toMagnitudeFloat()` (`Scrollable2D.kt:451`) против
`if (abs(delta - consumed) > 0.5f) this.cancelAnimation()` (`Scrollable.kt:1053`). Разложение
`pixels.toDecomposedOffset()` сохраняет модуль (`cos²+sin²=1`), поэтому «вернул ровно `delta`» =
«модули совпали» = fling жив.

### 2. Держатель `GraphCanvasState`

```kotlin
@Stable
internal class GraphCanvasState {
  private var graphNodes by mutableStateOf(emptyList<GraphNode>())

  // Камера хранится уже зажатой. Незажатого сдвига больше нет: он и был мёртвой зоной.
  private var camera by mutableStateOf(Offset.Zero)

  // Только один смысл: «камеру вели хотя бы раз».
  private var isMoved by mutableStateOf(false)

  private var viewport by mutableStateOf(IntSize.Zero)
  private var placement by mutableStateOf(GraphPlacement.Empty)

  // Обычное поле, не снапшот: читается только внутри лямбды потребления.
  private var drive = CameraDrive.Gesture

  val telemetry = GraphCanvasTelemetry()

  /** Фабрика не composable, поэтому владеет им держатель. */
  val scroll: Scrollable2DState = Scrollable2DState { delta -> consume(delta) }

  val offset: State<Offset> = derivedStateOf {
    if (isMoved) camera else cameraRangeOf(placement, viewport).resting
  }

  private fun consume(delta: Offset): Offset {
    val range = cameraRangeOf(placement, viewport)
    val from = if (isMoved) camera else range.resting
    val step = cameraStepOf(camera = from, delta = delta, range = range, drive = drive)
    camera = step.camera
    isMoved = true
    telemetry.onCamera(delta = delta, consumed = step.consumed, drive = drive)
    return step.consumed
  }

  internal fun setDrive(value: CameraDrive) { drive = value }
}
```

- `Scrollable2DState(...)` — обычная функция-фабрика (`Scrollable2DState.kt:104`), не composable.
  Внутри `DefaultScrollable2DState` уже есть `MutatorMutex`, поэтому палец, положенный на летящее
  полотно, отменяет затухание, а входящее сообщение — нет.
- `pan()` исчезает. Чтение `placement`/`viewport` в `consume` безопасно: она исполняется в корутине
  `scroll {}`, а не в фазе Compose.
- `isMoved` теряет второй смысл («raw засеян») — засевать больше нечего. Третий смысл («ведёт
  анимация») стал отдельным `drive`. Защёлку `isMoved` **оставляю**.
- `layout()` пере-зажимает камеру, считая диапазон от аргументов, а не от снапшот-полей:

```kotlin
  viewport = viewportSize
  placement = result
  if (isMoved) {
    val clamped = cameraRangeOf(result, viewportSize).clamp(camera)
    if (clamped != camera) camera = clamped
  }
  telemetry.onMeasure()
```

### 3. Composable `GraphCanvas`

```kotlin
  val overscroll = rememberOverscrollEffect()
  val flingBehavior = ScrollableDefaults.flingBehavior()
  val cameraFling = remember(flingBehavior, state) {
    CameraFlingBehavior(flingBehavior, state::setDrive)
  }

  Box(
    modifier = modifier
      .clipToBounds()
      .scrollable2D(state = state.scroll, overscrollEffect = overscroll, flingBehavior = cameraFling)
      .overscroll(overscroll)
  )
```

Уходят: `pointerInput(Unit)`, `detectDragGestures`, `rememberUpdatedState(state)` и
**`change.consume()`**. Последнее — не потеря: `DragGestureNode` сам делает `dragEvent.consume()`
при пересечении slop (`Draggable.kt:891`) и на каждом последующем событии (`Draggable.kt:1010`).

`PredictiveBackHandler` (`PredictiveNodeHost.kt:119`) от этого не страдает и не выигрывает: он висит
на системном predictive-back, а не на pointer-input Compose — краевой свайп система забирает до
Compose. Поэтому «полотно увидит не все горизонтальные драги» остаётся верным и после замены, просто
это уже не про потребление событий.

Побочно приезжает `startDragImmediately() = shouldScrollImmediately()` (`Scrollable2D.kt:205`,
`:468`): касание во время инерции ловит её без slop.

Декоратор — отдельным файлом `canvas/CameraFlingBehavior.kt`:

```kotlin
internal class CameraFlingBehavior(
  private val delegate: FlingBehavior,
  private val onDriveChanged: (CameraDrive) -> Unit
) : FlingBehavior {
  override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
    onDriveChanged(CameraDrive.Fling)
    try {
      return with(delegate) { performFling(initialVelocity) }
    } finally {
      onDriveChanged(CameraDrive.Gesture)
    }
  }
}
```

Флаг взводится вовремя: `performFling` вызывается **внутри** `scroll { }` (`Scrollable2D.kt:456`),
то есть до первого `scrollBy`.

Почему `ScrollableDefaults.flingBehavior()`, а не `flingBehavior = null`: при `null` узел сам
подставляет `DefaultFlingBehavior(splineBasedDecay(UnityDensity))` и чинит плотность в
`onAttach`/`onDensityChange` (`Scrollable2D.kt:164`, `:246`, `:257`) — кривая та же, но обернуть его
нельзя. Публичный путь к наблюдаемому экземпляру один: `ScrollableDefaults.flingBehavior()` →
`rememberPlatformDefaultFlingBehavior()` → `DefaultFlingBehavior(rememberSplineBasedDecay<Float>())`
(`Scrollable.android.kt:29-32`) — ровно то же, что под `LazyColumn` в `ChatCommits.kt`.

Overscroll: `rememberOverscrollEffect()` (`Overscroll.kt:344`), эффект отдаётся **и** в
`scrollable2D` (события), **и** в `Modifier.overscroll` (отрисовка) — `Overscroll.kt:258-265` прямо
говорит, что второе само событий не обрабатывает. Порядок модификаторов — предложение по смыслу
обёртки; порядок отрисовки stretch в коде не проверен, это пункт для устройства.

Токен `flingDecay()` в `AppMotion` **не заводится**: спека приезжает платформенной, и токен, который
никто не читает, — ложное обещание настройки.

### 4. Телеметрия

`panEvents` расщепляется на три счётчика (`GraphTelemetry` 9 → 11 полей):

| поле | строка | предел | смысл |
|---|---|---|---|
| `gesturePans` | `pan` | `FRAME_LIMIT` | дельты от пальца |
| `flingPans` | `fling` | `FRAME_LIMIT` | дельты от затухания |
| `blockedFlings` | `wall` | `IDLE_LIMIT` | шаги fling'а, где потреблено ноль |

```kotlin
internal fun onCamera(delta: Offset, consumed: Offset, drive: CameraDrive) {
  if (drive == CameraDrive.Gesture) {
    gesturePans++
    lastPan = delta   // затухание больше не перетирает «last pan» нулями
  } else {
    flingPans++
    if (consumed == Offset.Zero) blockedFlings++
  }
}
```

Различение состояний:
- **идёт fling** — `fling` и `layer` тикают вместе, `wall` в нуле;
- **fling молотит в стенку** — `fling ≈ fps` при стоящем `layer`; под подходом A это невозможно по
  построению, поэтому строка `wall` — сторож самого контракта;
- **упор при живом пальце** — `pan` тикает, `layer` стоит, `wall` в нуле (гашение при жесте не
  считается: рука у края даёт легитимные 60 отказов в секунду). Сигнал этого случая — overscroll.

Ломается `GraphCanvasTelemetryTest.«every counter is converted, not just the first»`
(`GraphCanvasTelemetryTest.kt:36-52`): девять именованных аргументов. Плюс `GraphTelemetry.Empty`
9 → 11 нулей, три строки в `ratesOf`, три в `toPhaseRows`; `VISIBLE_ROWS` в `GraphDebugOverlay.kt:175`
поднять с 10 до 11.

### 5. Развилка: диагональ, упёршаяся одной осью

**Выбор: поглощать — но только под затуханием.** `drive == Fling` и хоть одна ось сдвинулась →
потреблено `delta` целиком; ни одна не сдвинулась → `Offset.Zero`. Под пальцем — честное `applied`.

Причины:
1. Вырожденная ось Y — норма, а не край: `panRangeOf` при одной дорожке отдаёт центрирующую точку
   (`GraphGeometry.kt:180-181`), и **любой** наклонный флик под платформенным правилом умирал бы на
   первом шаге. «360» выполнялось бы ровно для одного угла.
2. Поглощать под пальцем нельзя: stretch применяется только к остатку и только при
   `source == UserInput` (`AndroidOverscroll.android.kt:644`). Забрав дельту, мы убили бы
   единственный сигнал «дальше истории нет».
3. Угол в стену останавливает намертво: обе оси не сдвинулись → `Zero` → `abs(delta-consumed)>0.5` →
   `cancelAnimation` → остаток скорости уходит в `applyToFling` и подсвечивает оба края
   (`AndroidOverscroll.android.kt:719-742`).

Цена явно: на кадре, где свободная ось сама упирается, потреблённое завышено на подкадровую
величину; и скольжение вдоль стенки короче чистого флика той же скорости — путь меряется по
диагонали, а не по свободной оси.

### 6. Зависимости

foundation не объявлен нигде, приезжает транзитивно через material3 1.4.0 как **1.10.0** при
`compose = 1.11.4`. Цепочка: `chronology:ui → uikit → core:ui`, `core/ui/build.gradle:13` отдаёт
`api libs.bundles.android.ui`. Правка в двух местах `gradle/libs.versions.toml`:

```toml
[libraries]
compose-foundation = { module = "androidx.compose.foundation:foundation", version.ref = "compose" }

[bundles]
android-ui = [ "compose-ui", "compose-ui-util", "compose-foundation", ... ]
```

**Честно о непроверенном:** foundation 1.11.4 в кэше Gradle **отсутствует** (там 1.6.7 / 1.9.4 /
1.9.5 / 1.10.0 / 1.10.2 / 1.10.6), тогда как `ui-android` и `animation-android` 1.11.4 лежат. Что
1.11.4 существует у foundation — вывод из синхронности релиз-поезда AndroidX, а не факт с диска.
И весь разбор `Scrollable2D` сделан по **1.10.0**, то есть правка каталога уводит код на версию,
исходники которой никто не читал.

Безопасный вариант на первый шаг: `composeFoundation = "1.10.0"` собственным version-ref'ом — делает
явным то, что и так резолвится, ничего не меняет в поведении и снимает зависимость от чужого графа.
Подъём до `compose` — отдельным решением, с перечитыванием `Scrollable2D.kt` (в 1.9.5 механизм
гашения был другим — `shouldCancelFling`/`isFlingContinuationAtBoundsEnabled`, в 1.10.0 его уже нет).

Опт-инов не требуется: `Modifier.scrollable2D` помечен только `@Stable` (`Scrollable2D.kt:83-84`).

### 7. Тесты

`GraphCameraTest.kt` (JUnit 5, предложением в бэктиках):
- `consumed delta equals the request while the camera has room`
- `consumed delta is truncated at the boundary`
- `a gesture into the wall leaves the whole delta to the overscroll`
- `a fling slides along a wall it has hit` — главный тест решения §5
- `a fling stops when neither axis can move`
- `a degenerate range consumes nothing`
- `a diagonal fling keeps its direction while both axes have room` — дельты подаются так же, как их
  раскладывает платформа (`Scrollable2D.kt:408-415`); накопленное `dy/dx` постоянно и равно `tan θ`
- `a camera outside a narrowed range is pulled back into it`
- `a resting camera moves from the centred value, not from zero`

`GraphCanvasTelemetryTest.kt` — правится существующий и добавляется
`a fling blocked at the wall is counted apart from the gesture`.

Чего **не** покрыть: в бандле `unittest` нет ни compose-ui-test, ни Robolectric — связка
«модификатор ↔ состояние ↔ fling behavior» проверяется только руками на устройстве.

### 8. Слабые места подхода — как есть

1. **Управление приватной эвристикой снаружи.** «Верни больше, чем сдвинул, и fling выживет» — не
   контракт `Scrollable2DState`, а следствие двух строк реализации (`Scrollable2D.kt:451` +
   `Scrollable.kt:1053`). Между 1.9.5 и 1.10.0 механизм уже менялся. Компилятор молчит при смене.
2. **Мы врём nested scroll.** Поглощённая дельта на упёршейся оси уходит вверх как потреблённая.
   Сегодня родителя-скроллера нет; появится — поверит в потребление, которого не было.
3. **Признак «идёт fling» едет на декораторе-наблюдателе.** Работает потому, что `performFling`
   зовут внутри `scroll {}`. `dispatchRawDelta` (`Scrollable2DState.kt:156`) флаг обходит.
4. **Правка каталога — общесистемная ради одного экрана**, и целится в версию, исходники которой
   никто не читал.
5. **Отход от платформы, который нельзя проверить здесь.** Скольжение вдоль стенки — то самое, что
   либо ложится в руку, либо нет.
6. **Дыра в покрытии там, где живёт предсказанный дефект.** Провал «fling молотит в стенку» — провал
   проводки, а проводка не тестируема имеющимся инструментом.
7. **Диагональный fling в одну стенку не светится.** Плата за решение §5.
8. **`canScroll` остаётся всегда `true`** — фабрика зашивает (`Scrollable2DState.kt:163`).
   Состояние говорит о себе неправду; починка требует реализовать интерфейс руками с `MutatorMutex`.
9. **`GraphCanvas` обрастает сборкой сотрудников** — три `remember`/composable-вызова вместо одного
   `pointerInput`.
10. **Порядок `clipToBounds`/`scrollable2D`/`overscroll` выведен из смысла обёртки, не из кода
    отрисовки stretch** — не проверено.

---

## Подход B: своя скалярная физика

### 0. Поправка к разведке

Утверждение «обе перегрузки `detectDragGestures` отдают `onDragEnd` без параметров» — **неверно**.
В foundation 1.10.0 есть вторая, публичная перегрузка:

```kotlin
// f1100/commonMain/.../gestures/DragGestureDetector.kt:231-241
@OptIn(ExperimentalFoundationApi::class)
suspend fun PointerInputScope.detectDragGestures(
  orientationLock: Orientation?,
  onDragStart: (down: PointerInputChange, slopTriggerChange: PointerInputChange, overSlopOffset: Offset) -> Unit = { _, _, _ -> },
  onDragEnd: (change: PointerInputChange) -> Unit = {},   // ← up-событие
  onDragCancel: () -> Unit = {},
  shouldAwaitTouchSlop: () -> Boolean = { true },
  onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
)
```

Подтверждено в байткоде через `javap` по `foundation.aar/classes.jar` 1.10.0. Однопараметрическая
перегрузка — делегат ко второй (`:177-183`).

Следствие: **жест почти не переписывается**. Нужен только `VelocityTracker`.

Вторая поправка: внутри этой перегрузки `change.consume()` уже вызывается самой foundation —
в цикле `drag(...) { onDrag(it, it.positionChange()); it.consume() }` (`:308-312`) и при пересечении
слопа (`:264`). Значит `change.consume()` в `GraphCanvas.kt:71` — дубль. И от
`PredictiveBackHandler` он не защищает: тот `OnBackPressedCallback`, а не конкурент по
pointer-событиям. Аргумент «consume обязан остаться» держится на том, что жест не должен утечь
в родителя, а не на соседстве с back.

### 1. Арифметика

#### `entity/GraphCameraRange.kt`

```kotlin
@Immutable
data class GraphCameraRange(
  val x: ClosedFloatingPointRange<Float>,
  val y: ClosedFloatingPointRange<Float>
) {
  /** Где камера стоит, пока её не двигали. */
  val rest: Offset get() = Offset(x.endInclusive, y.endInclusive)

  companion object { val Empty = GraphCameraRange(0f..0f, 0f..0f) }
}
```

#### `entity/GraphPanStep.kt`

```kotlin
@Immutable
data class GraphPanStep(val camera: Offset, val consumed: Offset)
```

#### `components/canvas/GraphCamera.kt` — три чистые функции

```kotlin
internal fun cameraRangeOf(placement: GraphPlacement, viewport: IntSize): GraphCameraRange {
  if (placement.isEmpty) return GraphCameraRange.Empty
  return GraphCameraRange(
    x = timelinePanRangeOf(placement.centreSpanX, viewport.width.toFloat()),
    y = panRangeOf(placement.bounds.top, placement.bounds.bottom, viewport.height.toFloat())
  )
}

/**
 * Кламп стоит на записи, а не на чтении, и это не перестановка мест. Накапливая незажатый сдвиг,
 * состояние банкует перерегулирование: упор на три тысячи пикселей превращается в мёртвую зону
 * такой же величины, которая сама не рассасывается и вылезает телепортом при следующей раскладке.
 */
internal fun panStepOf(camera: Offset, delta: Offset, range: GraphCameraRange): GraphPanStep {
  val moved = Offset(
    x = (camera.x + delta.x).coerceIn(range.x),
    y = (camera.y + delta.y).coerceIn(range.y)
  )
  return GraphPanStep(camera = moved, consumed = moved - camera)
}

/** Упёрлась ли камера по всем осям, которые несут бросок. */
internal fun isCameraStuck(camera: Offset, direction: Offset, range: GraphCameraRange): Boolean =
  isAxisStuck(camera.x, direction.x, range.x) && isAxisStuck(camera.y, direction.y, range.y)

private fun isAxisStuck(
  camera: Float,
  direction: Float,
  range: ClosedFloatingPointRange<Float>
): Boolean {
  // Нулевая компонента направления и вырожденный диапазон — одно и то же: нести бросок этой оси
  // нечем. Условие смотрит на направление, а не на дельту кадра, поэтому нулевая дельта первого
  // кадра затухания не читается как упор.
  if (direction == 0f || range.start >= range.endInclusive) return true
  return if (direction > 0f) camera >= range.endInclusive else camera <= range.start
}
```

#### `components/canvas/FlingDirection.kt`

```kotlin
/**
 * Затухание одномерное — по модулю скорости, — а скалярный путь раскладывается по осям этим
 * вектором. Платформа (Scrollable2D.kt:408-414) раскладывает через угол:
 * `abs(cos(atan2(vy, vx)) * s) * sign(vx)`. Здесь то же самое без тригонометрии:
 * `cos(atan2(vy, vx)) == vx / |v|`, а `abs` с `sign` восстанавливают знак — выражение тождественно
 * компоненте единичного вектора, включая `vx == 0` (`sign(0f) == 0f`).
 */
internal class FlingDirection(velocity: Velocity) {
  val magnitude: Float = sqrt(velocity.x * velocity.x + velocity.y * velocity.y)

  val vector: Offset =
    if (magnitude > 0f) Offset(velocity.x / magnitude, velocity.y / magnitude) else Offset.Zero

  fun offsetOf(distance: Float): Offset = vector * distance
}
```

### 2. Держатель

Уходят `rawOffsetX`/`rawOffsetY` и ветка засева в `pan()` — камера хранится уже зажатой.

```kotlin
@Stable
internal class GraphCanvasState {
  private var graphNodes by mutableStateOf(emptyList<GraphNode>())

  // `mutableStateOf` со структурным сравнением гасит пере-зажатие вхолостую.
  private val camera = mutableStateOf(Offset.Zero)
  private var isMoved by mutableStateOf(false)
  private var viewport by mutableStateOf(IntSize.Zero)
  private var placement by mutableStateOf(GraphPlacement.Empty)
  private var cameraRange by mutableStateOf(GraphCameraRange.Empty)

  private var flingJob: Job? = null   // обычное поле: за него никто не рисует

  val telemetry = GraphCanvasTelemetry()
  val offset: State<Offset> = camera   // derivedStateOf больше не нужен
```

```kotlin
  fun pan(delta: Offset): Offset {
    val consumed = applyPan(delta)
    telemetry.onPan(delta)
    return consumed
  }

  /**
   * Scope приходит параметром и держателю не принадлежит — как у [PredictiveBackController]:
   * корутина жеста умирает в момент релиза, а затухать надо уже после неё.
   * Спека тоже приходит параметром: она зависит от плотности, о которой держатель не знает,
   * и подменяется в тестах на ту, что не тянет Android-фреймворк.
   */
  fun fling(scope: CoroutineScope, velocity: Velocity, decay: DecayAnimationSpec<Float>) {
    stopFling()
    val direction = FlingDirection(velocity)
    // Порог платформенный и он не про UX: spline берёт логарифм скорости и ниже единицы отдаёт NaN
    // (DefaultFlingBehavior: `if (abs(initialVelocity) > 1f)`). Здесь же отсекается NaN от трекера.
    if (direction.magnitude.isNaN() || direction.magnitude <= 1f) return
    telemetry.onFlingStart(velocity)
    flingJob = scope.launch { runFling(direction, decay) }
  }

  fun stopFling() { flingJob?.cancel(); flingJob = null }

  private suspend fun runFling(direction: FlingDirection, decay: DecayAnimationSpec<Float>) {
    // Системный множитель длительности к инерции не применяется: это физика жеста, а не переход.
    // rememberCoroutineScope() наследует MotionDurationScale от рекомпозера окна
    // (WindowRecomposer.android.kt:333-336), поэтому без обёртки «Animator duration scale»
    // в опциях разработчика менял бы пролёт. DefaultFlingBehavior поступает так же.
    withContext(FlingDurationScale) {
      var travelled = 0f
      AnimationState(initialValue = 0f, initialVelocity = direction.magnitude)
        .animateDecay(decay) {
          val delta = direction.offsetOf(value - travelled)
          travelled = value
          telemetry.onFlingStep(delta, applyPan(delta))
          if (isCameraStuck(camera.value, direction.vector, cameraRange)) cancelAnimation()
        }
    }
  }

  private fun applyPan(delta: Offset): Offset {
    isMoved = true
    val step = panStepOf(camera.value, delta, cameraRange)
    camera.value = step.camera
    return step.consumed
  }
```

```kotlin
private val FlingDurationScale = object : MotionDurationScale {
  override val scaleFactor: Float get() = 1f
}
```

#### Хвост `layout()`

```kotlin
    viewport = viewportSize
    placement = result
    val range = cameraRangeOf(result, viewportSize)
    cameraRange = range
    camera.value = if (isMoved) panStepOf(camera.value, Offset.Zero, range).camera else range.rest
    telemetry.onMeasure()
    return result
```

Три свойства бесплатно: пере-зажатие — тот же `panStepOf`, что и панорамирование; запись из фазы
измерения безопасна (структурное сравнение делает неизменившееся пере-зажатие no-op, читатели —
`graphicsLayer` фазы рисования и листовая панель); раскладка посреди инерции доходит немедленно,
потому что цикл читает `cameraRange` каждый кадр, а не снимок на старте.

#### `isMoved`

Остаётся, теряет смысл «сдвиг засеян». Смысл «камерой управляет анимация» **в снапшот-состоянии не
заводится вовсе**: за него никто не рисует, он нужен только панели и живёт счётчиками телеметрии.
Защёлкивание навсегда — продуктовый дефект, к инерции отношения не имеющий; не чинится, но место
для будущего сброса теперь видно.

### 3. Жест

```kotlin
  val currentState by rememberUpdatedState(state)
  val currentDecay by rememberUpdatedState(AppTheme.motion.flingDecay())
  val flingScope = rememberCoroutineScope()
  …
      .pointerInput(Unit) {
        val tracker = VelocityTracker()
        detectDragGestures(
          orientationLock = null,
          onDragStart = { down, _, _ ->
            currentState.stopFling()
            tracker.resetTracking()
            tracker.addPointerInputChange(down)
          },
          onDrag = { change, dragAmount ->
            tracker.addPointerInputChange(change)
            currentState.pan(dragAmount)
          },
          onDragEnd = { up ->
            tracker.addPointerInputChange(up)
            // Кламп по осям, как у платформы (Draggable.kt:1057-1060): у трекера полиномиальная
            // подгонка, дрожание перед отпусканием умеет отдать десятки тысяч px/s, а у spline
            // пролёт растёт как v^1.736.
            val maximum = viewConfiguration.maximumFlingVelocity
            currentState.fling(
              scope = flingScope,
              velocity = tracker.calculateVelocity(Velocity(maximum, maximum)),
              decay = currentDecay
            )
          },
          onDragCancel = { tracker.resetTracking() }
        )
      }
```

Проверено: `VelocityTracker()`, `calculateVelocity(maximumVelocity: Velocity)`, `resetTracking()` —
`ui 1.11.4 .../pointer/util/VelocityTracker.kt:49, 88, 92`; `addPointerInputChange` — `:110`, `:130`;
`viewConfiguration` в `PointerInputScope` — `SuspendingPointerInputFilter.kt:84`;
`maximumFlingVelocity: Float` в px/s — `platform/ViewConfiguration.kt:57-59`.

### 4. Гашение на границе

Критерий свой и **не про дельту, а про диапазон**: `isCameraStuck(camera, direction, range)`.

Почему не платформенное `abs(delta - consumed) > 0.5f`:
1. У платформы оно работает на **скаляре** — `magnitude ≥ 0`, монотонно растёт, знаки в `sign()`.
   Свернуть потреблённое обратно в модуль значит принять платформенную развилку диагонали (§6).
2. Признак «дельту отвергли» ломается на нулевой дельте. Первый кадр `animateDecay` приходит
   с `playTime == 0`, то есть `delta == Offset.Zero`. И та же формула для «ось не участвует
   в броске» не работает: чисто горизонтальный флик у стенки никогда не отдаст `delta.y != 0`.

| Ситуация | `isAxisStuck(x)` | `isAxisStuck(y)` | Итог |
|---|---|---|---|
| середина полотна | false | false | едет |
| диагональ, X упёрлась, Y свободна | true | false | **едет вдоль стенки** |
| диагональ, Y вырождена | false | true | едет по X |
| горизонтальный флик у левого края | true | true (`direction.y == 0f`) | **встал** |
| нулевой кадр в начале | false | false | едет |

Полпикселя остаётся только в телеметрии — там нужно, потому что `(camera + delta) - camera != delta`
при большой камере и малой дельте:

```kotlin
private fun isStalled(requested: Float, consumed: Float): Boolean =
  abs(requested - consumed) > 0.5f
```

### 5. Телеметрия

Два новых счётчика: `flingSteps` (кадры затухания; `panEvents` становится только жестом) и
`flingStalls` (кадры затухания, где камера взяла меньше запрошенного; кадры жеста намеренно не
попадают — палец за краем тикает каждый кадр, и это норма).

```kotlin
  internal fun onPan(delta: Offset) { panEvents++; lastPan = delta }

  var lastFlingVelocity: Velocity = Velocity.Zero
    private set

  internal fun onFlingStart(velocity: Velocity) { lastFlingVelocity = velocity }

  internal fun onFlingStep(delta: Offset, consumed: Offset) {
    flingSteps++
    if (isStalled(delta.x, consumed.x) || isStalled(delta.y, consumed.y)) flingStalls++
  }
```

Строки: `phaseRow("fling", flingSteps, rates.flingSteps, FRAME_LIMIT)`,
`phaseRow("stuck", flingStalls, rates.flingStalls, IDLE_LIMIT)`. `stuck` берёт `IDLE_LIMIT = 8`:
при рабочем критерии §4 затухание может простоять максимум один кадр на флик, поэтому устойчивый
поток означает, что `isCameraStuck` сломан.

| Состояние | `pan` | `fling` | `layer` | `stuck` |
|---|---|---|---|---|
| покой | 0 | 0 | 0 | 0 |
| палец тянет | ≈fps | 0 | ≈fps | 0 |
| палец тянет за краем | ≈fps | 0 | 0 | 0 |
| идёт инерция | 0 | ≈fps | ≈fps | 0 |
| **инерция молотит в стенку** | 0 | ≈fps | 0 | **≈fps, красным** |

Плюс факт-строка `fling v` с `lastFlingVelocity` — по ней пролёт сверяется с таблицей на устройстве.
`GraphCanvasTelemetryTest` и `GraphTelemetry.Empty` перестают компилироваться — по назначению.
`GraphDebugOverlay.VISIBLE_ROWS = 10` поднять до 11.

### 6. Развилка «диагональ, упёршаяся одной осью»

**Инерция жива, пока её несёт хоть одна ось** — `&&` в `isCameraStuck`. Осознанный отход от
платформы: `scrollable2D` сворачивает потреблённое в модуль (`Scrollable2D.kt:451`), поэтому
диагональ гаснет целиком, стоит упереться одной оси; на этом экране при одной дорожке вертикаль не
потребляет ничего, и любой слегка наклонный флик умирал бы мгновенно. Платформенный вариант — `||`,
одна правка и один тест. Причина записана в KDoc `isCameraStuck`, где решение и живёт.

Цена: после первой стенки траектория перестаёт быть прямой и едет вдоль края. Проверять на устройстве.

### 7. Токен в `AppMotion`

```kotlin
  /**
   * Затухание броска: та же кривая, по которой останавливается любой список в приложении.
   * Порт `android.widget.Scroller`, а не «что-нибудь затухающее». Пролёт растёт как v^1.736:
   * 1000 dp/s → 194 dp за 555 мс, 2000 dp/s → 647 dp за 925 мс. `exponentialDecay` под эту кривую
   * не подгоняется ни одним множителем трения.
   */
  @Composable
  fun flingDecay(): DecayAnimationSpec<Float> = rememberSplineBasedDecay()
```

`rememberSplineBasedDecay<T>()` — `SplineBasedFloatDecayAnimationSpec.android.kt:41-49`, публичная,
пересоздаётся при смене `density.density`. По `workflow.md` новый токен одобрения не требует.

Запасной путь: `PointerInputScope` наследует `Density`, поэтому `splineBasedDecay<Float>(this)`
строится прямо в `onDragEnd`. Токен лучше: делает «как в чате» утверждением дизайн-системы.

### 8. Тесты

Главное преимущество: **вся физика — чистые функции, а цикл затухания гоняется на JVM без Compose
UI, Robolectric и устройства**, потому что `BroadcastFrameClock` лежит в `compose-runtime`
(`BroadcastFrameClock.kt:36`, `sendFrame` — `:70`), `kotlinx-coroutines-test` уже в бандле
`unittest` (`libs.versions.toml:118`), а `animateDecay` берёт часы из контекста корутины.

`GraphCameraTest.kt`: `the camera range spans from the first plate centred to the last`;
`an empty graph has nowhere to pan`; `consumed delta equals the request while the camera has room`;
`consumed delta is truncated at the boundary`; `consumed delta is zero on a saturated axis`;
`a degenerate range consumes nothing`; **`a rejected delta cannot be banked for later`** (уехать
в стенку на 3000, вернуться на 100, камера обязана сдвинуться ровно на 100 — тот самый дефект
разведки без Compose); `banked overshoot cannot outlive a layout pass`;
`a fling survives a wall while the other axis has room`;
`a fling dies when the only live axis hits its wall`;
`a fling on a degenerate axis dies at the opposite wall`; `a fling is not stuck before it has moved`.

`FlingDirectionTest.kt`: `magnitude is the speed regardless of direction`;
`a decomposed distance keeps the release angle`; `a decomposed distance keeps its length`;
**`the decomposition matches the platform angle formula`** (сетка по четырём квадрантам против
`abs(cos(atan2(vy,vx)) * d) * sign(vx)` — доказательство эквивалентности, прибитое тестом);
`a zero velocity has no direction`; `a NaN velocity has no magnitude`.

`GraphFlingTest.kt` — цикл целиком, `BroadcastFrameClock` + `exponentialDecay()`:

```kotlin
runTest {
  val clock = BroadcastFrameClock()
  val state = GraphCanvasState().apply { layout(…) }
  state.fling(this + clock, Velocity(1200f, 700f), exponentialDecay())
  var nanos = 0L
  while (clock.hasAwaiters) {
    nanos += 16_666_666L
    clock.sendFrame(nanos)
    runCurrent()
    trail += state.offset.value
  }
}
```

`a diagonal fling keeps its direction`; `a fling stops at the boundary instead of hammering it`;
`a fling slides along the wall it hit`; `a new touch cancels the running fling`;
**`a fling ignores the system animation duration scale`**; `a slow release does not fling at all`.

`GraphCanvasStateTest.kt`: `a layout pass re-clamps the stored camera`;
`an untouched camera follows new nodes`; `a touched camera stays where the user left it`.

**Чего тесты не покрывают:** `SplineBasedFloatDecayAnimationSpec` инициализируется от
`platformFlingScrollFriction = ViewConfiguration.getScrollFriction()` (`.android.kt:38`, top-level
`val`). Без Robolectric и без `returnDefaultValues` её в JVM-тесте не построить. Значит тесты гоняют
`exponentialDecay` и проверяют **структуру**, а **кривую** сверяет владелец на устройстве. То же
ограничение у любого подхода — но у `scrollable2D` кривую гарантирует один и тот же вызов
`ScrollableDefaults.flingBehavior()`, а здесь — только то, что я передал правильную спеку.

Ни один тест не запускался: это проект, а не реализация.

### 9. Слабые места — честно

1. **Растяжение у края потеряно.** Самая крупная потеря. `OverscrollEffect` (`Overscroll.kt:55-56`)
   звать руками можно, но это ещё два контракта — `applyToScroll(delta, source) { … }` и
   `applyToFling(velocity) { … }`. У `scrollable2D` это параметр. На полотне без скроллбара
   растяжение — единственный сигнал «дальше истории нет».
2. **Nested scroll нет вовсе.** Цена нулевая сегодня; станет ненулевой, когда внутри узла появится
   длинный скроллящийся текст или полотно ляжет в bottom sheet.
3. **Разведение источников — гонка, которой у платформы нет.** Вместо `MutatorMutex` с приоритетами
   — `flingJob?.cancel()`, асинхронная отмена: один кадр затухания может примениться уже после
   `onDragStart`. И «программного» источника — перелёта камеры к узлу, на который намекает
   `largeTween()` — нет вовсе.
4. **Смена ведущего пальца портит скорость.** `detectDragGestures` при подъёме отслеживаемого пальца
   переключается на другой, а `VelocityTracker` получает позицию нового как продолжение траектории
   старого. У платформы та же дыра, но починка (сброс трекера при смене `change.id`) — на мне,
   и в проекте её нет.
5. **Пинч-зум не разбирается.** `detectDragGestures` однопальцевый. Зум заявлен в KDoc `GraphCanvas`;
   когда появится, жест придётся переписывать вместе со скоростью.
6. **Объём переписанного — не ноль.** ≈45 строк арифметики, ≈40 цикла и отмены, ≈25 жеста,
   ≈25 телеметрии, токен; плюс ≈150 строк тестов. Против `scrollable2D` исчезли бы жест и цикл —
   около 65 строк. То есть своя физика стоит примерно 65 строк и один класс.
7. **Четыре платформенных решения повторены руками, и компилятор их не проверит:** порог `> 1f`,
   кламп по `maximumFlingVelocity`, `MotionDurationScale` со scaleFactor 1, терпимость 0.5 px.
   Забытый duration scale делает пролёт зависимым от опций разработчика; забытый кламп пускает
   в spline 30 000 px/s. Из четырёх тестами ловится один.
8. **Кламп на записи никто не заставляет сделать.** У `Scrollable2DState` контракт лямбды — входной
   билет: не вернув потреблённое, не скомпилируешься. Здесь единственная страховка — тест.
9. **Развилка диагонали — отход от платформы, непроверенный на устройстве.** Обратный ход — замена
   `&&` на `||`.
10. **От foundation подход не избавляет.** `detectDragGestures`, `Box`, `BasicText`, `verticalScroll`
    в этих файлах уже из foundation 1.10.0. Проблема «foundation 1.10.0 при остальном 1.11.4»
    не устраняется, только сужается: расходиться теперь может детектор жеста, а не физика.

### Проверено по исходникам

`AnimationState(initialValue: Float, initialVelocity: Float)` — animation-core 1.11.4
`AnimationState.kt:272-278`; `AnimationState.animateDecay` — `SuspendAnimation.kt:181-185`;
`AnimationScope.cancelAnimation()` — `AnimationState.kt:172`; `splineBasedDecay<T>(density)` —
animation 1.11.4 `SplineBasedDecay.kt:124`; `rememberSplineBasedDecay<T>()` —
`SplineBasedFloatDecayAnimationSpec.android.kt:41`; `exponentialDecay<T>()` —
`DecayAnimationSpec.kt:104`; `VelocityTracker` — ui 1.11.4 `VelocityTracker.kt:49, 88, 92, 110`;
`MotionDurationScale` — `MotionDurationScale.kt:35`; `maximumFlingVelocity` —
`platform/ViewConfiguration.kt:57-59`; `PointerInputScope.viewConfiguration` —
`SuspendingPointerInputFilter.kt:84`; `Offset.getDistance()`, `times(Float)` — ui-geometry
`Offset.kt:122, 193`; `Velocity` — ui-unit `Velocity.kt:38-46`; `BroadcastFrameClock.sendFrame` —
runtime `BroadcastFrameClock.kt:36, 70`; `detectDragGestures(orientationLock, …)` — foundation 1.10.0
`DragGestureDetector.kt:231-241` + `javap`; MotionDurationScale в контексте рекомпозера окна —
`WindowRecomposer.android.kt:333-336`.

Классpath: `compose-animation` (1.11.4) в бандле `android-ui` (`libs.versions.toml:142`), который
`core/ui` отдаёт через `api` (`core/ui/build.gradle:13`), а `uikit` — через `api project(":core:ui")`.
`testImplementation` наследует `implementation`.

**Не проверял:** ни один тест не запускался; работоспособность `BroadcastFrameClock` + `runTest`
в этой конфигурации выведена из API, а не из прогона. `getScrollFriction() == 0.015f` не подтверждено
на диске. Развилка диагонали и совпадение пролёта с таблицей требуют устройства.

---

## Подход C (foil): покомпонентное 2D-затухание

`Animatable`/`AnimationState` над `Offset` + `splineBasedDecay<Offset>()` + свой `VelocityTracker`.

### 1. Механизм

`splineBasedDecay<Offset>(density)` = `SplineBasedFloatDecayAnimationSpec(density).generateDecayAnimationSpec()`
(`SplineBasedDecay.kt:124-125`). `generateDecayAnimationSpec` отдаёт `DecayAnimationSpecImpl`, тот на
`vectorize()` — `VectorizedFloatDecaySpec` (`DecayAnimationSpec.kt:114-123`). Внутри:

```kotlin
// DecayAnimationSpec.kt:137-141 — значение
for (i in 0 until valueVector.size)
    valueVector[i] = floatDecaySpec.getValueFromNanos(playTimeNanos, initialValue[i], initialVelocity[i])
// DecayAnimationSpec.kt:149-156 — длительность
maxDuration = maxOf(maxDuration, floatDecaySpec.getDurationNanos(initialValue[i], initialVelocity[i]))
```

**Две независимые одномерные анимации на общем таймере, длительность — максимум из двух.** Разведка
права: «одна 2D-анимация или две 1D» — вопрос без содержания.

Закрытая форма проверена численно портом `AndroidFlingSpline` (`SplineBasedDecay.kt:31-105`):

```
fc = friction·9.80665·39.37·density·160·0.84
D  = ln(0.78)/ln(0.9) = 2.3582018154259448
n  = D/(D−1)  = 1.736267606656372     distance ∝ |v|^n
m  = 1/(D−1)  = 0.7362676066563721    duration ∝ |v|^m     (n = m + 1)
```

Таблица спеки воспроизведена точно (density 1.0/2.0/2.75/3.5 дают одно и то же в dp/мс):
300 dp/s → 24.0 dp / 228 мс; 1000 → 194.3 dp / 555 мс; 2000 → 647.4 dp / 924 мс;
5000 → 3177.6 dp / 1815 мс; 8000 → 7186.4 dp / 2566 мс. В разведке длительности на 1 мс больше:
`flingDuration` делает `.toLong()`, то есть **усечение**, а не округление (`FlingCalculator.kt:62`).

### 2. Проект целиком

#### 2.1 Чистые функции

```kotlin
// entity/CameraRange.kt
@Immutable
internal data class CameraRange(
  val x: ClosedFloatingPointRange<Float>,
  val y: ClosedFloatingPointRange<Float>
) {
  val resting: Offset get() = Offset(x.endInclusive, y.endInclusive)
}

// entity/CameraStep.kt
@Immutable
internal data class CameraStep(val camera: Offset, val consumed: Offset)

// GraphGeometry.kt — оба рядом с timelinePanRangeOf/panRangeOf
internal fun cameraRangeOf(placement: GraphPlacement, viewport: IntSize): CameraRange
internal fun cameraStepOf(camera: Offset, delta: Offset, range: CameraRange): CameraStep
```

`cameraStepOf` клампит **поосно** и возвращает потреблённое поосно.

#### 2.2 `GraphCanvasState`

```kotlin
private var cameraX by mutableFloatStateOf(0f)   // уже зажато, не raw
private var cameraY by mutableFloatStateOf(0f)
private var isCameraOwned by mutableStateOf(false)
private var flingJob: Job? = null

val offset: State<Offset> = derivedStateOf {
  val range = cameraRangeOf(placement, viewport)
  if (isCameraOwned) Offset(cameraX, cameraY) else range.resting
}

fun pan(delta: Offset): Offset
fun fling(scope: CoroutineScope, velocity: Velocity, decay: DecayAnimationSpec<Offset>)
fun stopFling()
fun layout(...): GraphPlacement   // + пере-кламп сохранённой камеры
```

**Хранить зажатое, а не raw.** KDoc `GraphCanvasState.kt:59-60` объясняет raw тем, что «понадобится
для оттяжки за край и для затухания инерции». Для этого подхода не понадобится: оттяжку делает
`OverscrollEffect` своим состоянием, а затуханию нужен сигнал отказа, а не банк.

#### 2.3 Затухание

```kotlin
fun fling(scope: CoroutineScope, velocity: Velocity, decay: DecayAnimationSpec<Offset>) {
  flingJob?.cancel()
  flingJob = scope.launch {
    var previous = Offset.Zero
    telemetry.onFlingStart()
    try {
      AnimationState(
        typeConverter = Offset.VectorConverter,
        initialValue = Offset.Zero,
        initialVelocity = Offset(velocity.x, velocity.y)
      ).animateDecay(decay) {
        val requested = value - previous
        previous = value
        val consumed = pan(requested)
        telemetry.onFlingFrame(requested, consumed)
        // Гасим только когда обе оси не потребили ничего: ось Y вырождена при одной дорожке.
        if (consumed == Offset.Zero && requested != Offset.Zero) cancelAnimation()
      }
    } finally { telemetry.onFlingEnd() }
  }
}
```

Три решения, каждое подтверждено исходником:

- **`AnimationState.animateDecay`, а не `Animatable.animateDecay`.** У `Animatable` блок имеет тип
  `Animatable<T,V>.() -> Unit` (`Animatable.kt:278`) — `cancelAnimation()` из него не позвать,
  а `stop()` — `suspend` (`Animatable.kt:402`). У `AnimationState.animateDecay`
  (`SuspendAnimation.kt:181-198`) блок — `AnimationScope<T,V>.() -> Unit`, и `cancelAnimation()`
  там есть (`AnimationState.kt:172-175`). Цена: теряется `MutatorMutex` из `Animatable`
  (`Animatable.kt:299`), развод жеста и анимации становится моей заботой.
- **`Animatable.updateBounds` брать нельзя, хотя он ровно про границы.** KDoc: «Animation will stop
  as soon as **any** dimension specified in lowerBound is reached» (`Animatable.kt:99-118`),
  реализация — `clampToBounds` → `cancelAnimation()` → `BoundReached` (`Animatable.kt:308-321`).
  При одной дорожке `panRangeOf` возвращает вырожденный диапазон (`GraphGeometry.kt:180-181`),
  то есть `lower.y == upper.y`; любой флик, отклонённый от горизонтали хоть на градус, сдвигает y
  на первом же кадре и **убивает всю анимацию мгновенно**. Штатный механизм границ несовместим
  с вырожденной осью — это стоит знать всем трём подходам.
- **Анимируется накопитель от `Offset.Zero`, а не сама камера** — иначе рассинхрон с `layout()`.

#### 2.4 Жест

```kotlin
.pointerInput(Unit) {
  val tracker = VelocityTracker()
  val maxVelocity = viewConfiguration.maximumFlingVelocity
  detectDragGestures(
    orientationLock = null,
    onDragStart = { down, _, _ ->
      currentState.stopFling(); tracker.resetTracking(); tracker.addPointerInputChange(down)
    },
    onDragEnd = { change ->
      tracker.addPointerInputChange(change)
      currentState.fling(
        scope, tracker.calculateVelocity(Velocity(maxVelocity, maxVelocity)).toValidVelocity(), decay
      )
    },
    onDragCancel = { tracker.resetTracking() },
    onDrag = { change, dragAmount ->
      change.consume()
      tracker.addPointerInputChange(change)
      currentState.pan(dragAmount)
    }
  )
}
```

**Уточнение к разведке.** «Обе перегрузки `detectDragGestures` отдают `onDragEnd` без параметров» —
неверно для foundation 1.10.0: перегрузка с `orientationLock` объявляет
`onDragEnd: (change: PointerInputChange) -> Unit` (`DragGestureDetector.kt:240`, KDoc `:201`).
Функция публичная, `@OptIn` на ней — use-site. Свой `awaitEachGesture` писать не нужно. Скорость она
всё равно не считает — трекер свой.

`Velocity.toValidVelocity()` в foundation `internal` (`Draggable.kt:1113-1114`) — три строки
в `mapper/VelocityMappers.kt` (NaN→0; трекер их отдаёт, ради чего платформа хелпер и завела).

#### 2.5 Телеметрия

Два счётчика: `fling` (`FRAME_LIMIT`) — кадры затухания отдельно от `pan`; `rejected` (`IDLE_LIMIT`)
— кадры, где **обе** оси не потребили ничего. `rejected` считает полный отказ: по построению такой
кадр может быть один на бросок, следующим действием стоит `cancelAnimation()`. Частичный отказ
(одна ось у стенки, вторая жива) — норма и не считается. `lastPan` перестаёт затираться:
`onFlingFrame` пишет в отдельное `lastFling`. `isFlinging` — обычное поле (панель снимает
по таймеру, `GraphDebugOverlay.kt:69-80`). `GraphCanvasTelemetryTest` сломается — по назначению.

#### 2.6 Токен в `AppMotion`

```kotlin
/** Затухание броска: платформенная кривая скролла — та же, что у списка в чате. */
@Composable
fun <T> flingDecay(): DecayAnimationSpec<T> = rememberSplineBasedDecay()
```

#### 2.7 Тесты

Проходят: `consumed delta equals the request while the camera has room`;
`consumed delta is truncated at the boundary`; `consumed delta is zero on a saturated axis`;
`a degenerate range consumes nothing`; `banked overshoot cannot outlive a layout pass`.
**Провал: `a diagonal fling keeps its direction` (см. §5).**

Отдельная проблема тестируемости, специфичная для подхода: направление — свойство платформенной
спеки, а не моего кода. Чтобы его проверить, тест обязан создать `SplineBasedFloatDecayAnimationSpec`,
а тот при инициализации читает `ViewConfiguration.getScrollFriction()`
(`SplineBasedFloatDecayAnimationSpec.android.kt:38`). `returnDefaultValues` в проекте выключен
(`android-library-convention.gradle:43-53`), Robolectric в бандле `unittest` нет — значит нужен
`mockkStatic(ViewConfiguration::class)`. Приёма, которого в репозитории нет ни разу.

### 3. Честная цена, в числах

Все проценты **не зависят ни от density, ни от скорости** (проверено на density 1.0/2.0/2.75/3.5
и v 500…8000, совпадение до 4-го знака). Абсолютные величины — в dp для броска **2000 dp/s**
(647.4 dp по спеке).

#### 3.1 Увод направления прилёта

`θ_end = atan((tan θ)^1.736267606656372)`.

| Бросок | Прилёт | Увод | `dy/dx` vs `vy/vx` |
|---|---|---|---|
| 1° | 0.051° | +0.949° | −94.9 % |
| 5° | 0.834° | +4.166° | −83.4 % |
| 10° | 2.813° | +7.187° | −72.1 % |
| 15° | 5.802° | +9.198° | −62.2 % |
| 20° | 9.812° | +10.188° | −52.5 % |
| **22.146°** | 11.858° | **+10.288° — максимум** | −48.4 % |
| 25° | 14.891° | +10.109° | −43.0 % |
| 30° | 21.071° | +8.929° | −33.3 % |
| 40° | 36.406° | +3.594° | −12.1 % |
| 45° | 45.000° | 0° | 0 % |
| 60° | 68.929° | −8.929° | +49.8 % |
| 70° | 80.188° | −10.188° | +110.4 % |
| 80° | 87.187° | −7.187° | +258.9 % |

**Тождество, которого в разведке нет:** относительная ошибка `dy/dx` на прилёте = `(tan θ)^m − 1` =
ровно та же величина, что доля fling'а по одной оси. Один параметр описывает и увод, и «крючок».

#### 3.2 Выгиб траектории и хвост

| Бросок | Хорда, dp | Макс. выгиб | Промах прилёта от линии броска | Доля времени по одной оси | Доля **пути** в хвосте |
|---|---|---|---|---|---|
| 5° | 643.2 | 6.1 dp (0.94 %) | 46.8 dp (7.27 %) | 83.4 % | 57.3 % |
| 10° | 631.2 | 15.2 dp (2.41 %) | 79.0 dp (12.51 %) | 72.2 % | 37.2 % |
| 15° | 612.7 | 23.7 dp (3.87 %) | 97.9 dp (15.98 %) | 62.2 % | 24.5 % |
| 20° | 589.7 | 29.7 dp (5.04 %) | 104.3 dp (17.69 %) | 52.5 % | 15.7 % |
| **26.9°** | 555.3 | **31.9 dp (5.74 %) — максимум** | 96.4 dp (17.36 %) | 38.4 % | 7.4 % |
| 30° | 540.5 | 30.0 dp (5.54 %) | 83.9 dp (15.52 %) | 33.2 % | 5.0 % |
| 40° | 506.4 | 13.0 dp (2.57 %) | 31.7 dp (6.27 %) | 12.2 % | 0.5 % |
| 45° | 501.6 | 0 | 0 | 0 % | 0 % |

Два уточнения к разведке: **максимум выгиба — 5.74 % при ≈26.9°**, а не 5.6 % при 30°; и **хвост
длинный по времени, короткий по пути** — при 30° это 33 % длительности и 5 % расстояния, поэтому он
читается как излом в конце, а не как второе движение. Промах прилёта от линии броска (третья
колонка) в разведке отсутствует и вдвое-втрое больше выгиба: **до 17.7 % длины броска**.

#### 3.3 Недолёт — цена, о которой разведка не знает

Каждая ось получает `v·cos θ` и `v·sin θ`, а расстояние растёт как `v^1.736`, поэтому сумма катетов
короче одномерного пролёта:

| Бросок | Пролёт, dp | Недолёт | Длительность, мс | Короче на |
|---|---|---|---|---|
| 0° / 90° | 647.4 | 0 % | 924 | 0 % |
| 5° | 643.2 | 0.6 % | 922 | 0.2 % |
| 10° | 631.2 | 2.5 % | 914 | 1.1 % |
| 20° | 589.7 | 8.9 % | 883 | 4.4 % |
| 30° | 540.5 | 16.5 % | 831 | 10.1 % |
| 40° | 506.4 | 21.8 % | 760 | 17.7 % |
| **45°** | **501.6** | **22.54 %** | **716** | **22.54 %** |

Закрытая форма: пролёт = `√(cos^{2n}θ + sin^{2n}θ)`, при 45° это ровно `2^{(1−n)/2} = 2^{−m/2} =
0.774598`; длительность — та же константа, потому что `n = m + 1`.

**Следствие: «инвариантны 0°, 45° и 90°» — верно только про направление.** При 45° бросок летит
строго по диагонали, но на **22.5 % короче и на 22.5 % быстрее**, чем тот же бросок по горизонтали.
По-настоящему бесплатны только 0° и 90°.

### 4. Где цена не видна

Пороги заметности, оба обоснованные: **8 dp** — touch slop, то расстояние, которое система сама
считает шумом пальца; **52 dp** — половина `LANE_STEP = 104.dp` (`GraphGeometry.kt:249`): пока
боковой промах меньше половины шага дорожки, приезжаешь на ту же дорожку.

Ширина «невидимых» окрестностей по промаху прилёта:

| порог | 300 dp/s | 1000 dp/s | 2000 dp/s | 5000 dp/s |
|---|---|---|---|---|
| 8 dp | вся 360° | 0–2.6°, 40.9–49.2°, 87.4–90° (**15 %**) | 0–0.7°, 43.8–46.2°, 89.3–90° (**4 %**) | 0–0.1°, 44.8–45.2°, 89.9–90° (**1 %**) |
| 52 dp | вся 360° | вся 360° | 0–5.7°, 36.6–53.4°, 84.3–90° (**31 %**) | 0–0.9°, 43.4–46.6°, 89–90° (**6 %**) |

Чисто угловой порог (от скорости не зависит): 0.5° → 3 % квадранта; 1° → 5 %; 2° → 11 %; 5° → 30 %.

**Вывод честный и невесёлый.** Подход неотличим от правильного там, где бросок медленный: при
300 dp/s он безупречен на всех 360°, потому что весь бросок укладывается в 24 dp и промах физически
меньше пальца. Проблема ровно в том, что затухание завели ради быстрых бросков: чем сильнее флик,
тем уже окно. При 2000 dp/s невидимы 4 % направлений по строгому порогу и 31 % по мягкому.
Окно сужается как `v^{−n}`. Плюс недолёт 22.5 % при 45° не виден **никогда** — сравнивать не с чем.

### 5. Тест `a diagonal fling keeps its direction` — провалю

**Провалю на любом угле, кроме 0°, 45° и 90°.** `dy/dx` не просто отличается от `vy/vx` в конце —
оно **не постоянно ни на одном интервале**. Бросок 30°, 2000 dp/s, ожидается `tan 30° = 0.577350269`:

| момент | `dy/dx` | расхождение |
|---|---|---|
| 5 % длительности | 0.571558 | −1.00 % |
| 10 % | 0.553363 | −4.15 % |
| 25 % | 0.495674 | −14.15 % |
| 50 % | 0.436208 | −24.45 % |
| 75 % | 0.396843 | −31.26 % |
| 100 % | 0.385296 | −33.26 % |

Худшее по углам: 10° → **−72.1 %**, 20° → −52.5 %, 30° → −33.3 %, 60° → +49.8 %, 70° → +110.4 %.
Тест с любым разумным допуском (даже 5 %) падает начиная примерно с 1.5° от оси.

Первый кадр почти правильный (при 30° ошибка −0.3 %) — важно для диагноза: **бросок стартует верно
и уводится по дороге**, поэтому баг-репорт будет звучать как «поехало правильно, а приехало не туда»,
и искать будут в клампе камеры.

### 6. Можно ли спасти дешёвой правкой

#### 6.1 Чего спасти нельзя — доказательство

Прямая траектория с верным направлением требует `x(t) = X·f(t)`, `y(t) = Y·f(t)` с одной и той же
`f` и `Y/X = tan θ`. Покомпонентная спека даёт `Y/X = (tan θ)^n` и `Ty/Tx = (tan θ)^m`. Оба условия
выполняются для всех θ ⟺ `n = 1` и `m = 0` ⟺ `D → ∞`. `D = ln(0.78)/ln(0.9)` — `private val` на
уровне файла (`FlingCalculator.kt:32`), константа спеки, не параметр. Единственный параметр
экземпляра — `density` (`SplineBasedFloatDecayAnimationSpec.kt:28`) — общий для обеих осей
и в отношении сокращается. `friction` вообще не параметр: `FlingCalculator` — `internal class`
(`:47`), `platformFlingScrollFriction` — `internal actual val` (`.android.kt:38`).

**Пока обе оси идут по своим часам, кривая гнётся. Общие часы = одна скалярная величина прогресса =
подход B.** Спасения, оставляющего покомпонентность, не существует.

#### 6.2 Что спасти можно — и почему это не помогает

Предварительный перекос начальной скорости:

```kotlin
private const val DISTANCE_EXPONENT = 1.736267606656372f

internal fun Velocity.toDecayVelocity(): Offset {
  val magnitude = sqrt(x * x + y * y)
  if (magnitude == 0f) return Offset.Zero
  val warp = 1f / DISTANCE_EXPONENT
  return Offset(
    x = magnitude * (abs(x) / magnitude).pow(warp) * sign(x),
    y = magnitude * (abs(y) / magnitude).pow(warp) * sign(y)
  )
}
```

Константу можно откалибровать по публичному API: `DecayAnimationSpec<Float>.calculateTargetValue(0f, v)`
(`DecayAnimationSpec.kt:78-89`) — закон степенной точно, две пробы дают
`n = ln(d₂/d₁)/ln(v₂/v₁) = 1.736267606656` до последнего знака.

| | без перекоса | с перекосом |
|---|---|---|
| направление прилёта | увод до **10.288°** | **точно 0.0000°** на всех углах |
| пролёт | недолёт до **22.5 %** | **647.4 dp — ровно по спеке** |
| длительность | короче до 22.5 % | короче до 13.6 % |
| макс. выгиб | 5.74 % при 26.9° | 5.74 % при 17.6° |
| хвост по одной оси | 72.2 % при 10° | 52.1 % при 10° |

**И это всё равно не спасение.** Перекос не убирает ошибку — он её переносит. Анимация теперь
*стартует* по направлению `atan((tan θ)^{1/n})`:

| бросок | старт анимации | ошибка на старте |
|---|---|---|
| 5° | 13.811° | +8.811° |
| 10° | 20.207° | +10.207° |
| **11.858°** | 22.146° | **+10.288° — максимум** |
| 20° | 29.193° | +9.193° |
| 30° | 36.084° | +6.084° |

Максимум ровно тот же **10.2883°** (отображение обратное), но перенесён в худшее место: **в момент
отрыва пальца, где глаз ещё ведёт палец**. Вместо «доехало не туда» получаем «рвануло не туда» —
заметнее, а не тише. Пиковый выгиб не уменьшился ни на десятую процента.

### 7. Настоящие преимущества — без преувеличения

**«Самый короткий код» — неправда, и это надо сказать первым.** Перестройка камеры, телеметрия
и тесты общие для всех трёх подходов, ≈180 строк. Специфичного для инерции:

| | подход C | `scrollable2D` |
|---|---|---|
| жест + скорость | ≈18 строк сверх нынешнего | −7 строк (блок `pointerInput` уходит целиком) |
| `toValidVelocity` руками | 3 | 0 |
| запуск затухания | ≈22 (`fling` + `flingJob`) | ≈8 (`Scrollable2DState` в держателе) |
| каталог версий | 0 | +2 |
| **итого** | **≈49 строк** | **≈17 строк** |

Что действительно есть:
1. **Граф зависимостей не трогается.** Всё в `animation`/`animation-core`/`ui` **1.11.4**.
   Единственный foundation-API — `detectDragGestures`, который тут уже стоит. Это единственное
   место, где подход C объективно лучше рекомендации.
2. **Отмена — одна строка и никакой семантики.** `flingJob?.cancel()` на `onDragStart`.
3. **Правило границы формулируется прямо.** У `scrollable2D` потреблённое сворачивается в модуль,
   и то же поведение приходится добывать, сообщая о непотреблённой дельте как о потреблённой —
   то есть враньём в контракте.
4. **Дом-стиль совпадает буквально** (`PredictiveBackController.kt:116-127, 140-145`).
5. **Скольжение вдоль стенки получается само.**

Чего нет: overscroll из коробки, nested scroll, `MutatorMutex`, платформенное обращение со
скоростью, и — главное — правильная геометрия.

### 8. Уточнения к разведке

1. **`detectDragGestures` с `orientationLock` отдаёт `onDragEnd(change: PointerInputChange)`**
   (`DragGestureDetector.kt:240`). Формулировку «обе перегрузки без параметров» надо поправить.
2. **Длительности в таблице спеки на 1 мс меньше**: `.toLong()` усекает (`FlingCalculator.kt:62`).
   228/555/924/1246/1815/2566.
3. **Максимум выгиба — 5.74 % при ≈26.9°**, а не 5.6 % при 30°.
4. **Хвост длинный по времени, короткий по пути**: при 30° — 33.2 % длительности и 5.0 % расстояния.
5. **«Инвариантны 0°, 45° и 90°» — только по направлению.** При 45° пролёт короче ровно на
   `1 − 2^{−m/2} = 22.54 %`, и длительность — на столько же.
6. **`Animatable.updateBounds` на этом экране непригоден**: вырожденный `panRangeOf` + правило «стоп
   при достижении границы по *любой* оси» убивают наклонный бросок на первом кадре. Это стоит знать
   всем трём подходам.
7. **`durationScale == 0f`** (анимации выключены в дев-опциях) отдаёт `playTimeNanos =
   anim.durationNanos` на первом же кадре (`SuspendAnimation.kt:333-338`) — весь fling приезжает
   одной дельтой. Кламп это переживает, но проверить на устройстве стоит.
8. **`ViewConfiguration.getScrollFriction()` делает spline-спеку недоступной юнит-тесту** без
   `mockkStatic`. Общее ограничение, но для C фатально: у него вся геометрия направления живёт
   внутри спеки.

### Итог как foil

Подход работает, аккуратно чинит камеру и стоит ≈49 строк специфичного кода. Цена измерена: до
**10.29°** увода направления, до **17.7 %** длины броска мимо цели, до **5.74 %** выгиба, до
**22.5 %** недолёта — включая диагональ 45°, которую все считали безопасной. Тест
`a diagonal fling keeps its direction` проваливается на всех углах, кроме 0/45/90, с расхождением
до 72 %. Невидим только на медленных бросках и в узких окнах вокруг осей и диагонали, сужающихся
как `v^{−1.736}`. Спасти дешёвой правкой можно лишь наполовину. Единственное честное преимущество
перед `scrollable2D` — независимость от версии foundation.

---

# Часть II. Вердикты судей

## Вердикт: линза «физика»

Всё численное перепроверено собственным портом `AndroidFlingSpline` + `FlingCalculator`
(`scratchpad/phys.py`, `trace.py`, `dir.py`), покадровая арифметика — во float32.
Каждое утверждение о библиотеке — с файлом и строкой из распакованных исходников.

---

### 0. База: что я перечитал сам

| факт | место |
|---|---|
| `Velocity.angle get() = atan2(x = x, y = y)` — именованные аргументы `kotlin.math.atan2(y, x)`, то есть **угол стандартный от +X**, подмены осей нет | `f1100/…/gestures/Scrollable2D.kt:521-522` |
| разложение `abs(cos(angle)*s)*sign(vx)`, `abs(sin(angle)*s)*sign(vy)`; ветка `angle.isNaN()` → `Offset(0f, this)` | `Scrollable2D.kt:407-415` |
| `return consumedOffset.toMagnitudeFloat()` — потреблённое сворачивается в модуль | `Scrollable2D.kt:451` |
| гашение — единственный механизм `if (abs(delta - consumed) > 0.5f) cancelAnimation()`; порог `abs(initialVelocity) > 1f`; цикл в `withContext(motionDurationScale)` | `Scrollable.kt:1038-1056` |
| `DefaultScrollMotionDurationScale.scaleFactor == 1f` | `Scrollable.kt:1071-1077` |
| `DefaultScrollable2DState.canScroll(offset) = true` — **всегда** | `Scrollable2DState.kt:163` |
| `shouldDispatchOverscroll(offset) = scrollableState.canScroll(offset)` → overscroll в контуре **всегда** | `Scrollable2D.kt:357`, `:322-333` |
| скорость клампится `maximumFlingVelocity` и чистится `toValidVelocity()` **до** `onDragStopped` → ветка `angle.isNaN()` в `scrollable2D` недостижима | `Draggable.kt:1057-1061`, `:591-598`, `:1113-1114` |
| `MutatorMutex.mutateWith`: `tryMutateOrCancel` → `job.cancel()`, затем `mutex.withLock` — новый блок **не стартует**, пока старый не отпустил `finally` | `MutatorMutex.kt:83-88, 90-100, 158-165` |
| `flingPosition(time)` делает `coerceIn(0f, 1f)` → ось с вышедшим временем **насыщается**, а не улетает | `anim1114/…/SplineBasedDecay.kt:145-148` |
| `duration` собирается через `.toLong()` — **усечение** | `anim1114/…/FlingCalculator.kt:57-62` |
| `VectorizedFloatDecaySpec`: спека к каждой компоненте, длительность `maxOf` | `animcore1114/…/DecayAnimationSpec.kt:130-160` |
| `doAnimationFrameWithScale`: при `durationScale == 0f` первый кадр получает `playTimeNanos = anim.durationNanos` | `animcore1114/…/SuspendAnimation.kt:326-338` |
| `MotionDurationScaleImpl` читает системный animator duration scale и стоит в контексте рекомпозера окна | `ui1114/…/WindowRecomposer.android.kt:333-336, 423-446` |

**Таблица спеки воспроизведена точно** (density 1.0/2.0/2.75/3.5 дают одно и то же в dp/мс):

| v | пролёт | длительность |
|---|---|---|
| 300 dp/s | 24.02 dp | **228** мс |
| 1000 | 194.31 | **555** |
| 2000 | 647.40 | **924** |
| 3000 | 1308.92 | **1246** |
| 5000 | 3177.62 | **1815** |
| 8000 | 7186.36 | **2566** |

Разведка (и вслед за ней KDoc токена подхода B, «2000 dp/s → 647 dp за 925 мс») даёт длительности
на 1 мс больше. **Прав подход C** (§8.2): `flingDuration` усекает. Мелочь, но она попадёт в KDoc
дизайн-системы и в тест.

Ещё одна общая вещь, которой нет ни в одном подходе: `getValueFromNanos` режет время до **целых
миллисекунд** (`SplineBasedFloatDecayAnimationSpec.kt:47`). На пике 2000 dp/s это до 2 dp
покадрового дрожания у всех трёх одинаково. Гнаться за субпиксельной гладкостью бессмысленно.

---

### Подход A — `scrollable2D` + платформенный `FlingBehavior`

#### Что выдержало давление

**1. Заявленное тождество «разложение сохраняет модуль» — верно.** Прогнал во float32 по 3600
углам × 5 величин дельты (0.001 … 467 px):

```
max |sqrt(px² + py²) − s| = 3.05e-05 px      порог, который важен: 0.5
```

Запас четыре порядка. При камере до 10⁶ px и дельте до 500 px тождество не ломается.

**2. Заявление «возврат полной дельты сохраняет fling живым» — верно, и оно выживает даже
через overscroll-обёртку, чего подход A не проверял.** `applyToScroll` возвращает
`consumedOffset + consumedByDelta` (`AndroidOverscroll.android.kt:678`), а наша лямбда получает не
`delta`, а `leftForDelta = delta − consumedOffset` (`:617-618`). Вернув свой полный вход, лямбда
даёт `consumedOffset + leftForDelta = delta` — телескопирует ровно в исходное. Модули совпадают,
`cancelAnimation` не срабатывает. Аргумент A держится.

**3. Направление держится на всех 360° — численно.** Покадровая симуляция всего полёта
(2000 dp/s, 57 кадров, float32), накопленное `dy/dx` против `tan θ`:

| бросок | `dy/dx` первый кадр | последний кадр | `tan θ` | макс. отн. ошибка |
|---|---|---|---|---|
| 5° | 0.087488662 | 0.087488661 | 0.087488664 | 1.8e-05 % |
| 10° | 0.176326985 | 0.176326966 | 0.176326981 | 1.9e-05 % |
| 22.146° | 0.406993412 | 0.406993536 | 0.406993426 | 2.9e-05 % |
| 30° | 0.577350267 | 0.577350450 | 0.577350269 | 3.1e-05 % |
| 60° | 1.732050815 | 1.732050267 | 1.732050808 | 3.1e-05 % |

Пролёт — `647.399` dp при спеке `647.399`. Тест `a diagonal fling keeps its direction` проходит с
любым разумным допуском. Пролёт совпадает с таблицей **точно**, потому что это буквально тот же
`rememberSplineBasedDecay`.

**4. Гонки флага `drive` нет.** `MutatorMutex` не просто отменяет старого мутатора, а держит
`Mutex` (`MutatorMutex.kt:158-165`): блок нового жеста не начнётся, пока `finally` декоратора
`CameraFlingBehavior` не вернул `drive = Gesture`. Слабое место №3 из §8 подхода A закрыто.

**5. Граница не молотит.** Трассировка (rangeX = −400…440, viewport 1000, 2000 dp/s):

| сценарий | кадров до остановки | камера |
|---|---|---|
| в 50 px от левой стенки, Y свободна, 150° | 37 (полный полёт, скользит вдоль стенки, добивает Y) | (−400, 0) |
| то же, Y **вырождена** | **4** | (−400, −123) |
| **угол**: уже у левой стенки и у верхней, 135° | **2** | (−400, 0) |

Один лишний кадр против B — это кадр, на котором `applied` впервые становится нулевым; на нём
платформа ещё не знает об отказе. Молотьбы нет ни в одном сценарии.

#### БЛОКИРУЮЩИЙ ДЕФЕКТ: overscroll съедает вертикальную скорость целиком, и 360° ломается

Подход A сам подключает `rememberOverscrollEffect()` параметром `scrollable2D` (§3) — и вместе с
ним получает `applyToFling`, который **до** запуска инерции снимает скорость на релаксацию
растяжения:

```kotlin
// AndroidOverscroll.android.kt:703-719
val consumedY =
    if (edgeEffectWrapper.isTopStretched() && velocity.y < 0f)
        getOrCreateTopEffect().absorbToRelaxIfNeeded(velocity.y, containerSize.height, density)
    …
val remainingVelocity = velocity - consumed
val consumedByVelocity = performFling(remainingVelocity)   // ← угол считается ОТ ЭТОГО
```

а `absorbToRelaxIfNeeded` — **всё-или-ничего** (`EdgeEffectCompat.android.kt:74-88`):

```kotlin
return if (flingDistance <= actualDistance) {
    onAbsorbCompat(velocity.roundToInt())
    velocity            // ← съедает ВСЮ скорость этой оси
} else 0f
```

**Воспроизведение.** Экран с одной дорожкой — по условию задачи норма.

1. `panRangeOf` при помещающемся содержимом отдаёт вырожденный диапазон
   (`GraphGeometry.kt:169-181`), поэтому по Y камера не потребляет ничего.
2. Значит на **каждом** кадре драга `leftForOverscroll.y == delta.y`, и `applyToScroll` при
   `source == UserInput` тянет верхний/нижний край (`AndroidOverscroll.android.kt:644-668`).
   Условие `> 0.5f` выполняется всегда. То есть **вертикальное растяжение на этом экране активно
   после любого свайпа**, даже слегка наклонного.
3. Отпускаем под углом. `distanceCompat` (API 31+) отдаёт глубину растяжки; `flingDistance` —
   тот же сплайн, что и у инерции (`EdgeEffectCompat.android.kt:197-207`). Если
   `flingDistance(vy) ≤ глубина`, **весь `vy` уходит в растяжку**, и `available` в
   `doFlingAnimation` становится `(vx, 0)` — угол броска **скачком** становится ровно 0°/180°.

Порог, посчитанный по той же формуле:

| глубина растяжки | `vy` ниже этого съедается целиком |
|---|---|
| 10 dp | 181 dp/s |
| 50 dp | 458 dp/s |
| 100 dp | 682 dp/s |
| 200 dp | 1017 dp/s |
| 300 dp | 1284 dp/s |

Один вертикальный свайп на 200 dp по экрану с одной дорожкой оставляет растяжку такой глубины, что
следующий бросок теряет вертикаль вплоть до 1000 dp/s.

**Почему это дефект физики, а не косметика.** Результат одного и того же жеста становится
**разрывным** по невидимому пользователю состоянию. Бросок 30° при 2000 dp/s на экране с одной
дорожкой:

| состояние края | что делает fling | горизонтальный пролёт | длительность |
|---|---|---|---|
| растяжка глубокая → `vy` съеден | одномерный fling с \|v\|=1732 | **504.3 dp** | **831 мс** |
| растяжки нет → `vy` доехал | правило §5 подхода A, скольжение вдоль стенки | **560.7 dp** | **924 мс** |

Разрыв **11.2 % по пути и 11.2 % по времени** от того, тянул ли пользователь до этого палец вниз.
Угол при этом не «уводит на градус», а **защёлкивается на ось** — то самое, что требование «360»
запрещает.

И отдельно: правило §5 (поглощать дельту на упёршейся оси, врать nested scroll) заведено ровно ради
экрана с одной дорожкой — а на этом экране оно в большинстве бросков **не срабатывает вовсе**,
потому что вертикали до него не доезжает. Цена уплачена, товар не получен.

**Дефект в композиции выборов подхода A, а не в ядре.** Лечится: не отдавать `overscrollEffect` в
`scrollable2D` (оставить только `Modifier.overscroll` для отрисовки и звать `applyToScroll` руками),
либо не брать overscroll вовсе. Но **как подход A написан — он не выполняет требование «fling на
360»** на экране, который сам же называет нормой. Проверить это на устройстве обязательно
(API 31+; на API 30 и ниже `distanceCompat == 0f` и дефекта нет — `EdgeEffectCompat.android.kt:103-108`).

#### Серьёзные замечания

**A-1. Критерий гашения — тест на точность float, замаскированный под геометрию.**
`if (applied.x == 0f && applied.y == 0f)` — точное сравнение с нулём. При большой координате камеры
дельта, меньшая половины ulp, читается как «ось не двинулась»:

| камера | ulp | дельта, читающаяся как ноль |
|---|---|---|
| 8 192 px | 0.00098 | < 0.0005 px |
| 65 536 px | 0.0078 | < 0.004 px |
| 1 048 576 px | 0.125 | < 0.06 px |

Хроника длиной в 10⁵–10⁶ px реальна (`LANE_STEP = 104.dp` × число сообщений). Практический вред
мал — такие дельты бывают только в хвосте, который и так вот-вот кончится, — но правило смотрит не
туда: подход B на том же месте спрашивает про **диапазон**, и этой чувствительности не имеет.

**A-2. Диагональ на вырожденной оси доезжает медленно — цена решения §5 не измерена.**
Экран с одной дорожкой, 2000 dp/s, полная длительность 924 мс всегда:

| бросок от горизонтали | горизонтальный пролёт под A | что дал бы платформенный `\|\|` |
|---|---|---|
| 5° | 645 dp | 0 |
| 30° | 561 dp | 0 |
| 60° | **324 dp** | 0 |
| 80° | **112 dp** | 0 |

При 80° полотно почти секунду ползёт на 112 dp — средняя скорость 122 dp/s. Это читается не как
инерция, а как заедание. Решение §5 верно для 0–30° и сомнительно за 60°; ни A, ни B этого не
посчитали.

**A-3. Разрыв договора с overscroll в другую сторону.** Вернув полную дельту на упёршейся оси,
подход A делает `leftForOverscroll == 0` — значит **во время инерции растяжка не загорается
никогда**, кроме случая полного отказа. Диагональный fling, доехавший до стенки, не подсвечивает её.
A знает об этом (§8.7), но не связал с тем, что overscroll — его единственный аргумент против B.

**A-4. `canScroll` всегда `true` подтверждён** (`Scrollable2DState.kt:163`) — и это не косметика:
именно через него overscroll попадает в контур на каждом кадре (`Scrollable2D.kt:357`). Пункт 8
слабых мест подхода A на деле сильнее, чем он сам написал.

#### Оценка по линзе физики: **7 / 10**

Ядро — эталон: направление точное до 3e-5 %, пролёт и длительность буквально платформенные, граница
и угол закрываются за 2–4 кадра, гонок нет. Блокирует не арифметика, а собственный выбор подключить
overscroll параметром: он ломает 360° защёлкиванием на ось и даёт 11 % разрыв по пролёту на
основном экране задачи.

---

### Подход B — своя скалярная физика

#### Что выдержало давление

**1. Заявленное тождество `cos(atan2(vy, vx)) == vx/|v|` — верно, включая нулевую компоненту.**
Именованные аргументы `atan2(x = x, y = y)` в `Scrollable2D.kt:522` действительно дают стандартный
угол от +X: у `kotlin.math.atan2(y, x)` параметр `y` получает `velocity.y`. Подмены осей нет.
Прогон во float32 по 3600 углам × 5 величин дельты, платформенная формула против `vx/|v| · s`:

```
max |platform − B| = 3.05e-05 px          порог гашения: 0.5 px
```

Тождество держится. Случаи `vx == 0` и `vy == 0` совпадают точно: у платформы
`abs(cos(±π/2)·s) · sign(0f) = ±0.0`, у B — `0f/|v| · s = 0`. Формула B **не хуже** платформенной,
а на отрицательных дельтах строго лучше: платформа восстанавливает знак через `abs`/`sign` и потому
корректна только пока `s ≥ 0` (у decay из нуля это так), B корректна всегда.

Тест `the decomposition matches the platform angle formula` придётся писать **с допуском** (1e-4 px
хватит с запасом) — на точном равенстве он упадёт.

**2. Направление держится на всех 360°** — разложение то же, что у A, значит и таблица `dy/dx` та
же (см. A, п.3): отклонение ≤ 3.1e-05 %, пролёт `647.399` при спеке `647.399`.

**3. Критерий границы строго лучше платформенного и лучше, чем у A.** Он смотрит на диапазон, а не
на дельту, поэтому невосприимчив ни к нулевой дельте нулевого кадра, ни к точности float при
большой камере (дефект A-1). Трассировка:

| сценарий | кадров | камера |
|---|---|---|
| середина, обе оси свободны, 30° | 36 (до упора X) | (440, 0) |
| 50 px от стенки, Y свободна, 150° | 36, скользит вдоль стенки | (−400, 0) |
| 50 px от стенки, **Y вырождена** | **3** | (−400, −123) |
| **угол** (обе оси уже упёрлись), 135° | **1** | (−400, 0) |
| одна дорожка, 5° | 19 | (440, −123) |
| одна дорожка, 60° | 57 (полный полёт) | (323.7, −123) |

Угол закрывается на **первом же кадре** — на кадре с нулевой дельтой, где A ещё ничего не знает.
Молотьбы нет нигде. Таблица §4 подхода B воспроизведена целиком, включая строку «нулевой кадр в
начале → едет».

**4. Вырожденный диапазон разобран честно, а не пережит.** `range.start >= range.endInclusive` и
`direction == 0f` сведены в одно условие «нести бросок этой оси нечем» — это ровно верная физика,
и она единственная из трёх не зависит от того, что показала последняя дельта.

**5. `MotionDurationScale` учтён — и это не педантизм.** `MotionDurationScaleImpl`
(`WindowRecomposer.android.kt:423-446`) читает системный animator duration scale и лежит в
контексте рекомпозера, откуда его наследует `rememberCoroutineScope()`. Без `withContext(scaleFactor
= 1f)` при выключенных анимациях в дев-опциях `doAnimationFrameWithScale` отдал бы
`playTimeNanos = durationNanos` **на первом кадре** (`SuspendAnimation.kt:333-338`) — весь fling
одной дельтой, телепорт. B единственный из двух «ручных» подходов это закрыл.

**6. Смена набора узлов посреди затухания доезжает.** Цикл читает `cameraRange` каждый кадр, а
`layout()` его переписывает. Телепорта нет и залипания нет: анимируется накопитель `travelled` от
нуля, а не сама камера, поэтому пере-зажатие камеры в `layout()` не рассинхронизирует анимацию.
Расширение диапазона (новое сообщение) идущий fling просто получает как «стенка отодвинулась».

**7. Поправка B к разведке про `detectDragGestures` верна** — перегрузка с `orientationLock`
объявляет `onDragEnd: (change: PointerInputChange) -> Unit` (`f1100/…/DragGestureDetector.kt:240`).
API-ссылки §«Проверено по исходникам» я выборочно перепроверил: `AnimationState(initialValue: Float,
initialVelocity: Float)` — `AnimationState.kt:272-278` ✓; `AnimationState.animateDecay` —
`SuspendAnimation.kt:181-185` ✓; `cancelAnimation()` — `AnimationState.kt:172-175` ✓;
`calculateVelocity(maximumVelocity)` — `VelocityTracker.kt:88-89` ✓.

#### Блокирующих дефектов по физике не нашёл

#### Серьёзные замечания

**B-1. Трекер не получает кадр пересечения слопа — скорость занижена на коротких фликах.**
`onDragStart = { down, _, _ -> … addPointerInputChange(down) }` выбрасывает второй параметр,
`slopTriggerChange`. Платформенный `DragGestureNode` кормит трекер **каждым** изменением
(`Draggable.kt:1050`, `:1055`). `addPointerInputChange` накапливает `positionChange()`
(KDoc `VelocityTracker.kt:96-104`), поэтому пропущенный кадр — это пропущенный участок пути при
неизменённой временной шкале. На жестах длиннее горизонта трекера это неважно, на быстром флике —
занижение. Правка в один аргумент, но её надо сделать. **То же самое у подхода C** (§2.4).

**B-2. Политика NaN расходится с платформой, и это надо записать как решение.** B гасит весь fling
при `magnitude.isNaN()`. Платформа сначала чистит `toValidVelocity()` (`Draggable.kt:1113-1114`,
`NaN → 0f`), то есть при `vx = NaN, vy = 500` бросила бы вертикально на 500. Ветка
`available.angle.isNaN()` в `Scrollable2D.kt:409` при этом **мёртвая** — до неё NaN не доживает.
Выбор B (не бросать вовсе) защитим, но он не «то же самое, что у платформы», как читается из §1.

**B-3. KDoc токена содержит цифру из разведки, а не из кода.** «2000 dp/s → 647 dp за **925** мс» —
на самом деле **924** (`flingDuration` усекает, `FlingCalculator.kt:62`). Токен `AppMotion.flingDecay()`
— место, куда эта таблица попадёт надолго.

**B-4. Цена решения §6 не измерена — то же замечание, что A-2.** Одна дорожка, 2000 dp/s: бросок
60° даёт 324 dp за 924 мс, 80° — 112 dp за 924 мс. Строка «едет вдоль стенки» в таблице §4 верна,
но она не говорит, что «едет» может значить «ползёт секунду». Развилка `&&` / `||` заслуживает
угловой оговорки, а не одного бита.

**B-5. Растяжки нет вовсе — значит на экране с одной дорожкой вертикальный свайп не даёт ни
движения, ни отклика.** Это не дефект физики (арифметика верна), но это ровно тот сигнал, ради
которого A подключил overscroll — и получил из-за него блокирующий дефект. B меняет одну проблему
на другую, и обе надо назвать вслух.

**B-6. Запись `camera.value` в `layout()` — это запись снапшот-состояния из фазы измерения.**
`offset` у B — тот же самый `State`, а `debugInfo` его читает. KDoc `GraphCanvasState.kt:44-49`
прямо говорит, что промах фазы «уже приводил к бесконечному циклу измерения». Спасает
структурное сравнение `mutableStateOf`: пере-зажатие вхолостую — no-op. Но замечание одинаково
касается всех трёх подходов (A ставит явный сторож `if (clamped != camera)`, C не оговаривает
ничего), и стоит явного сторожа, а не рассуждения о политике сравнения.

#### Оценка по линзе физики: **9 / 10**

Единственный подход, у которого и направление, и пролёт, и критерий границы, и системный
duration scale одновременно верны, а критерий гашения не зависит ни от точности float, ни от
дельты нулевого кадра. Снял балл за занижение скорости на слопе (B-1), расхождение по NaN (B-2) и
неизмеренную цену развилки диагонали (B-4).

---

### Подход C (foil) — покомпонентное затухание

#### Что выдержало давление: его собственные числа

Воспроизвёл все таблицы C своим портом. Совпадение практически полное:

**Увод направления** `θ_end = atan((tan θ)^n)`, `n = 1.7362676`:

| бросок | мой прилёт | C | увод | ошибка `dy/dx` |
|---|---|---|---|---|
| 1° | 0.051° | 0.051° | +0.949° | −94.9 % |
| 5° | 0.834° | 0.834° | +4.166° | −83.4 % |
| 10° | 2.813° | 2.813° | +7.187° | −72.1 % |
| **22.146°** | 11.858° | 11.858° | **+10.2883°** | −48.4 % |
| 30° | 21.071° | 21.071° | +8.929° | −33.3 % |
| 45° | 45.000° | 45.000° | 0° | 0 % |
| 70° | 80.188° | 80.188° | −10.188° | +110.5 % |

Максимум увода найден численно золотым сечением: **10.2883° при θ = 22.1461°** — ровно как у C.

**Недолёт** (`√(cos^{2n}θ + sin^{2n}θ)`): 5° → 0.65 %, 10° → 2.51 %, 20° → 8.90 %, 30° → 16.52 %,
40° → 21.78 %, **45° → 22.52 %**. У C 22.54 % — расхождение в третьем знаке оттого, что C взял
`D` в double, а код делает `(ln(0.78)/ln(0.9)).toFloat()` (`FlingCalculator.kt:32`): истинные
`n = 1.7362676463664735`, `m = 0.7362676463664736`. Вывод C не меняется.

**Выгиб и промах прилёта** — совпали до второго знака, включая нетривиальный максимум:
26.9° → выгиб **5.74 %**, промах прилёта 20° → **104.3 dp (17.69 %)**. Таблица `dy/dx` по времени
при 30° тоже воспроизведена (100 % → −33.26 % ровно; в середине мои значения на 0.1–0.4 п.п.
хуже C — разница от квантования до миллисекунд).

**Насыщение короткой оси подтверждено кодом:** `flingPosition` клампит время в `[0,1]`
(`SplineBasedDecay.kt:145-148`), поэтому «крючок» — это именно остановка одной оси, а не улёт.

Численная часть подхода C — самая аккуратная из трёх. Его поправки к разведке (§8.1–8.5) верны.

#### Блокирующий дефект 1: геометрия — подтверждён, как и заявлено

Тест `a diagonal fling keeps its direction` проваливается на всех углах, кроме 0/45/90, с
расхождением до 72 % на 10° и 110 % на 70°. Доказательство невозможности спасения (§6.1) верно:
`D` — `private val` файлового уровня, `friction` — `internal`, единственный параметр экземпляра
`density` в отношении осей сокращается. Ничего не добавлю.

Одно уточнение к §3.3, которое C сформулировал слабее, чем следует: **«инвариантны 0°, 45° и 90°»
неверно даже про направление в динамике.** При 45° конечная точка на линии броска, но `dy/dx`
постоянен только потому, что обе оси идут по **одинаковым** часам; при любом другом угле
`dy/dx` не постоянен ни на одном интервале, то есть тест не спасёт даже допуск на конечную точку.

#### Блокирующий дефект 2: системный duration scale не нейтрализован

C сам называет это в §8.7 как «проверить на устройстве», но **в проекте §2.3 его не чинит**:
`scope.launch { AnimationState(...).animateDecay(decay) { … } }` — без `withContext`.

Воспроизведение: дев-опции → Animator duration scale = «Off». `durationScale == 0f` →
`doAnimationFrameWithScale` отдаёт `playTimeNanos = anim.durationNanos` **на первом кадре**
(`SuspendAnimation.kt:333-338`) → весь пролёт приезжает одной дельтой. При 2000 dp/s это
мгновенный прыжок камеры на 647 dp. Кламп это переживёт, «инерция как в чате» — нет.
Симметрично, при scale = 10× fling длится 8.3 с.

Это не про геометрию, и подход A от этого защищён платформой
(`DefaultScrollMotionDurationScale = 1f`, `Scrollable.kt:1071-1077`), а B — своим
`FlingDurationScale`. **C — единственный, кто отдаёт физику инерции в руки настройки разработчика.**

#### Серьёзные замечания

**C-1. `mockkStatic` нужен не «для полноты», а потому что вся геометрия направления живёт внутри
спеки.** C это сказал (§2.7) — соглашаюсь и усиливаю: у B и A геометрия живёт в собственном коде и
проверяется на `exponentialDecay`; у C проверить нечего, кроме камеры.

**C-2. Тот же пропуск `slopTriggerChange`, что у B (B-1).**

**C-3. Критерий гашения `consumed == Offset.Zero && requested != Offset.Zero`** имеет ту же
чувствительность к точности float, что A-1, плюс `Offset` — value class над упакованным `Long`,
и структурное равенство сравнивает биты. Практически безопасно (`x − x` даёт `+0.0`), но
рассуждение опирается на IEEE, а не на диапазон.

**C-4. `Animatable.updateBounds` непригоден — верно и важно всем троим.** KDoc «stop as soon as
**any** dimension … is reached» + вырожденный `panRangeOf` = смерть наклонного броска на первом
кадре. Проверено в `Animatable.kt`; это лучшая находка подхода C после его собственных таблиц.

**C-5. Единственное объективное преимущество C сформулировано верно** (граф зависимостей не
трогается: `animation`/`animation-core`/`ui` 1.11.4). Но это не физика.

#### Оценка по линзе физики: **3 / 10**

Foil работает как задумано: он честен, его числа перепроверяются до третьего знака, и он сам
объясняет, почему проигрывает. По физике он проигрывает дважды — заявленной геометрией и
незамеченным duration scale.

---

### Прямой ответ

| подход | направление 360° | граница | угол | вырожденный диапазон | смена узлов | пролёт vs таблица | оценка |
|---|---|---|---|---|---|---|---|
| **A** | точно (3e-5 %) — **но защёлкивается на ось из-за overscroll** | 4 кадра | 2 кадра | живёт | доезжает | **точно** | **7** |
| **B** | точно (3e-5 %) | 3 кадра | **1 кадр** | живёт | доезжает | точно | **9** |
| **C** | **увод до 10.29°, `dy/dx` до −72 %** | 4 кадра | 1 кадр | живёт | доезжает | **недолёт до 22.5 %** | **3** |

**Блокирующий дефект по физике есть у двух подходов из трёх.**

- **C — два, и оба блокирующие**: покомпонентная геометрия (заявлен, подтверждён численно) и
  неотключённый системный `MotionDurationScale` (не заявлен как дефект, только как «проверить»).
- **A — один, в том виде, в котором подход написан**: `applyToFling` → `absorbToRelaxIfNeeded`
  съедает вертикальную скорость **целиком** (всё-или-ничего, `EdgeEffectCompat.android.kt:74-88`),
  и на экране с одной дорожкой, где растяжка активна после любого свайпа, диагональный бросок
  защёлкивается на горизонталь. Разрыв 11.2 % по пролёту и по длительности от невидимого
  состояния края. Требование «fling на 360» не выполняется на экране, который сам подход называет
  нормой. Лечится отвязкой overscroll от `scrollable2D` — ядро подхода при этом остаётся эталонным.
- **B — блокирующих нет.** Три серьёзных замечания (слоп-кадр в трекере, политика NaN,
  неизмеренная цена развилки при больших углах) правятся строками, а не пересмотром физики.

**Если решает физика — B.** Если A снимет overscroll с параметра `scrollable2D`, A и B по этой
линзе сравняются, и A вырвется вперёд тем, что пролёт гарантирован тем же вызовом, что у
`LazyColumn` в чате, а не тем, что автор передал правильную спеку.

---

## Вердикт: линза «риск»

Всё ниже проверено по распакованным исходникам:
`f195` / `f1100` / `f1102` / `f1106` (foundation), `anim1114`, `animcore1114`, `ui1114`,
и по кэшу Gradle (`~/.gradle/caches/modules-2`). Ни одно утверждение подходов не принято на слово.

---

### 0. Что проверено и подтвердилось у всех трёх

Выдуманных API **не нашлось ни одного**. Это надо сказать первым: все три подхода переписали
разведку по исходникам, и каждая ссылка, несущая вес, устояла.

| Утверждение | Где | Итог |
|---|---|---|
| `scrollable2D(state, enabled, overscrollEffect, flingBehavior, interactionSource)`, только `@Stable` | `f1100/…/Scrollable2D.kt:83-91` | ✅ |
| `flingBehavior` — скалярный `FlingBehavior`, не 2D | там же | ✅ |
| `return consumedOffset.toMagnitudeFloat()`, `= sqrt(x²+y²)` | `Scrollable2D.kt:401,449` | ✅ |
| `if (abs(delta - consumed) > 0.5f) this.cancelAnimation()` | `Scrollable.kt:1053` | ✅ дословно |
| `performFling` зовётся внутри `scroll(MutatePriority.Default){}` | `Scrollable2D.kt:432,452-455` | ✅ |
| `fun Scrollable2DState(consumeScrollDelta: (Offset)->Offset)` — не composable | `Scrollable2DState.kt:104` | ✅ |
| `DefaultScrollable2DState` содержит `MutatorMutex`; `canScroll = true` зашито | `:129,163` | ✅ |
| `ScrollableDefaults.flingBehavior()` → `DefaultFlingBehavior(rememberSplineBasedDecay<Float>())` | `Scrollable.kt:592`, `Scrollable.android.kt:29-32` | ✅ |
| `rememberOverscrollEffect(): OverscrollEffect?` и `Modifier.overscroll(OverscrollEffect?)` — **оба nullable** | `Overscroll.kt:344,276` | ✅ компилируется |
| stretch применяется только при `source == UserInput` | `AndroidOverscroll.android.kt:644` | ✅ |
| `detectDragGestures(orientationLock, …, onDragEnd: (PointerInputChange)->Unit, …)` | `DragGestureDetector.kt:232-244` | ✅ разведка была неправа, оба подхода правы |
| `it.consume()` уже внутри цикла драга | `DragGestureDetector.kt:308-312` | ✅ ручной `consume()` — дубль |
| `VelocityTracker`, `calculateVelocity(Velocity)`, `resetTracking`, `addPointerInputChange` | `ui1114 VelocityTracker.kt:49,88,92,110` | ✅ |
| кламп по `maximumFlingVelocity` у платформы | `Draggable.kt:1057-1060` | ✅ |
| `AnimationState(typeConverter, initialValue: T, initialVelocity: T)` | `animcore1114 AnimationState.kt:303-309` | ✅ |
| `animateDecay(block: AnimationScope<T,V>.()->Unit)`, `cancelAnimation()` | `SuspendAnimation.kt:181-184`, `AnimationState.kt:172` | ✅ |
| `Offset.Companion.VectorConverter` | `VectorConverters.kt:103` | ✅ |
| `VectorizedFloatDecaySpec` — покомпонентно, `maxDuration = maxOf(...)` | `DecayAnimationSpec.kt:133-157` | ✅ дословно |
| `rememberSplineBasedDecay<T>()` пересоздаётся по `density.density` | `SplineBasedFloatDecayAnimationSpec.android.kt:41-48` | ✅ |
| `platformFlingScrollFriction = ViewConfiguration.getScrollFriction()` — top-level `val` | там же `:38` | ✅ юнит-тест кривой невозможен |
| `Velocity.toValidVelocity()` — `internal` | `Draggable.kt:1113` | ✅ |
| `durationScale == 0f` → `playTimeNanos = anim.durationNanos` | `SuspendAnimation.kt:333-338` | ✅ |
| `GraphTelemetry` — ровно 9 полей; `VISIBLE_ROWS = 10` | проект | ✅ |
| в бандле `unittest` нет Robolectric и compose-ui-test, есть `mockk` и `coroutines-test` | `libs.versions.toml:115-121` | ✅ |

---

### Подход A: `scrollable2D` + платформенный `FlingBehavior`

#### Блокирующий риск ⛔ — рекомендованная правка каталога ломает сборку

A §6 предлагает основным вариантом:
```toml
compose-foundation = { module = "androidx.compose.foundation:foundation", version.ref = "compose" }
```
`compose = "1.11.4"` (`gradle/libs.versions.toml:15`). **Такой версии foundation не существует.**

Доказательство с диска, а не рассуждение о поездах:

`~/.gradle/caches/modules-2/metadata-2.107/descriptors/androidx.compose.animation/animation-android/1.11.4/*/descriptor.bin`
содержит подряд:
```
animation-core                 1.11.4
androidx.compose.foundation  foundation-layout   1.10.0     ← модуль ИЗ релиза 1.11.4
androidx.compose.runtime     runtime             …
```
Модуль **самого релиза 1.11.4** зависит на `foundation-layout` **1.10.0**.

`ui-android:1.11.4` несёт на foundation не зависимость, а *constraint*: `1.7.0`,
reason «prevents a regression in Overscroll» — то есть ui 1.11.x спроектирован жить
с foundation другой мажорной линии.

Версии, которые Gradle вообще видел (тот же кэш, те же прогоны, те же репозитории):

| модуль | верхушка списка |
|---|---|
| `runtime` | 1.10.6, **1.11.0, 1.11.1, 1.11.3, 1.11.4** |
| `ui` | 1.10.6, **1.11.1, 1.11.3, 1.11.4** |
| `animation` | 1.10.6, **1.11.0, 1.11.1, 1.11.3, 1.11.4** |
| `foundation` | 1.10.2, 1.10.5, **1.10.6 — и всё** |
| `foundation-layout` | 1.10.5, **1.10.6 — и всё** |

Вывод A «1.11.4 у foundation есть, потому что поезд синхронный» — **опровергнут**. Правка,
записанная как основная, даёт `Could not find androidx.compose.foundation:foundation:1.11.4`
на первом же sync. Чинится одной строкой (`composeFoundation = "1.10.6"`), но в проекте
стоит не она.

#### Что выдержало давление — и выдержало сильно

**1. Механизм «вернул модуль = запрошенный модуль → fling жив» — реален и арифметически замкнут.**
`performFling(available.magnitude)` подаёт **неотрицательную** скорость, `animateDecay` от нуля даёт
монотонно растущий `value`, значит `delta > 0` на каждом кадре; `consumed = sqrt(x²+y²) ≥ 0`.
Сравнение `abs(delta - consumed)` идёт между двумя неотрицательными скалярами, а разложение
`toDecomposedOffset` сохраняет модуль. Возврат `delta` целиком действительно даёт совпадение
модулей. Это не догадка A — это выводится из четырёх строк, которые я прочитал.

**2. Механизм не менялся ни разу внутри всей существующей линии 1.10.x.**
`diff` между `f1100`/`f1102`/`f1106` для `Scrollable2D.kt` и `Scrollable.kt` — **пустой, побайтово**.
То есть ближайший реально возможный апдейт (1.10.0 → 1.10.6) подход A не трогает вообще.

**3. Признак «идёт fling» — не гонка, вопреки опасению.** `MutatorMutex.mutateWith` после
`tryMutateOrCancel` берёт `mutex.withLock` (`MutatorMutex.kt:156-171`). Старый блок держит
мьютекс до конца своего `finally`, значит `finally { drive = Gesture }` в `CameraFlingBehavior`
гарантированно отрабатывает **до** первой дельты нового драга. Порядок доказан, а не выведен.

**4. Декоратор компилируется.** `with(delegate) { performFling(v) }` — ровно тот идиом, которым
пользуется сама foundation (`Scrollable2D.kt:452-455`): диспатч-ресивер от `with`, extension-ресивер
`ScrollScope` от override.

**5. Обёртка не ломает мышь.** `shouldBeTriggeredByMouseWheel` (`Scrollable.kt:1013`) читается
только в 1D `Scrollable.kt:827`; у `Scrollable2D` mouse-wheel-узла нет вовсе. Обёртка
`CameraFlingBehavior` перестаёт быть `ScrollableDefaultFlingBehavior`, и это ничего не задевает.

**6. Плотность у обёрнутого behavior живая.** `updateDefaultFlingBehavior()` чинит только
собственный placeholder узла; A передаёт свой, но он приходит из `rememberSplineBasedDecay`,
который сам пересоздаётся по `density.density`. Мины «забытая плотность» здесь нет.

**7. Жест переживает входящее сообщение.** `Scrollable2DNode.update` сбрасывает pointer input
только при смене `scrollableState` (`Scrollable2D.kt:487-491`). `state.scroll` создаётся в держателе
один раз, `rememberOverscrollEffect()` стабилен по `remember(overscrollFactory)`,
`ScrollableDefaults.flingBehavior()` — по `remember(flingSpec)`. Ни одного сброса от новых узлов.
Это **сильнее** нынешнего `pointerInput(Unit)` + `rememberUpdatedState`: там сохранность жеста
держится на дисциплине автора, здесь — на `equals` элемента.

**8. Побочно приезжает accessibility.** `Scrollable2DNode : SemanticsModifierNode` заводит
`scrollBy`/`scrollByOffset` (`Scrollable2D.kt:273-294`). Сегодня у полотна их нет.

#### Серьёзные замечания

**A1. Единственный документированный контракт лямбды нарушается сознательно.**
KDoc `Scrollable2DState`: *«The amount of scrolling delta consumed must be returned from this lambda
to ensure proper nested scrolling behaviour»*. A возвращает **больше**, чем потребил. Формулировка A
(«эвристика, не контракт») занижает: контракт есть, он документирован, и A его нарушает — ради
недокументированного следствия в чужом `DefaultFlingBehavior`. Компилятор молчит; тест на имеющемся
инструменте невозможен; ошибка проявится как «fling умирает при первом касании стенки», то есть
как ровно тот дефект, ради которого всё затевалось.

**A2. Между 1.9.5 и 1.10.0 механизм действительно ломался — вот как именно.**
В 1.9.5 (`f195/…/Scrollable2D.kt:398-404,451-458`) был второй путь убийства fling'а:
```kotlin
private fun shouldCancelFling(pixels: Offset): Boolean =
    !scrollableState.canScroll(pixels) || !isScrollableNodeAttached.invoke()
…
val cancelFling = if (isFlingContinuationAtBoundsEnabled) !isScrollableNodeAttached.invoke()
                  else shouldCancelFling(pixelsOffset)
if (pixelsOffset != Offset.Zero && cancelFling) throw FlingCancellationException()
```
В 1.10.0 весь этот блок и флаг `ComposeFoundationFlags.isFlingContinuationAtBoundsEnabled` **удалены**.
Практический вывод для A двоякий и он важнее, чем «механизм менялся»:
- на 1.9.5 подход A **тоже** бы работал — но только потому, что `canScroll` зашит в `true`;
- значит слабое место A №8 («починить `canScroll`, чтобы состояние не врало о себе») — это
  **не улучшение, а бомба**: на любом рантайме класса 1.9.5 честный `canScroll` мгновенно убивал бы
  fling через брошенное исключение. Правка, которая выглядит как уборка, откатывает фичу.

**A3. Overscroll вмешивается в путь потребления, и A этого не разбирает.**
`shouldDispatchOverscroll(offset) = scrollableState.canScroll(offset)` (`Scrollable2D.kt:358`),
а `canScroll` всегда `true` → `applyToScroll` вызывается **на каждом кадре fling'а тоже**.
То, что доедет до `DefaultFlingBehavior`, — это `consumedOffset + consumedByDelta`
(`AndroidOverscroll.android.kt:672`), где первое слагаемое — расслабление растяжения, а не движение
камеры. На текущем коде сумма всё равно равна `delta` и fling выживает — я проследил, — но цепочка
A §1 («`scrollBy` вернул → fling жив») короче реальной на одно звено, и звено это чужое и меняется.

**A4. Правка каталога — общесистемная ради одного экрана.** `android-ui` идёт через `api` из
`core/ui` во всё приложение. Явное объявление foundation фиксирует версию для всех модулей;
любое будущее обновление material3, которое попросит foundation свежее, теперь молча получит
downgrade вместо upgrade — и это не сломает сборку, а изменит поведение чужих экранов.

**A5. Дыра в покрытии ровно там, где живёт предсказанный дефект** (A признаёт, п.6). Ни
compose-ui-test, ни Robolectric в `unittest` нет. `GraphCameraTest` проверит арифметику
`cameraStepOf`, а связку «модификатор ↔ состояние ↔ behavior» — ничто.

#### Пинч-зум: A платит дороже всех

Оба детектора — **winner-take-all**, это видно в коде обоих:
- `detectZoom` (`Transformable.kt:340,376-380`): `canceled = event.changes.fastAny { it.isConsumed }`,
  и `while (!canceled …)` — один чужой `consume()` убивает жест насовсем; сам он после слопа
  консьюмит всё, что сдвинулось.
- `DragGestureNode` (через `DragGestureDetector.kt:264,308-312`): `change.consume()` на слопе и
  на каждом событии.

Совместить их можно только чередованием в цепочке и удачей по слопу, а не проектом:
`transformable` даёт ровно один хук — `canPan: (Offset) -> Boolean` (`Transformable.kt:99-104`), и он
гасит только вклад пана в слоп, но не отменяет ни консьюм зума, ни консьюм драга.

Асимметрия, из-за которой A хуже B и C: **в A жест лежит внутри чужого узла, который нельзя
отредактировать.** В B и C добавление зума — правка своего блока `pointerInput`:
`detectDragGestures` → `detectTransformGestures`, который отдаёт `pan`, `zoom`, `rotation` одним
колбэком и корректно работает и с одним пальцем (`event.calculatePan()`). В A выбор такой:
либо драться двумя узлами за поток указателей, либо снять `scrollable2D` — а снять его значит
потерять и механизм затухания, то есть откатиться к B/C целиком.

Отдельно, общее для всех трёх: с зумом дельта приходит в экранных пикселях, а камера обязана стать
в координатах контента (`delta / scale`). Это правка в `consume`/`pan`, одинаковая везде.

#### Виртуализация: ломает все три одинаково, и никто этого не заметил

`cameraRangeOf(placement, viewport)` во **всех трёх** проектах берёт диапазон из `placement` —
результата раскладки **скомпонованных** узлов. Как только полотно начнёт компоновать только видимое,
`placement.bounds` и `centreSpanX` схлопнутся до видимого подмножества, и диапазон камеры поедет
вместе с камерой: границы будут убегать вперёд. Ни один из трёх не разделяет «протяжённость всего
графа» и «что сейчас в композиции». Это общая дыра проектирования, не различитель.

A получает поверх этого свою добавку: сегодня пан не вызывает измерения вовсе (камера читается в
`graphicsLayer`), а при виртуализации каждый кадр пана станет измерением, и `layout()` A будет
пере-зажимать камеру из фазы измерения по диапазону, посчитанному от частичной раскладки, — против
летящего fling'а. То же верно для B (`layout()` тоже пере-зажимает) и C.

#### Данные из домена во время жеста

Переживает, и лучше нынешнего — см. «выдержало давление», п. 7.

#### Что ломается молча — счёт мин

1. Изменившаяся эвристика гашения в чужой библиотеке (проявится как «нет fling'а», не как краш).
2. Возврат «больше, чем потреблено», уехавший в nested scroll, когда появится родитель-скроллер.
3. `dispatchRawDelta` обходит флаг `drive` — сегодня достижим только через nested scroll, то есть
   мина заряжена, но не под ногой.
4. Порядок `clipToBounds`/`scrollable2D`/`overscroll` не выведен из кода отрисовки stretch.
5. Правка каталога меняет версию foundation **всему приложению**.

Итого **5** мин, из них 1 — в самом ядре решения. `MotionDurationScale` и плотность у A закрыты
платформой и минами не являются — это его главное преимущество.

#### Обратимость

Дешёвая. Удалить `CameraFlingBehavior.kt`, вернуть блок `pointerInput` (7 строк), убрать три
`remember` и две строки каталога. Арифметика (`cameraRangeOf`/`cameraStepOf`) и её тесты
переживают откат и переиспользуются в B. **Откат ≈ 6 правок, ноль выброшенных тестов.**

---

### Подход B: своя скалярная физика

#### Блокирующих рисков нет

Все API проверены поимённо и все существуют. Механизм не зависит ни от одной приватной эвристики
foundation. Кривая — публичная `rememberSplineBasedDecay`. Гашение — своё, читаемое, тестируемое.

#### Что выдержало давление

**1. `MotionDurationScale` закрыт правильно и по правильной причине.** `withContext(FlingDurationScale)`
со `scaleFactor = 1f` повторяет `DefaultScrollMotionDurationScale` (`Scrollable.kt:1074-1079`).
Проверено, что это не педантизм: `rememberCoroutineScope()` наследует контекст рекомпозера окна,
а тот несёт `MotionDurationScaleImpl` от системной настройки
(`ui1114 WindowRecomposer.android.kt:333-336`), и при `durationScale == 0f`
`playTimeNanos = anim.durationNanos` уже на первом кадре (`SuspendAnimation.kt:333-338`).

**2. Гонка отмены, которую B записал себе в минус, — на самом деле не гонка.** `flingJob?.cancel()`
и цикл затухания живут на одном главном диспатчере; `withFrameNanos` при отменённом job резюмируется
исключением, и `doAnimationFrame` не выполняется. Ни один кадр не применится после `onDragStart`.
Оговорка: это перестанет быть правдой в тот день, когда fling уедет на `Dispatchers.Default`.

**3. План тестов реально исполним.** `BroadcastFrameClock` в `compose-runtime`, `kotlinx-coroutines-test`
в бандле `unittest` (`libs.versions.toml:118`), `exponentialDecay` — в `animation-core` и **не** тянет
`ViewConfiguration.getScrollFriction()` (тот сидит в `SplineBasedFloatDecayAnimationSpec.android.kt:38`,
другой файл, другой инициализатор класса). Тест `a fling ignores the system animation duration scale`
проверяет именно ту мину, которая иначе была бы невидимой.

**4. Критерий гашения честнее платформенного на этом экране.** `isAxisStuck` смотрит на **направление
броска**, а не на дельту кадра, — и потому переживает нулевую дельту первого кадра, чего критерий
`abs(delta-consumed) > 0.5f` не умеет. Это не подгонка: `panRangeOf` при одной дорожке отдаёт
вырожденный диапазон, и платформенное правило действительно убивало бы любой наклонный флик.

**5. Поправка к разведке верна, и она снимает главную «стоимость» подхода.**
`detectDragGestures(orientationLock = …)` c `onDragEnd: (PointerInputChange) -> Unit` есть и в 1.9.5,
и в 1.10.0 — байт в байт. Опт-ин на ней use-site, вызывающему ничего не нужно.

#### Серьёзные замечания

**B1. Четыре платформенных решения повторены руками — B считает их сам, и счёт верный.**
Порог `> 1f`, кламп `maximumFlingVelocity`, `MotionDurationScale`, терпимость `0.5f`. Компилятор не
проверит ни одно. B честно говорит, что тестом ловится один; на деле ловятся два (duration scale и
порог), кламп скорости — только на устройстве и только дрожащим пальцем.

**B2. Растяжение у края потеряно.** Это самая крупная реальная потеря против A, и B её называет
первой. На полотне без скроллбара stretch — единственный сигнал «дальше истории нет». Вернуть можно:
`applyToScroll(delta, source) { }` и `applyToFling(velocity) { }` публичны (`Overscroll.kt:55-56`),
но это два новых контракта руками, и `source` придётся выставлять самому — а именно от него зависит,
применится stretch или нет (`AndroidOverscroll.android.kt:644`).

**B3. Кламп на записи ничем не принуждён.** У A контракт лямбды — входной билет: не вернув `Offset`,
не скомпилируешься. У B единственная страховка — тест `a rejected delta cannot be banked for later`.
Тест хороший, но его можно удалить, а инвариант — нет.

**B4. Смена ведущего пальца портит скорость** — B признаёт. У платформы дыра та же
(`Draggable.kt:1051-1059` не сбрасывает трекер при смене id), так что это паритет, а не регресс.
Мелочь, которую B пропустил: платформа зовёт `addPointerInputChange(event, offset = nodeOffset)`
(`:1051`) — второй параметр компенсирует смещение самого узла. Для неподвижного полотна не нужен;
станет нужен, если полотно поедет в анимированном контейнере.

**B5. `flingJob` — обычное поле, но `telemetry.onFlingStart` пишет снапшот-состояние из
корутины.** Безобидно, но означает, что «идёт fling» существует в двух местах (job и счётчики),
и рассинхрон их никем не ловится.

#### Пинч-зум

Дешевле всех вместе с C: `detectDragGestures` → `detectTransformGestures` внутри своего
`pointerInput`, скорость по-прежнему своя. Само затухание не переписывается вообще — оно уже
скалярное и уже работает от вектора направления, а зум меняет только перевод дельты в координаты
контента.

#### Виртуализация

Ломается так же, как у A и C (общая дыра `cameraRangeOf(placement, …)`). Одна поблажка: B читает
`cameraRange` из снапшот-состояния каждый кадр цикла, а не снимает на старте, поэтому меняющийся
диапазон доходит немедленно и без телепорта. У A то же свойство есть по построению.

#### Данные из домена во время жеста

Не меняется: `pointerInput(Unit)` + `rememberUpdatedState` остаются как есть. Добавляется
`rememberUpdatedState(AppTheme.motion.flingDecay())` — и это правильно: без него смена плотности
(поворот на устройстве с разной density, изменение системного шрифта) заморозила бы старую спеку
внутри живого `pointerInput(Unit)`.

#### Что ломается молча — счёт мин

1. Забытый `MotionDurationScale` (в проекте есть, тестом ловится).
2. Забытый кламп `maximumFlingVelocity` (тестом не ловится).
3. Порог `> 1f` (ловится тестом `a slow release does not fling at all`).
4. Терпимость `0.5f` в телеметрии (косметика).
5. Кламп на записи, не принуждённый ничем, кроме теста.
6. Кривая: тесты гоняют `exponentialDecay`, а поедет `splineBasedDecay` — совпадение проверяет
   только владелец на устройстве.

Итого **6** мин, но ни одна не в ядре и четыре покрыты тестами или тривиально проверяемы.
Это больше мин, чем у A, и они **мельче**: у A одна мина стоит всей фичи.

#### Обратимость

Самая дорогая по объёму, самая дешёвая по риску. ≈135 строк своего кода и ≈150 строк тестов.
Но откатывать нечего: если физика не ляжет в руку, правится **одна константа или один оператор**
(`&&` → `||` в `isCameraStuck`), а не архитектура. Каталог не тронут, чужие экраны не тронуты,
граф зависимостей не тронут. **Откат из B в A стоит ровно столько же, сколько вход в A с нуля.**

---

### Подход C (foil): покомпонентное затухание

#### Блокирующий риск ⛔ №1 — проект отгружает известно неверную траекторию

C сам это доказывает в §5 и §6.1 и я перепроверил механизм: `DecayAnimationSpecImpl.vectorize` →
`VectorizedFloatDecaySpec`, где `getValueFromNanos` крутит цикл по компонентам, а `getDurationNanos`
берёт `maxOf` (`animcore1114/DecayAnimationSpec.kt:133-157`). Две независимые одномерные анимации
на общем таймере — подтверждено дословно. Доказательство невозможности спасения (`n = 1 ⟺ D → ∞`,
`D` — `private val` уровня файла) корректно: единственный параметр экземпляра — `density`, и он
в отношении осей сокращается.

С точки зрения риска важно не «кривая гнётся», а **как этот дефект будет выглядеть в проде**:
C сам пишет, что первый кадр почти правильный (при 30° ошибка −0.3 %). То есть баг-репорт придёт как
«поехало правильно, приехало не туда», и искать будут в клампе камеры — в коде, который исправен.
Это худший из возможных профилей отказа: правильный старт, неправильный финиш, ложный подозреваемый.

#### Блокирующий риск ⛔ №2 — `MotionDurationScale` найден и не закрыт

C §8.7 находит: при `durationScale == 0f` `playTimeNanos = anim.durationNanos` на первом кадре
(`SuspendAnimation.kt:333-338` — проверено дословно), «весь fling приезжает одной дельтой». И делает
из этого вывод «кламп это переживает, проверить на устройстве стоит».

Но в §2.3 обёртки нет:
```kotlin
flingJob = scope.launch { … AnimationState(…).animateDecay(decay) { … } }
```
`scope` — из `rememberCoroutineScope()`, который наследует `MotionDurationScaleImpl` окна
(`WindowRecomposer.android.kt:333-336`). Значит:
- «Animator duration scale: off» (дев-опции, экономия батареи, «убрать анимации» в спец-возможностях)
  → полотно **телепортируется** вместо броска, тогда как `LazyColumn` в чате в том же приложении
  продолжает нормально летать — потому что `DefaultFlingBehavior` обёрнут (`Scrollable.kt:1042`);
- «0.5×» / «5×» → бросок вдвое быстрее / впятеро медленнее, молча.

Это не «проверить на устройстве», это дефект, найденный и оставленный. Три строки, и C единственный
из трёх, у кого они не написаны.

#### Что выдержало давление

1. **Все API настоящие**, включая неочевидные: `AnimationState(typeConverter, initialValue: T,
   initialVelocity: T)` (`AnimationState.kt:303`), `Offset.Companion.VectorConverter`
   (`VectorConverters.kt:103`), блок `AnimationScope<T,V>.()->Unit` с доступным `cancelAnimation()`.
2. **Разбор `Animatable` против `AnimationState` — верный и полезный всем трём.**
   `Animatable.updateBounds` действительно останавливает анимацию при достижении границы по
   **любой** оси, а `panRangeOf` при одной дорожке вырожден, значит штатный механизм границ на этом
   экране непригоден. Это лучшая находка документа и её стоит унести в общий свод.
3. **Граф зависимостей не трогается** — единственное объективное преимущество перед A, и C честно
   говорит, что оно единственное. Но оно **не отличает C от B**: B тоже ничего не объявляет.
4. **Уточнения к разведке верны**: усечение `.toLong()` в `flingDuration`, максимум выгиба ≈26.9 %,
   «45° инвариантны только по направлению». Аккуратная работа.
5. **Самокритика калиброванная.** C сам опровергает «самый короткий код» и сам проваливает свой
   главный тест. Как foil документ сделан честно.

#### Серьёзные замечания

**C1. Телеметрия рассинхронизируется при быстрых бросках.** `finally { telemetry.onFlingEnd() }`
старой корутины может отработать **после** `telemetry.onFlingStart()` новой: `flingJob?.cancel()`
не ждёт завершения. `isFlinging` залипнет в `false` во время живого броска. У A этой проблемы нет
(`MutatorMutex` держит порядок), у B тоже нет (`onFlingEnd` не заводится).

**C2. `change.consume()` в `onDrag` — дубль** (`DragGestureDetector.kt:311`). Безобиден, но C сам же
пишет в §8.1, что разобрался с этой перегрузкой; оставленный дубль — след недоправленного кода.

**C3. Тестируемость хуже всех и это структурно.** У C **вся геометрия направления живёт внутри
платформенной спеки**, а спеку в JVM-тесте не построить: `platformFlingScrollFriction` —
top-level `val`, инициализируемый `ViewConfiguration.getScrollFriction()` при загрузке класса файла.
`mockkStatic` доступен (mockk в бандле есть), но приёма в репозитории нет ни разу. У B та же
недоступность спеки не мешает: у B в спеке живёт только *кривая*, а *направление* — свой код,
и оно тестируется на `exponentialDecay`.

#### Пинч-зум / виртуализация / данные во время жеста

Ровно как у B — свой `pointerInput`, локальная правка; та же общая дыра с `cameraRangeOf`;
`rememberUpdatedState` на месте.

#### Что ломается молча — счёт мин

1. **`MotionDurationScale` — не закрыт вовсе.**
2. Кривизна траектории (не «сломается», а уже сломано; невидимо в коде, видно только рукой).
3. Недолёт 22.5 % на 45° — не виден **никогда**, сравнивать не с чем.
4. Кламп скорости: `toValidVelocity()` руками чинит только NaN; кламп по `maximumFlingVelocity`
   в §2.4 есть — засчитано.
5. Порядок `onFlingStart`/`onFlingEnd` между корутинами.

Итого **5** мин, из них две — в ядре, и одна из этих двух неустранима.

#### Обратимость

Формально дешёвая (≈49 строк), фактически — обратимость не спасает: дефект не в проводке, а в выборе
спеки, и переход C → B это переписывание цикла затухания целиком плюс новый `FlingDirection`.
То есть **C обратим в B ценой всего специфичного кода C.**

---

### Оценки по линзе риска (0–10, выше — меньше риска)

| | A | B | C |
|---|---|---|---|
| API доказаны по jar-ам | 10 | 10 | 10 |
| приватное / недокументированное в несущей конструкции | 3 | 9 | 7 |
| устойчивость к апдейту foundation | 5 | 8 | 8 |
| граф зависимостей | 3 | 10 | 10 |
| переживает пинч-зум | 3 | 7 | 7 |
| переживает виртуализацию | 4 | 4 | 4 |
| переживает данные во время жеста | 10 | 8 | 8 |
| тихие поломки (мины × вес) | 4 | 7 | 3 |
| обратимость | 9 | 7 | 3 |
| **Итог** | **6** | **8** | **4** |

#### Прямой ответ: блокирующие риски

- **A — да, один, и он дешёвый.** Рекомендованная правка каталога целится в
  `androidx.compose.foundation:foundation:1.11.4`, которой не существует: модуль `animation:1.11.4`
  из того же релиза зависит на `foundation-layout:1.10.0`, а список версий foundation в кэше
  обрывается на 1.10.6. Сборка упадёт на резолве. Чинится заменой одной строки на явный
  `composeFoundation = "1.10.6"` — то есть на «безопасный вариант», который A сам же и описал,
  но не поставил основным. После этой правки блокирующих рисков у A **не остаётся**; остаётся
  крупный неустранимый — управление чужой недокументированной эвристикой без единого теста,
  плюс самая дорогая цена за будущий зум.

- **B — нет.** Ни одного. Самый большой минус (потерянное растяжение у края) — потеря
  функциональности, а не риск обрушения.

- **C — да, два.** (1) Проект сознательно отгружает неверную геометрию, и профиль отказа —
  худший из возможных: верный старт, неверный финиш, ложный подозреваемый в клампе камеры.
  (2) `MotionDurationScale` найден в §8.7 и не закрыт в §2.3 — при системной настройке
  «анимации выключены» полотно телепортируется, пока список в том же приложении летает нормально.
  Второй лечится тремя строками; первый — не лечится, что C и доказывает сам.

#### Одно замечание ко всем трём

`cameraRangeOf(placement, viewport)` во всех трёх берёт границы камеры из раскладки
**скомпонованных** узлов. В день, когда появится виртуализация, у всех трёх границы полотна начнут
убегать вместе с камерой. Это надо развести до, а не после: «протяжённость всего графа» и «что
сейчас в композиции» — разные величины, и сегодня они случайно совпадают.

---

## Вердикт: линза «конвенции»

Судятся `approach-a.md` (scrollable2D), `approach-b.md` (своя скалярная физика), `approach-c.md`
(покомпонентное затухание). Физику и риск не пересуживаю.

**Определение, которым пользуюсь.** *Блокирующее* — нарушение, которое нельзя устранить, не тронув
замысел подхода. Expression body, порядок аргументов, префикс в имени — это *серьёзные замечания*:
чинятся правкой на месте, замысел переживают. Иначе слово «блокирующее» обесценивается.

---

### 0. Что установлено по репозиторию (общая база)

Проверено на диске, ссылки нужны всем трём разборам.

**Состав тестового бандла** (`gradle/libs.versions.toml:115-121`):

```toml
unittest = [ "junit-jupiter-api", "junit-jupiter-params", "kotlinx-coroutines-test", "mockk", "turbine" ]
```

Ни `compose-ui-test`, ни Robolectric — заявка всех трёх подходов верна. `feature/chronology/ui/build.gradle`
подключает только `libs.bundles.unittest` + два `testRuntimeOnly`. Compose-классы на тестовом
classpath присутствуют (`testImplementation` наследует `implementation`, `uikit` → `core:ui` →
`api libs.bundles.android.ui`) — доказательство лежит на диске: `GraphGeometryTest.kt:3` импортирует
`androidx.compose.ui.unit.dp` и тест зелёный.

`returnDefaultValues` **действительно не включён**: в `build-conventions/src/main/groovy/android-library-convention.gradle`
блок `testOptions.unitTests.all` содержит только `useJUnitPlatform()` и логирование. Заявка C верна.

**Приёмов нет в репозитории** (grep по всем `*.kt` вне `build/`):
- `mockkStatic` — **0 совпадений**;
- `BroadcastFrameClock` — **0 совпадений**;
- `runTest` — только в тестах ViewModel (`ConversationViewModelTest`, `BranchViewModelTest`,
  `DirectThreadViewModelTest`), всегда `runTest(testDispatcher)`, никогда с чужими часами.

**Формат защищает правило про expression body.** `build-conventions/src/main/groovy/spotless-convention.gradle`
явно отключает `ktlint_standard_function-expression-body`. То есть проект выключил именно то правило
ktlint, которое *навязывало* бы expression body, — `kotlin.md` §«Expression body запрещён у функций»
поддержан инструментом, и `spotlessApply` эти нарушения **не починит**. А вот `{ a(); b() }` в одну
строку ktlint развернёт сам — такие места я в счёт не беру.

**Куда кладут сущности.** Все шесть соседей по `ui/entity` в теме полотна названы с префиксом:
`GraphNode`, `GraphEdge`, `GraphPlacement`, `GraphTelemetry`, `GraphDebugInfo`, `GraphDebugRow`.
Enum в `entity` — норма (`entity/MessageNodeState.kt`, `entity/TimeGap.kt`,
`auth-session/domain/.../domain/entity/AuthSessionState.kt`).

**Как называют вычисление, а не маппер.** `graphPlacementOf`, `topLaneOf`, `leftOffsetsOf`,
`panRangeOf`, `timelinePanRangeOf` (`GraphGeometry.kt`), `ratesOf` (`GraphCanvasTelemetry.kt:100`) —
суффикс `…Of`, живут рядом с арифметикой. В `mapper/` лежат только межпредставленческие переходы и
только расширениями: `TimeGap.toStepWidth(): Dp`, `GraphTelemetry.toPhaseRows(): List<GraphDebugRow>`,
`GraphDebugInfo.toFactRows(): List<GraphDebugRow>` — **каждый меняет тип**.

**Единственная константа-аргумент, которую придётся отбить.** `kotlin.md` §«Константа — только ради
переиспользования» в самом полотне уже нарушена дважды и осознанно:
`private const val MILLIS_IN_SECOND` (`GraphCanvasTelemetry.kt:124`, одно место чтения — `:109`) и
`private const val NODE_LIMIT` (`GraphTelemetryMappers.kt:55`, одно место — `:18`). Обе с
объясняющим комментарием. Значит обвинение «одноразовая приватная константа» здесь не живёт —
файл по соседству делает то же самое.

**Мёртвый токен в `AppMotion` уже есть.** `largeTween()` (`AppMotion.kt:73`) и питающий его
`decelerate` (`:111`) — **ноль вызовов во всём репозитории** (grep). При этом KDoc `largeTween`
обещает «перелёты камеры, стягивание линии», а KDoc `decelerate` — «Для камеры на полотне».
Дизайн-система уже заявила права на камеру полотна и уже не исполняет заявку.

---

### Подход A — `scrollable2D` + платформенный `FlingBehavior`

#### Блокирующие нарушения

**Нет.**

#### Серьёзные замечания

**A1. Решающая арифметика уходит туда, где бандл её не достаёт — и A этого не признаёт.**
`GraphGeometry.kt:83-85` формулирует дисциплину прямо: «Чистая функция, и это не эстетика: вся
арифметика раскладки, в которой случились все регрессии этой фичи, здесь проверяется юнит-тестом,
а не глазами на устройстве». `cameraStepOf` этому отвечает — но его результат зависит от аргумента
`drive`, а весь жизненный цикл `drive` живёт в двух непроверяемых местах: `CameraFlingBehavior`
(§3, декоратор с `try/finally`) и `GraphCanvasState.consume()`. Тест-лист §7 не содержит **ни одного**
теста держателя — `GraphCanvasStateTest` у A попросту нет, хотя конструирование `GraphCanvasState()`
и вызов `layout(...)` бандлом покрываются (B это и делает). Итог: правило «поглощать под затуханием,
честно потреблять под пальцем» — центральное решение A (§5) — проверяется юнит-тестом ровно наполовину:
функция да, взведение флага нет. Сам A это знает (слабое место №3, «признак „идёт fling“ едет на
декораторе-наблюдателе»), но выводит в раздел рисков, а не в раздел тестов.

**A2. `cameraRangeOf` уезжает в новый файл по обоснованию, которое опровергается тем самым файлом,
из которого уезжает.** A §1: «разведка предлагала положить это в `GraphGeometry.kt`, но там „где
стоят узлы“, а тут „где может быть камера“ — другая тема и другой тест». Открываем `GraphGeometry.kt`:

- `:135-152` `timelinePanRangeOf` — KDoc «**Допустимый сдвиг содержимого** по оси времени»;
- `:154-182` `panRangeOf` — KDoc «**Допустимый сдвиг содержимого** по одной оси», и дальше
  «Камера — это сдвиг содержимого: `экран = полотно + камера`».

«Где может быть камера» уже лежит в `GraphGeometry.kt`, и тесты этого уже лежат в
`GraphGeometryTest.kt:81-143` (шесть тестов, из них четыре про `panRangeOf`/`timelinePanRangeOf`).
A раскалывает одну тему на два файла и два тест-класса, оставив вызываемых в старом файле, а
вызывающего унеся в новый. Замысла это не трогает — но заявленная причина ложная, а по прецеденту
прав здесь C.

**A3. Имена сущностей выпадают из своего пакета.** `CameraRange`, `CameraStep`, `CameraDrive` рядом с
шестью `Graph*`. Писаного правила нет — есть шесть соседей подряд. B это заметил и назвал
`GraphCameraRange`/`GraphPanStep`.

**A4. Expression body.** `entity/CameraRange.kt`:

```kotlin
fun clamp(camera: Offset): Offset =
  Offset(camera.x.coerceIn(x), camera.y.coerceIn(y))
```

`kotlin.md` §«Expression body запрещён у функций». Под исключения не подпадает (не `val`, не
лямбда-билдер). `spotlessApply` не починит — правило ktlint выключено намеренно
(`spotless-convention.gradle:14`). Одна строка, но она есть. Рядом `val resting: Offset get() = …` —
законно, `val` правило не затрагивает.

**A5. Решение §5 не имеет места в коде.** Развилка «диагональ, упёршаяся одной осью» — самое спорное
решение A, изложено на 15 строк в плане, и нигде не сказано, что этот текст переезжает в KDoc
`cameraStepOf`. Дом-дисциплина обратная: причина живёт при коде (`GraphGeometry.kt:129-130` —
комментарий про «минимум и максимум, а не первый с последним»; `GraphCanvasTelemetry.kt:9-16`;
`workflow.md`: «У каждого правила записана причина. Правило без причины переоткрывают на следующем
ревью»). B единственный сказал, где причина будет жить.

**A6. Правка общего каталога версий из фичи.** `[bundles] android-ui` кормит все UI-модули
(`core/ui/build.gradle:13` отдаёт его через `api`). Правило прямого запрета в `.claude/rules` нет,
поэтому это замечание, а не нарушение: просто отмечаю, что стоимость решения выходит за
`feature/chronology`. Версионный риск — не моя линза, отсылаю к вердикту риска.

#### Что выдержало давление

**A7. Фазовая дисциплина — лучшая из трёх, и она адресная.** `compose.md` §«Наружу — `State<T>`»
объясняет: «Промах на один уровень (чтение в теле композабла вместо фазы рисования) уже приводил к
бесконечному циклу измерения», и то же повторено в KDoc `GraphCanvasState.kt:44-48`. У A:
- наружу отдаётся `val offset: State<Offset>` ✔;
- запись из фазы измерения **условная**: `if (isMoved) { val clamped = …; if (clamped != camera) camera = clamped }`.
  Это единственный из трёх сторожей, который виден глазом, а не держится на политике сравнения
  по умолчанию;
- диапазон в `layout()` считается **от аргументов** (`cameraRangeOf(result, viewportSize)`), а не от
  только что записанных снапшот-полей. Это ровно то рассуждение, которое уже записано в KDoc
  `layout()` (`GraphCanvasState.kt:136-143`): «фаза размещения получает то же значение, что посчитала
  фаза измерения, и рассинхронизировать их нечем». A эту мысль не сломал, а продолжил.

Чтение `placement`/`viewport` внутри `consume()` — из корутины `scroll {}`, не из фазы Compose,
подписчика не создаёт. Заявка A корректна.

**A8. Телеметрия обычными полями — соблюдено, и `drive` тоже обычное поле.** `GraphCanvasTelemetry.kt:9-13`:
«Все поля — **обычные**, не снапшот-состояние… Инструмент не должен вызывать то, что измеряет».
A добавляет `gesturePans`/`flingPans`/`blockedFlings` обычными `Int` и отдельно оговаривает, что
`drive` — «обычное поле, не снапшот: читается только внутри лямбды потребления». Снапшот-флага для
панели не заводит. ✔ Пункт 3 сдан.

**A9. Публичный `val scroll: Scrollable2DState` — не течь.** Формально наружу отдан изменяемый
держатель. Но дом-стиль это уже допускает: `PredictiveBackController.kt:28-31` отдаёт наружу два
`Animatable` публично, а `:34, :38, :42, :46, :50` — `var … private set`. Обвинение не выживает.

**A10. Композабл становится чище, а не грязнее.** KDoc `GraphCanvas.kt:34` обещает: «Композабл здесь
ничего не считает — только композирует, принимает жест и рисует». У A из композабла уходят
`pointerInput`, `detectDragGestures`, `change.consume()` и `rememberUpdatedState` — обещание
выполняется буквальнее, чем сегодня. Три `remember` взамен — это сборка сотрудников, а не счёт.

**A11. Отказ от токена аргументирован правилом репозитория, а не вкусом.** A: «токен, который никто
не читает, — ложное обещание настройки». Это дословно причина из `kotlin.md` §«Константа — только
ради переиспользования»: «Константа обещает, что значение используется где-то ещё; когда это
неправда, обещание приходится каждый раз перепроверять». И у аргумента есть подтверждение на диске:
`AppMotion.largeTween()` — ноль вызовов, а его KDoc обещает «перелёты камеры». Такой токен в файле
уже лежит и уже никем не читается.

Контр-давление, которое я обязан приложить: `compose.md` открывается словами «Проект живёт на
собственной дизайн-системе `AppTheme` (цвета, типографика, **`AppMotion`**, `AppShapes`,
`Elevation`), не на Material», а A берёт движение камеры у `ScrollableDefaults`. Аргумент не проходит:
физика скролла — не переход, и списки в приложении уже летят по платформенной кривой без всякого
токена. Отказ A законен.

**A12. Имена тестов — предложением в бэктиках, как в существующих классах.** ✔
`consumed delta is truncated at the boundary`, `a fling slides along a wall it has hit` — та же
форма, что `a plate wider than the gap does not swallow its neighbour` (`GraphGeometryTest.kt:67`).
Все девять предложенных тестов бандлом запускаются: это чистые функции над `Offset`/`IntSize`,
Android не трогают. Заявка «связка модификатор ↔ состояние ↔ fling behavior не покрывается» — верна и
названа честно.

---

### Подход B — своя скалярная физика

#### Блокирующие нарушения

**Нет.**

#### Серьёзные замечания

**B1. Четыре expression body, один — в чужом файле, где так не пишут.**
`kotlin.md` §«Expression body запрещён у функций», исключения — только `val` и функция-лямбда-билдер:

| место | код |
|---|---|
| `GraphCamera.kt` | `internal fun isCameraStuck(...): Boolean =\n  isAxisStuck(...) && isAxisStuck(...)` |
| `FlingDirection.kt` | `fun offsetOf(distance: Float): Offset = vector * distance` |
| §4 | `private fun isStalled(requested: Float, consumed: Float): Boolean =\n  abs(requested - consumed) > 0.5f` |
| `AppMotion.kt` §7 | `fun flingDecay(): DecayAnimationSpec<Float> = rememberSplineBasedDecay()` |

Последний тяжелее прочих: в `AppMotion.kt` **все шесть** функций-фабрик написаны блоком с `return`
(`:32-35`, `:38-47`, `:50-54`, `:57-61`, `:64-69`, `:73-78`). B вставляет туда единственную строку
с `=`. Плотность нарушений — самая высокая из трёх; чинится механически, но выдаёт, что
`kotlin.md` §«Expression body» не читали.

**B2. Заявленный флагман тестируемости в текущем виде не запустится.** §8 §«Главное преимущество»
строит `GraphFlingTest` так:

```kotlin
runTest {
  val clock = BroadcastFrameClock()
  …
  state.fling(this + clock, Velocity(1200f, 700f), exponentialDecay())
  var nanos = 0L
  while (clock.hasAwaiters) { … }
}
```

`runTest` даёт `TestScope` на `StandardTestDispatcher` — `scope.launch` только **ставит задачу в
очередь**, не исполняя её. К моменту первой проверки `clock.hasAwaiters` корутина ещё не стартовала,
ожидающих у часов нет, цикл не выполняется **ни разу**, `trail` пуст. Дальше `runTest` при
завершении дожимает детей — корутина стартует, виснет в `withFrameNanos`, кадров ей больше никто не
шлёт, и тест падает по таймауту `runTest`. Нужен `runCurrent()` **до** цикла. Дефект точечный
(одна строка), но он в единственном месте, ради которого B заявляет преимущество перед A и C, и B
сам пишет: «Ни один тест не запускался».

Отдельно к конвенции: приёма в репозитории нет (grep: `BroadcastFrameClock` — 0, `runTest` только с
`testDispatcher` в тестах ViewModel), а режим отказа у него — **зависание**, при том что
`./gradlew test` в `kotlin-convention.gradle`/`android-library-convention.gradle` таймаута на тест не
задаёт. B заводит в проект новую тестовую машинерию — это законно, но заявлять её как «бесплатное
преимущество» рано.

**B3. Записей снапшот-состояния из фазы измерения у B становится больше, а сторож — самый неявный.**
Хвост `layout()` у B пишет `viewport`, `placement`, **`cameraRange`** (новое снапшот-поле) и
**`camera.value` безусловно, каждый проход измерения**. Сторож назван — «`mutableStateOf` со
структурным сравнением гасит пере-зажатие вхолостую» — и он настоящий (`mutableStateOf` по умолчанию
`structuralEqualityPolicy`). Но это единственный из трёх сторожей, которого не видно в коде: он
держится на политике сравнения по умолчанию, которую следующий читатель обязан знать наизусть.
Сравните с A (`if (clamped != camera)`). Учитывая, что KDoc `GraphCanvasState.kt:44-48` заведён
ровно потому, что промах в этой теме уже стоил бесконечного цикла измерения, невидимый сторож —
шаг назад от нынешней явности.

Побочно: B заменяет `val offset: State<Offset> = derivedStateOf { … }` на `= camera`. Утечки нет
(объявленный тип `State<Offset>`), но покой камеры перестаёт быть *выводимым* и становится
*записываемым из измерения*. `debugInfo` при этом остаётся `derivedStateOf` — B об этом не говорит.

**B4. Четыре платформенных решения переписаны руками, тремя из них никто не сторожит.** B перечисляет
честно (слабое место №7): порог `> 1f`, кламп по `maximumFlingVelocity`, `MotionDurationScale`
со `scaleFactor = 1`, терпимость `0.5 px` — «Из четырёх тестами ловится один». По линзе конвенций
это тот же класс долга, против которого написан KDoc `GraphGeometry.kt:83-85`: значение, от которого
зависит поведение, обязано иметь тест, иначе его переоткроют на следующем ревью.

**B5. Токен назван не по форме соседей.** `fun flingDecay(): DecayAnimationSpec<Float>` при том, что
в `AppMotion.kt` все четыре спек-фабрики параметризованы: `fun <T> smallestTween(): TweenSpec<T>`,
`fun <T> mediumTween()`, `fun <T> largeTween()`. `rememberSplineBasedDecay<T>()` дженерик допускает.
C здесь попал точнее.

#### Что выдержало давление

**B6. Пункт 1 линзы сдан лучше всех.** Каждое решение, которое может дать регрессию, — чистая функция
с именем и тестом: `panStepOf` (кламп на записи), `isCameraStuck`/`isAxisStuck` (когда гасить),
`FlingDirection` (разложение по осям), `cameraRangeOf`. Плюс — единственный из трёх — B предлагает
`GraphCanvasStateTest` на сам держатель (`a layout pass re-clamps the stored camera`,
`an untouched camera follows new nodes`, `a touched camera stays where the user left it`), и это
бандлом покрывается: `GraphCanvasState()` конструируется без Android, `Density(1f)` — чистый Kotlin.
Именно этого нет у A.

**B7. `FlingDirection` в `components/canvas`, а не в `entity` — прав по прецеденту.** Возражение
«сущности лежат в `entity`» (`architecture.md`) не проходит: `GraphGeometry` —
`@JvmInline value class` с методом `laneYOf`, вычислительный объект, и он лежит в
`components/canvas/GraphGeometry.kt:25`, а не в `entity`. `FlingDirection` — тот же жанр.
Отдельный файл на класс ✔ (`kotlin.md` §«Один класс — один файл»).

**B8. Имена сущностей ложатся в пакет.** `GraphCameraRange`, `GraphPanStep` — семь `Graph*` подряд
вместо шести. Единственный подход, который это заметил.

**B9. Обвинение по константам не выживает.** `private val FlingDurationScale = object : MotionDurationScale`
— одно место чтения, и по букве `kotlin.md` §«Константа — только ради переиспользования» его следует
вписать по месту. Но соседние файлы полотна делают ровно так же и с тем же оправданием — объясняющим
комментарием: `MILLIS_IN_SECOND` (`GraphCanvasTelemetry.kt:124`, одно чтение),
`NODE_LIMIT` (`GraphTelemetryMappers.kt:55`, одно чтение). У B комментарий тоже есть. Снимаю.

**B10. Телеметрия — образцовая по букве и по причине.** `flingSteps`/`flingStalls` — обычные `Int`;
`lastFlingVelocity: Velocity … private set` — обычное поле, как существующее
`lastPan` (`GraphCanvasTelemetry.kt:32-33`); `flingJob: Job?` с явной мотивировкой «обычное поле:
за него никто не рисует». Снапшот-флага «идёт инерция» B заводить **отказывается** и объясняет,
почему: «за него никто не рисует, он нужен только панели и живёт счётчиками телеметрии» (§2). Это
дословно рассуждение `GraphCanvasTelemetry.kt:9-16`. ✔ Пункт 3 сдан с запасом.

**B11. Обещание KDoc закрыто честнее всех — и заменяющий текст показан.** Обещание
(`GraphCanvasState.kt:59-60`): незажатый сдвиг «понадобится для оттяжки за край и для затухания
инерции». B не стирает его, а опровергает, и опровержение живёт в KDoc `panStepOf`:

> «Кламп стоит на записи, а не на чтении, и это не перестановка мест. Накапливая незажатый сдвиг,
> состояние банкует перерегулирование: упор на три тысячи пикселей превращается в мёртвую зону такой
> же величины, которая сама не рассасывается и вылезает телепортом при следующей раскладке.»

Плюс два теста именно на это: `a rejected delta cannot be banked for later`,
`banked overshoot cannot outlive a layout pass`. Единственный из трёх, кто **показал заменяющий
KDoc текстом**, а не пересказал намерение в плане. И единственный, кто сказал, где живёт причина
спорного решения: «Причина записана в KDoc `isCameraStuck`, где решение и живёт» (§6).

**B12. Тон и плотность комментариев — ближе всех к окружению.** Причина при каждом решении, ссылка на
платформенный файл:строку, называние конкретного дефекта («банкует перерегулирование», «телепортом»),
предупреждение о NaN у трекера. Это тот же регистр, что `GraphGeometry.kt:174-176` («Именно `0f - min`,
а не `-min`: у нуля унарный минус даёт отрицательный нуль, и граница печаталась как „-0.0“»).

**B13. Токен — действительно токен, одобрения не требует.** `workflow.md` §«Как пополнять правила»
управляет файлами в `.claude/rules/`, а не дизайн-системой. `flingDecay()` по форме — близнец
`mediumTween()`/`largeTween()`: `@Composable`-фабрика спеки в `AppMotion`. Заявка B верна. И в отличие
от A, у B токен **читается** — из `GraphCanvas`, — то есть возражение «ложное обещание настройки»
к B не относится.

**B14. Имена тестов ✔.** `a rejected delta cannot be banked for later`,
`a fling dies when the only live axis hits its wall` — форма и артикль как в
`GraphGeometryTest.kt`/`GraphCanvasTelemetryTest.kt`.

---

### Подход C — покомпонентное затухание (foil)

#### Блокирующие нарушения

**C1. Несущая арифметика кладётся туда, куда инструмент проекта не дотягивается, — и C это знает.**

Дисциплина записана в двух местах и обе — про эту самую фичу:

- `GraphGeometry.kt:83-85`: «Чистая функция, и это не эстетика: вся арифметика раскладки, **в которой
  случились все регрессии этой фичи**, здесь проверяется юнит-тестом, а не глазами на устройстве»;
- `entity/GraphNode.kt` KDoc: «Здесь только то, из чего считается положение, **чтобы раскладку можно
  было проверить юнит-тестом без Compose**».

У C направление и длина полёта — не его код, а свойство `SplineBasedFloatDecayAnimationSpec`. Чтобы
их проверить, тест обязан построить эту спеку, а она при инициализации читает
`ViewConfiguration.getScrollFriction()`. C сам выводит следствие (§2.7): нужен
`mockkStatic(ViewConfiguration::class)` — «Приёма, которого в репозитории нет ни разу». Проверил:
верно, **0 совпадений**. И `returnDefaultValues` действительно выключен — проверил, в
`android-library-convention.gradle` блок `testOptions` содержит только `useJUnitPlatform()` и логирование.

Хуже, чем C пишет: `platformFlingScrollFriction` — top-level `val`, то есть читается при
инициализации класса файла. Порядок тестов внутри Gradle-воркера не управляем; если класс успел
инициализироваться раньше мока, он мёртв в этой JVM навсегда (`ExceptionInInitializerError` →
`NoClassDefFoundError` на всех последующих обращениях). Запасной выход C сам по себе ненадёжен.

И финальный аккорд: единственный тест, который отличает C от B по существу —
`a diagonal fling keeps its direction` — C **заранее объявляет красным** (§2.7 «Провал», §5
«Провалю на любом угле, кроме 0°, 45° и 90°»). То есть проект предлагает положить в
`feature/chronology/ui/src/test` заведомо падающий тест либо не писать его вовсе, оставив несущее
поведение непокрытым.

По моей линзе это блокирующее: не потому что цифра плохая (это не моя линза), а потому что
**замысел подхода делает невыполнимой ту дисциплину, ради которой в этой фиче вся арифметика и была
вынесена в чистые функции**. Устранить, не тронув замысел, нельзя — «общие часы = одна скалярная
величина прогресса = подход B» — это вывод самого C (§6.1).

#### Серьёзные замечания

**C2. Единственный из трёх, кто не назвал сторожа против цикла измерения.** §2.2 ограничивается
строкой `fun layout(...): GraphPlacement   // + пере-кламп сохранённой камеры`. Что удерживает
запись `cameraX`/`cameraY` из фазы измерения от повторного запуска измерения — не сказано нигде.
Механически безопасно (`mutableFloatStateOf` тоже сравнивает структурно), но безопасно **случайно**.
Это тем заметнее, что C — единственный, кто цитирует KDoc `GraphCanvasState` построчно
(`:59-60`), и при этом проходит мимо `:44-48` того же KDoc, где записано: «Промах на один уровень —
чтение в теле полотна вместо фазы рисования — уже приводил к бесконечному циклу измерения».
A ставит `if (clamped != camera)`, B хотя бы называет политику сравнения. C — ничего.

**C3. `Velocity.toValidVelocity()` в `mapper/VelocityMappers.kt` — не маппер.**
`kotlin.md` §«Мапперы — в пакет `mapper`» определяет предмет: «Преобразование **одного представления
в другое**». `Velocity → Velocity` — не смена представления, а санация NaN. Правило имени тоже не
сходится: «в остальных случаях — `to<Результат>()`» даёт для результата `Velocity` имя `toVelocity()`,
что бессмысленно; `toValidVelocity` — прилагательное вместо результата, а правило прямо запрещает
изобретать формы («Заводить `toDomain`, `mapToX`, `toXParams` нельзя»). Все три существующих маппера
модуля меняют тип: `TimeGap → Dp`, `GraphTelemetry → List<GraphDebugRow>`,
`GraphDebugInfo → List<GraphDebugRow>`. Место функции — рядом с `fling` в `components/canvas`, имя —
по образцу соседей (`…Of`). Правка на три строки, замысла не касается.

**C4. Expression body в `AppMotion.kt`.**
`fun <T> flingDecay(): DecayAnimationSpec<T> = rememberSplineBasedDecay()` — как и у B, единственная
строка с `=` среди шести соседей с `return`. `kotlin.md` §«Expression body запрещён у функций»,
`spotlessApply` не чинит.

**C5. Имена сущностей выпадают из пакета.** `CameraRange`, `CameraStep` рядом с шестью `Graph*` —
то же замечание, что A3.

**C6. Аналитика адресована плану, а не коду.** §3-§6 — сильнейший разбор из трёх (таблицы увода,
недолёт, доказательство неспасаемости в §6.1). В коде C — два `//` внутри `fling` и одна строка KDoc
у токена. Дом-дисциплина обратная: «У каждого правила записана причина. Правило без причины
переоткрывают на следующем ревью» (`workflow.md`). Ни одно из шести уточнений §8 не имеет
названного адреса в коде.

**C7. `DISTANCE_EXPONENT` (§6.2) — одноразовая константа.** Читается один раз (`1f / DISTANCE_EXPONENT`).
Формально `kotlin.md` §«Константа — только ради переиспользования». Снимаю почти полностью: §6.2 —
отвергнутое самим C спасение, в проект не идёт, да и прецеденты `MILLIS_IN_SECOND`/`NODE_LIMIT`
такое допускают.

#### Что выдержало давление

**C8. По размещению `cameraRangeOf`/`cameraStepOf` прав именно C, и это прямой удар по A и B.**
C кладёт их в существующий `GraphGeometry.kt` «оба рядом с `timelinePanRangeOf`/`panRangeOf`».
Прецедент на его стороне целиком: `cameraRangeOf` — тонкая композиция ровно этих двух функций
(`GraphGeometry.kt:146` и `:169`), их тесты уже в `GraphGeometryTest.kt:81-143`, а сам файл давно не
«где стоят узлы», а «арифметика полотна»: в нём семь top-level функций, из них две — про допустимый
сдвиг камеры. Возражение «файл про пространство, `GraphGeometry.kt:15`» не проходит: та фраза —
KDoc `value class GraphGeometry`, а не файла, и файл уже импортирует `Rect`, `Dp`, `IntSize`.

**C9. Обещание KDoc отработано точнее всех по форме постановки вопроса.** C — единственный, кто
цитирует обещание с координатами (`GraphCanvasState.kt:59-60`) и отвечает **на обе его половины**
раздельно: «оттяжку делает `OverscrollEffect` своим состоянием, а затуханию нужен сигнал отказа,
а не банк». B закрыл его сильнее (показал заменяющий KDoc и два теста), но C поставил вопрос честнее
всех: он единственный процитировал то, что стирает.

**C10. Телеметрия — пункт 3 сдан с прямой ссылкой на механизм.** «`isFlinging` — обычное поле (панель
снимает по таймеру, `GraphDebugOverlay.kt:69-80`)». Это единственная из трёх формулировок, которая
ссылается на конкретный цикл `LaunchedEffect(telemetry) { … delay(TICK_MILLIS) … }`, а не на общее
правило. Снапшот-флага не заводит. `lastPan` перестаёт затираться затуханием — отдельное `lastFling`;
у A то же решение, у B тоже. ✔

**C11. `State<T>` соблюдён.** `val offset: State<Offset> = derivedStateOf { … }`, `cameraX`/`cameraY`
приватные `mutableFloatStateOf`, наружу — ничего изменяемого. Формально чище A (тот отдаёт наружу
`Scrollable2DState`), хотя у A на это есть прецедент.

**C12. Граф зависимостей не трогается.** `feature/chronology/ui/build.gradle` и
`gradle/libs.versions.toml` остаются как есть. По линзе конвенций это плюс к A6: изменение фичи
остаётся внутри фичи.

**C13. Имена тестов и честность списка ✔.** Форма совпадает; провальный тест помечен провальным, а не
вычеркнут из списка. Для foil'а это ровно то поведение, которого от него ждут.

---

### Оценки

| | A (`scrollable2D`) | B (своя физика) | C (покомпонентное) |
|---|---|---|---|
| 1. Чистая тестируемая арифметика | частично: `drive` и проводка вне тестов | **лучший**: каждое решение — функция + тест держателя | **провал**: геометрия внутри платформенной спеки |
| 2. Наружу `State<T>` + сторож фазы | **лучший**: явный `if`, диапазон от аргументов | сторож назван, но невидим; записей больше | сторож **не назван вовсе** |
| 3. Телеметрия обычными полями | ✔ | ✔ с объяснением | ✔ со ссылкой на механизм |
| 4. Тесты без Compose-рантайма | все запустятся; дыра честно названа | набросок не запустится (нужен `runCurrent()`); машинерии в репо нет | нужен `mockkStatic`, которого нет; ключевой тест заведомо красный |
| 5. Раскладка и размещение | раскол темы, причина ложная | тот же раскол, но имена в пакет ложатся | **прав**: рядом с `panRangeOf` |
| 6. Токен в `AppMotion` | отказ **обоснован правилом репо** | токен законный, но не дженерик | токен законный, форма соседей соблюдена |
| 7. Обещание KDoc про незажатый сдвиг | закрыто делом (overscroll), но не текстом | **закрыто текстом + двумя тестами** | процитировано и разобрано по половинам |
| 8. Стиль, имена, KDoc | 1 expression body; §5 без адреса в коде | **тон совпадает**, но 4 expression body | 1 expression body; аналитика мимо кода |

**A — 7.5.** Самая крепкая фазовая дисциплина и единственный отказ от токена, опирающийся на
записанную причину из `kotlin.md`, а не на вкус. Теряет на том, что решающий флаг `drive` живёт в
декораторе и держателе, тестов на держатель не предложено вовсе, а раскол `GraphCamera.kt` обоснован
утверждением, которое опровергается KDoc `panRangeOf` в том же каталоге. Блокирующих нет.

**B — 8.** Ложится в дом-стиль плотнее всех: причина живёт в KDoc при решении, телеметрия обычными
полями с проговорённой мотивировкой, имена сущностей продолжают ряд `Graph*`, каждое решение —
чистая функция с тестом, включая тесты самого держателя. Платит четырьмя expression body (один — в
`AppMotion.kt`, где так не пишет никто), самым неявным сторожем против записи из фазы измерения и
флагманским тестом, который в набросанном виде не запустится. Всё это чинится, не трогая замысел.
Блокирующих нет.

**C — 5.5.** Прав по размещению арифметики (единственный) и честнее всех поставил вопрос об
обещании KDoc. Но его замысел выводит несущую арифметику за пределы того, что бандл `unittest`
способен проверить, требует приёма (`mockkStatic`), которого в репозитории нет ни разу и который
против top-level `val` ненадёжен, и оставляет в фиче заведомо красный тест. Плюс единственный не
назвал сторожа против цикла измерения — в подходе, который цитирует KDoc этого самого класса.

### Прямой ответ

**Блокирующее нарушение конвенций есть у одного — у C.** Формулировка: подход C делает невыполнимой
дисциплину, записанную в `GraphGeometry.kt:83-85` и `entity/GraphNode.kt` («вся арифметика, в которой
случились все регрессии этой фичи, проверяется юнит-тестом без Compose»), потому что несущее поведение
живёт внутри платформенной спеки, недостижимой для тестового бандла, и C сам объявляет ключевой тест
провальным. Устранить это, не тронув замысел, нельзя — §6.1 самого C доказывает, что спасения,
оставляющего покомпонентность, не существует.

У A и B блокирующих нарушений нет: всё найденное у обоих — expression body, имена, размещение файлов,
отсутствующий `runCurrent()`, невидимый сторож — правится на месте и замысел переживает.

---

#### Сноска, ни на кого не работающая

Глобальное правило пользователя требует комментарии в коде только на английском; репозиторий
сплошь комментирован по-русски (`GraphCanvasState.kt`, `GraphGeometry.kt`, `PredictiveBackController.kt`
— все KDoc русские, английские попадаются лишь точечно, `AppMotion.kt:80, 94`). Все три подхода
следуют репозиторию. Различия между подходами здесь нет, на оценку не влияет; отмечаю, потому что
конфликт реальный и всплывёт при первом же ревью.
