package ru.sla.atlas.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.delay
import ru.sla.atlas.debug.entity.DebugRow
import ru.sla.atlas.debug.entity.DebugSnapshot
import ru.sla.atlas.debug.mapper.toFactRows
import ru.sla.atlas.debug.mapper.toPhaseRows
import ru.sla.atlas.entity.Node
import ru.sla.atlas.entity.Telemetry
import ru.sla.atlas.ui.AtlasCanvasState
import ru.sla.atlas.ui.peaksOf
import ru.sla.atlas.ui.ratesOf

/**
 * Отладочная панель полотна.
 *
 * Панель отвечает на вопросы, которые не разделить по внешнему виду графа: доходит ли жест до
 * обработчика, не упёрлась ли камера в границу и — главное — не крутится ли какая-то фаза Compose
 * сама по себе. Зависший экран выглядит одинаково при любой причине, а здесь видно, какая именно
 * фаза тикает, пока пользователь ничего не делает. Строка с вышедшим за норму значением краснеет.
 *
 * Счётчики приходят обычными полями и снимаются по таймеру, а не по подписке: инструмент не должен
 * вызывать то, что измеряет. Снапшот-запись из фазы измерения, прочитанная в композиции, сама и
 * есть тот бесконечный цикл, который тут ищут.
 *
 * Данные о камере, наоборот, читаются как состояние — поэтому панель рекомпонуется на кадрах
 * панорамирования. Это единственное место в полотне, где такое чтение допустимо.
 *
 * @param state камера полотна и результат его последней раскладки
 * @param levelLabelOf имя уровня детализации словами: как называются уровни, знает вызывающий
 * @param onBoundsChanged куда панель встала и когда её не стало: полотно ловит жест на всём
 *   вьюпорте и по этой зоне отличает палец, положенный на панель, от пальца на графе. Убранная
 *   панель обязана снять зону за собой — иначе полотно продолжит обходить стороной пустое место
 * @param modifier модификатор панели
 */
@Composable
fun <N : Node, L> DebugOverlay(
  state: AtlasCanvasState<N, L>,
  levelLabelOf: (L) -> String,
  onBoundsChanged: (key: Any, bounds: Rect) -> Unit,
  modifier: Modifier = Modifier
) {
  val telemetry = state.telemetry
  SideEffect { telemetry.onOverlayComposition() }

  val currentOnBoundsChanged by rememberUpdatedState(onBoundsChanged)
  DisposableEffect(Unit) {
    onDispose { currentOnBoundsChanged(ZONE_KEY, Rect.Zero) }
  }

  val info by state.debugInfo
  var totals by remember { mutableStateOf(Telemetry.Empty) }
  var rates by remember { mutableStateOf(Telemetry.Empty) }
  var peak by remember {
    mutableStateOf(
      DebugSnapshot(
        rates = Telemetry.Empty,
        totals = Telemetry.Empty,
        info = state.debugInfo.value,
        lastPan = telemetry.lastPan,
        lastFling = telemetry.lastFling
      )
    )
  }

  // Один снимок на такт и одна запись состояния — панель обновляется сама, не дожидаясь жеста,
  // и при этом не подписана ни на что из фаз измерения и рисования.
  LaunchedEffect(telemetry) {
    var previous = telemetry.read()
    var previousMillis = System.currentTimeMillis()
    while (true) {
      delay(TICK_MILLIS)
      val current = telemetry.read()
      val millis = System.currentTimeMillis()
      totals = current
      val tickRates = ratesOf(previous, current, millis - previousMillis)
      rates = tickRates
      val updatedPeaks = peaksOf(peak.rates, tickRates)
      if (updatedPeaks != peak.rates) {
        // Состояние читается из корутины, а не из композиции: подписки это не создаёт.
        peak = DebugSnapshot(
          rates = updatedPeaks,
          totals = current,
          info = state.debugInfo.value,
          lastPan = telemetry.lastPan,
          lastFling = telemetry.lastFling
        )
      }
      previous = current
      previousMillis = millis
    }
  }
  val phases = remember(totals, rates) {
    totals.toPhaseRows(rates)
  }
  val facts = remember(info, totals) {
    info.toFactRows(
      labelOf = levelLabelOf,
      lastPan = telemetry.lastPan,
      lastFling = telemetry.lastFling,
      tickMillis = TICK_MILLIS
    )
  }
  val peakPhases = remember(peak) {
    peak.totals.toPhaseRows(peak.rates)
  }
  val peakFacts = remember(peak) {
    peak.info.toFactRows(
      labelOf = levelLabelOf,
      lastPan = peak.lastPan,
      lastFling = peak.lastFling,
      tickMillis = TICK_MILLIS
    )
  }
  val pagerState = rememberPagerState(pageCount = { 2 })
  HorizontalPager(
    state = pagerState,
    modifier = modifier.onGloballyPositioned { onBoundsChanged(ZONE_KEY, it.boundsInRoot()) }
  ) { page ->
    DebugPage(
      title = if (page == 0) "CANVAS DEBUG" else "CANVAS PEAKS",
      phases = if (page == 0) phases else peakPhases,
      facts = if (page == 0) facts else peakFacts
    )
  }
}

