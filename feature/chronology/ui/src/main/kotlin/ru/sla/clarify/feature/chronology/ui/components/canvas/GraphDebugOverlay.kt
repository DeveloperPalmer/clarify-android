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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.delay
import ru.sla.clarify.feature.chronology.ui.entity.GraphDebugInfo
import ru.sla.clarify.feature.chronology.ui.entity.GraphDebugRow
import ru.sla.clarify.feature.chronology.ui.entity.GraphTelemetry
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.HSpacer
import ru.sla.clarify.uikit.theme.VSpacer
import kotlin.math.roundToInt

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

  val phases = remember(totals, rates) { phaseRowsOf(totals, rates) }
  val facts = remember(info, totals) { factRowsOf(info, telemetry.lastPan) }

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

/**
 * Строки по фазам Compose.
 *
 * Пределы разные, потому что фазы разные по назначению: измерение, размещение и рекомпозиция
 * полотна в покое обязаны стоять, а слой камеры, связи, фон, панель и жест идут покадрово.
 *
 * @param totals накопленные значения
 * @param rates значения в секунду
 * @return строки левой колонки
 */
private fun phaseRowsOf(totals: GraphTelemetry, rates: GraphTelemetry): List<GraphDebugRow> {
  return listOf(
    phaseRow("canvas", totals.canvasCompositions, rates.canvasCompositions, IDLE_LIMIT),
    phaseRow("nodes", totals.nodeCompositions, rates.nodeCompositions, NODE_LIMIT),
    phaseRow("overlay", totals.overlayCompositions, rates.overlayCompositions, FRAME_LIMIT),
    phaseRow("measure", totals.measurePasses, rates.measurePasses, IDLE_LIMIT),
    phaseRow("placement", totals.placementPasses, rates.placementPasses, IDLE_LIMIT),
    phaseRow("layer", totals.layerUpdates, rates.layerUpdates, FRAME_LIMIT),
    phaseRow("edges", totals.edgeDraws, rates.edgeDraws, FRAME_LIMIT),
    phaseRow("backdrop", totals.backdropDraws, rates.backdropDraws, FRAME_LIMIT),
    phaseRow("pan", totals.panEvents, rates.panEvents, FRAME_LIMIT)
  )
}

/**
 * Строка фазы: скорость впереди, накопленное значение следом.
 *
 * Скорость стоит первой не для красоты: зависание видно по тому, что фаза тикает при снятом пальце,
 * а накопленное число за смену только растёт и само по себе ни о чём не говорит.
 *
 * @param label подпись фазы
 * @param total накопленное значение
 * @param rate значений в секунду
 * @param limit предел, выше которого значение аномально
 * @return строка панели
 */
private fun phaseRow(label: String, total: Int, rate: Int, limit: Int): GraphDebugRow {
  return GraphDebugRow(
    label = label,
    value = "$rate/s · $total",
    isAnomalous = rate > limit
  )
}

/**
 * Строки с фактами о полотне.
 *
 * Числа округляются до целых пикселей: доли пикселя в диагностике ничего не решают, а строку вроде
 * `Rect.fromLTRB(252.0, 100.0, 2629.0, 447.0)` в колонку не уложить.
 *
 * @param info снимок камеры и раскладки
 * @param lastPan последнее приращение жеста
 * @return строки правой колонки
 */
private fun factRowsOf(info: GraphDebugInfo, lastPan: Offset): List<GraphDebugRow> {
  return listOf(
    GraphDebugRow("viewport", "${info.viewportWidth} × ${info.viewportHeight}"),
    GraphDebugRow(
      label = "camera",
      value = lineOf(info.camera) + if (info.isCameraMoved) "" else " · rest",
      isAnomalous = !info.camera.isValid()
    ),
    GraphDebugRow("bounds", lineOf(info.contentBounds)),
    GraphDebugRow(
      label = "span x",
      value = "${info.centreSpanX.start.roundToInt()} … ${info.centreSpanX.endInclusive.roundToInt()}"
    ),
    GraphDebugRow(
      label = "nodes",
      value = "${info.nodeCount} · edges ${info.edgeCount}",
      isAnomalous = info.nodeCount > 0 && info.contentBounds.isEmpty
    ),
    GraphDebugRow("last pan", lineOf(lastPan)),
    GraphDebugRow("tick", "$TICK_MILLIS ms")
  )
}

private fun lineOf(offset: Offset): String {
  if (!offset.isValid()) {
    return offset.toString()
  }
  return "${offset.x.roundToInt()}, ${offset.y.roundToInt()}"
}

private fun lineOf(rect: Rect): String {
  return "${rect.left.roundToInt()}, ${rect.top.roundToInt()} … " +
    "${rect.right.roundToInt()}, ${rect.bottom.roundToInt()}"
}

private const val TICK_MILLIS = 500L
private const val VISIBLE_ROWS = 10

// Фазы, которые в покое обязаны стоять. Несколько проходов на одно действие пользователя — норма,
// устойчивый поток — уже нет: именно так выглядит цикл «запись из измерения, чтение в композиции».
private const val IDLE_LIMIT = 8

// Рекомпозиция содержимого считается суммой по всем узлам, поэтому один полный проход небольшого
// графа сам по себе даёт десятки.
private const val NODE_LIMIT = 60

// Фазы, которые по построению идут покадрово: слой камеры, связи, фон, панель и сам жест. Выше
// частоты обновления экрана это означает больше одного прохода на кадр.
private const val FRAME_LIMIT = 140

private val PANEL_PADDING = 12.dp
private val PHASE_COLUMN_WIDTH = 148.dp
private val PHASE_LABEL_WIDTH = 68.dp
private val FACT_LABEL_WIDTH = 60.dp
