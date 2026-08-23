package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import ru.sla.clarify.feature.chronology.ui.entity.GraphTelemetry

/**
 * Счётчики проходов Compose по полотну.
 *
 * Все поля — **обычные**, не снапшот-состояние, и это главное свойство этого класса. Инструмент не
 * должен вызывать то, что измеряет: снапшот-запись из фазы измерения, которую кто-то читает в
 * композиции, сама и есть бесконечный цикл, который тут ищут. Панель забирает значения по таймеру,
 * а не по подписке.
 *
 * Счёт идёт всегда, независимо от тогла: инкремент целого стоит ничего, зато числа правдивы с
 * момента запуска, а не с момента включения панели.
 */
@Stable
class GraphCanvasTelemetry {

  private var canvasCompositions = 0
  private var nodeCompositions = 0
  private var overlayCompositions = 0
  private var measurePasses = 0
  private var placementPasses = 0
  private var layerUpdates = 0
  private var edgeDraws = 0
  private var backdropDraws = 0
  private var panEvents = 0
  private var flingSteps = 0
  private var flingStalls = 0

  /** Последнее приращение жеста. Затухание сюда не пишет: иначе строка вырождается в «0, 0». */
  var lastPan: Offset = Offset.Zero
    private set

  internal fun onCanvasComposition() {
    canvasCompositions++
  }

  internal fun onNodeComposition() {
    nodeCompositions++
  }

  internal fun onOverlayComposition() {
    overlayCompositions++
  }

  internal fun onMeasure() {
    measurePasses++
  }

  internal fun onPlacement() {
    placementPasses++
  }

  internal fun onLayerUpdate() {
    layerUpdates++
  }

  internal fun onEdgeDraw() {
    edgeDraws++
  }

  internal fun onBackdropDraw() {
    backdropDraws++
  }

  internal fun onPan(delta: Offset) {
    panEvents++
    lastPan = delta
  }

  /**
   * Кадр затухания.
   *
   * Кадры жеста сюда не попадают, и это не мелочь учёта: без разделения панель не отличит «идёт
   * инерция» от «инерция молотит в стенку», а именно это она и заведена показывать. Отпечаток
   * зависшего затухания — `fling` тикает при стоящем `layer`.
   *
   * Отказ считается только под затуханием: палец, прижатый к краю, отдаёт полсотни отказов в
   * секунду совершенно законно, и красная строка на нём была бы ложной тревогой. У затухания же
   * отказной кадр может быть только один на бросок — следующим действием идёт остановка.
   *
   * @param delta запрошенное приращение
   * @param consumed то, что камера из него взяла
   */
  internal fun onFlingStep(delta: Offset, consumed: Offset) {
    flingSteps++
    if (consumed == Offset.Zero && delta != Offset.Zero) {
      flingStalls++
    }
  }

  /**
   * Снимок счётчиков.
   *
   * @return накопленные значения на момент вызова
   */
  fun read(): GraphTelemetry {
    return GraphTelemetry(
      canvasCompositions = canvasCompositions,
      nodeCompositions = nodeCompositions,
      overlayCompositions = overlayCompositions,
      measurePasses = measurePasses,
      placementPasses = placementPasses,
      layerUpdates = layerUpdates,
      edgeDraws = edgeDraws,
      backdropDraws = backdropDraws,
      panEvents = panEvents,
      flingSteps = flingSteps,
      flingStalls = flingStalls
    )
  }
}

/**
 * Скорости изменения счётчиков.
 *
 * @param previous предыдущий снимок
 * @param current текущий снимок
 * @param elapsedMillis прошедшее между снимками время
 * @return те же поля, но в единицах в секунду
 */
internal fun ratesOf(
  previous: GraphTelemetry,
  current: GraphTelemetry,
  elapsedMillis: Long
): GraphTelemetry {
  if (elapsedMillis <= 0L) {
    return GraphTelemetry.Empty
  }
  fun rate(from: Int, to: Int): Int {
    return ((to - from) * MILLIS_IN_SECOND / elapsedMillis).toInt()
  }
  return GraphTelemetry(
    canvasCompositions = rate(previous.canvasCompositions, current.canvasCompositions),
    nodeCompositions = rate(previous.nodeCompositions, current.nodeCompositions),
    overlayCompositions = rate(previous.overlayCompositions, current.overlayCompositions),
    measurePasses = rate(previous.measurePasses, current.measurePasses),
    placementPasses = rate(previous.placementPasses, current.placementPasses),
    layerUpdates = rate(previous.layerUpdates, current.layerUpdates),
    edgeDraws = rate(previous.edgeDraws, current.edgeDraws),
    backdropDraws = rate(previous.backdropDraws, current.backdropDraws),
    panEvents = rate(previous.panEvents, current.panEvents),
    flingSteps = rate(previous.flingSteps, current.flingSteps),
    flingStalls = rate(previous.flingStalls, current.flingStalls)
  )
}

/**
 * Поэлементный максимум двух снимков.
 *
 * Аномалия живёт один такт панели и исчезает: к тому моменту, как её заметили глазом, строка уже
 * снова зелёная. Пик её удерживает — по каждой фазе отдельно, потому что всплески у них не
 * совпадают по времени.
 *
 * @param peaks накопленные максимумы
 * @param rates скорости последнего такта
 * @return максимум по каждой фазе
 */
internal fun peaksOf(peaks: GraphTelemetry, rates: GraphTelemetry): GraphTelemetry {
  return GraphTelemetry(
    canvasCompositions = maxOf(peaks.canvasCompositions, rates.canvasCompositions),
    nodeCompositions = maxOf(peaks.nodeCompositions, rates.nodeCompositions),
    overlayCompositions = maxOf(peaks.overlayCompositions, rates.overlayCompositions),
    measurePasses = maxOf(peaks.measurePasses, rates.measurePasses),
    placementPasses = maxOf(peaks.placementPasses, rates.placementPasses),
    layerUpdates = maxOf(peaks.layerUpdates, rates.layerUpdates),
    edgeDraws = maxOf(peaks.edgeDraws, rates.edgeDraws),
    backdropDraws = maxOf(peaks.backdropDraws, rates.backdropDraws),
    panEvents = maxOf(peaks.panEvents, rates.panEvents),
    flingSteps = maxOf(peaks.flingSteps, rates.flingSteps),
    flingStalls = maxOf(peaks.flingStalls, rates.flingStalls)
  )
}

private const val MILLIS_IN_SECOND = 1000L
