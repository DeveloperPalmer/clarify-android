package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.delay
import ru.sla.clarify.feature.chronology.ui.entity.GraphDebugRow
import ru.sla.clarify.feature.chronology.ui.entity.GraphTelemetry
import ru.sla.clarify.feature.chronology.ui.mapper.toFactRows
import ru.sla.clarify.feature.chronology.ui.mapper.toPhaseRows
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.HSpacer
import ru.sla.clarify.uikit.theme.VSpacer

/**
 * Отладочная панель полотна, под тоглом `chronologyDebugOverlay`.
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
 * @param modifier модификатор панели
 */
@Composable
internal fun GraphDebugOverlay(
  state: GraphCanvasState,
  modifier: Modifier = Modifier
) {
  val telemetry = state.telemetry
  SideEffect { telemetry.onOverlayComposition() }

  val info by state.debugInfo
  var totals by remember { mutableStateOf(GraphTelemetry.Empty) }
  var rates by remember { mutableStateOf(GraphTelemetry.Empty) }

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
      rates = ratesOf(previous, current, millis - previousMillis)
      previous = current
      previousMillis = millis
    }
  }
  val phases = remember(totals, rates) {
    totals.toPhaseRows(rates)
  }
  val facts = remember(info, totals) {
    info.toFactRows(
      lastPan = telemetry.lastPan,
      tickMillis = TICK_MILLIS
    )
  }
  Column(
    modifier = modifier.surface(
      backgroundColor = AppTheme.colors.cardPrimary,
      shape = AppTheme.shapes.round12,
      elevation = AppTheme.elevation.large
    )
  ) {
    VSpacer(PANEL_PADDING)
    BasicText(
      modifier = Modifier.padding(horizontal = PANEL_PADDING),
      text = "GRAPH DEBUG",
      style = AppTheme.typography.label3Bold.copy(color = AppTheme.colors.contentTertiary)
    )
    VSpacer(8.dp)
    // Две колонки: фазы Compose слева, факты о полотне справа. Так шестнадцать метрик укладываются
    // в девять строк и панель перестаёт закрывать граф, ради которого её открыли.
    val rowHeight = with(LocalDensity.current) { AppTheme.typography.caption.lineHeight.toDp() }
    Row(
      modifier = Modifier
        .heightIn(max = rowHeight * VISIBLE_ROWS)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = PANEL_PADDING)
    ) {
      GraphDebugColumn(
        modifier = Modifier.width(PHASE_COLUMN_WIDTH),
        rows = phases,
        labelWidth = PHASE_LABEL_WIDTH
      )
      HSpacer(12.dp)
      GraphDebugColumn(
        modifier = Modifier.fillMaxWidth(),
        rows = facts,
        labelWidth = FACT_LABEL_WIDTH
      )
    }
    VSpacer(PANEL_PADDING)
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
private fun GraphDebugColumn(
  rows: List<GraphDebugRow>,
  labelWidth: Dp,
  modifier: Modifier = Modifier
) {
  val anomalyColor = AppTheme.colors.errorPrimary
  val labelColor = AppTheme.colors.contentTertiary
  val valueColor = AppTheme.colors.contentPrimary
  val anomalyContentColor = AppTheme.colors.contentAccentSecondary
  Column(modifier = modifier) {
    rows.fastForEach { row ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(if (row.isAnomalous) anomalyColor else Color.Transparent)
      ) {
        BasicText(
          modifier = Modifier.width(labelWidth),
          text = row.label,
          style = AppTheme.typography.caption.copy(
            color = if (row.isAnomalous) anomalyContentColor else labelColor
          )
        )
        BasicText(
          text = row.value,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = AppTheme.typography.caption.copy(
            color = if (row.isAnomalous) anomalyContentColor else valueColor
          )
        )
      }
    }
  }
}

private const val TICK_MILLIS = 500L
private const val VISIBLE_ROWS = 10

private val PANEL_PADDING = 12.dp
private val PHASE_COLUMN_WIDTH = 148.dp
private val PHASE_LABEL_WIDTH = 68.dp
private val FACT_LABEL_WIDTH = 60.dp