/**
 * Страница панели: заголовок и две колонки под ним.
 *
 * @param title заголовок страницы
 * @param phases строки фаз Compose
 * @param facts строки фактов о полотне
 * @param modifier модификатор страницы
 */
@Composable
private fun DebugPage(
  title: String,
  phases: List<DebugRow>,
  facts: List<DebugRow>,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.background(
      color = PANEL_BACKGROUND,
      shape = RoundedCornerShape(12.dp)
    )
  ) {
    Spacer(Modifier.height(PANEL_PADDING))
    BasicText(
      modifier = Modifier.padding(horizontal = PANEL_PADDING),
      text = title,
      style = TITLE_STYLE.copy(color = LABEL_COLOR)
    )
    Spacer(Modifier.height(8.dp))
    // Две колонки: фазы Compose слева, факты о полотне справа. Так шестнадцать метрик укладываются
    // в девять строк и панель перестаёт закрывать граф, ради которого её открыли.
    val rowHeight = with(LocalDensity.current) { ROW_STYLE.lineHeight.toDp() }
    Row(
      modifier = Modifier
        .heightIn(max = rowHeight * VISIBLE_ROWS)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = PANEL_PADDING)
    ) {
      DebugColumn(
        modifier = Modifier.width(PHASE_COLUMN_WIDTH),
        rows = phases,
        labelWidth = PHASE_LABEL_WIDTH
      )
      Spacer(Modifier.width(12.dp))
      DebugColumn(
        modifier = Modifier.fillMaxWidth(),
        rows = facts,
        labelWidth = FACT_LABEL_WIDTH
      )
    }
    Spacer(Modifier.height(PANEL_PADDING))
  }
}

/**
 * Колонка строк с выровненными подписями.
 *
 * @param rows строки колонки
 * @param labelWidth ширина колонки подписей: выравнивание держится на ней, а не на пробелах
 * @param modifier модификатор колонки
 */
@Composable
private fun DebugColumn(
  rows: List<DebugRow>,
  labelWidth: Dp,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    rows.fastForEach { row ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(if (row.isAnomalous) ANOMALY_BACKGROUND else Color.Transparent)
      ) {
        BasicText(
          modifier = Modifier.width(labelWidth),
          text = row.label,
          style = ROW_STYLE.copy(
            color = if (row.isAnomalous) ANOMALY_CONTENT else LABEL_COLOR
          )
        )
        BasicText(
          text = row.value,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = ROW_STYLE.copy(
            color = if (row.isAnomalous) ANOMALY_CONTENT else VALUE_COLOR
          )
        )
      }
    }
  }
}

// Ключ зоны жеста: важно только то, что он один на панель и не совпадает с чужим. Читается дважды —
// при объявлении зоны и при её снятии.
private val ZONE_KEY = Any()

private const val TICK_MILLIS = 500L
private const val VISIBLE_ROWS = 12

private val PANEL_PADDING = 12.dp
private val PHASE_COLUMN_WIDTH = 148.dp
private val PHASE_LABEL_WIDTH = 68.dp
private val FACT_LABEL_WIDTH = 60.dp
