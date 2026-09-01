package ru.sla.atlas.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sla.atlas.entity.Telemetry

/**
 * Скорости считаются делением на прошедшее время, поэтому проверяется и вырожденный такт: панель
 * открывают, когда экран уже подвис, и нулевой интервал между снимками там вполне возможен.
 */
class GraphCanvasTelemetryTest {

  @Test
  fun `rate is the difference over the elapsed second`() {
    val rates = ratesOf(
      previous = Telemetry.Empty.copy(canvasCompositions = 10),
      current = Telemetry.Empty.copy(canvasCompositions = 40),
      elapsedMillis = 500L
    )

    assertEquals(60, rates.canvasCompositions, "тридцать за полсекунды — это шестьдесят в секунду")
  }

  @Test
  fun `a zero interval yields no rates instead of dividing by zero`() {
    val rates = ratesOf(
      previous = Telemetry.Empty,
      current = Telemetry.Empty.copy(measurePasses = 5),
      elapsedMillis = 0L
    )

    assertEquals(Telemetry.Empty, rates)
  }

  @Test
  fun `every counter is converted, not just the first`() {
    val current = Telemetry(
      canvasCompositions = 1,
      nodeCompositions = 2,
      overlayCompositions = 3,
      measurePasses = 4,
      placementPasses = 5,
      layerUpdates = 6,
      edgeDraws = 7,
      backdropDraws = 8,
      panEvents = 9,
      zoomEvents = 10,
      flingSteps = 11,
      flingStalls = 12
    )

    val rates = ratesOf(Telemetry.Empty, current, elapsedMillis = 1000L)

    assertEquals(current, rates, "за секунду скорость равна приращению")
  }

  @Test
  fun `every counter is peaked, not just the first`() {
    val loud = Telemetry(
      canvasCompositions = 1,
      nodeCompositions = 2,
      overlayCompositions = 3,
      measurePasses = 4,
      placementPasses = 5,
      layerUpdates = 6,
      edgeDraws = 7,
      backdropDraws = 8,
      panEvents = 9,
      zoomEvents = 10,
      flingSteps = 11,
      flingStalls = 12
    )

    val peaks = peaksOf(Telemetry.Empty, loud)

    assertEquals(loud, peaks, "счётчик, забытый в peaksOf, дал бы вечный ноль в строке пика")
  }

  @Test
  fun `a peak is held per phase, not per tick`() {
    val loudMeasure = Telemetry.Empty.copy(measurePasses = 87, layerUpdates = 45)
    val loudLayer = Telemetry.Empty.copy(measurePasses = 0, layerUpdates = 120)

    val peaks = peaksOf(peaksOf(Telemetry.Empty, loudMeasure), loudLayer)

    assertEquals(87, peaks.measurePasses, "всплеск держится и после спокойного такта")
    assertEquals(120, peaks.layerUpdates, "у каждой фазы свой пик: они не совпадают по времени")
  }

  @Test
  fun `a quiet tick cannot lower a peak`() {
    val peaks = peaksOf(Telemetry.Empty.copy(measurePasses = 87), Telemetry.Empty)

    assertEquals(87, peaks.measurePasses, "пик существует ровно затем, чтобы пережить свой такт")
  }
}
