package ru.sla.clarify.feature.chronology.ui.components.canvas

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sla.clarify.feature.chronology.ui.entity.GraphTelemetry

/**
 * Скорости считаются делением на прошедшее время, поэтому проверяется и вырожденный такт: панель
 * открывают, когда экран уже подвис, и нулевой интервал между снимками там вполне возможен.
 */
class GraphCanvasTelemetryTest {

  @Test
  fun `rate is the difference over the elapsed second`() {
    val rates = ratesOf(
      previous = GraphTelemetry.Empty.copy(canvasCompositions = 10),
      current = GraphTelemetry.Empty.copy(canvasCompositions = 40),
      elapsedMillis = 500L
    )

    assertEquals(60, rates.canvasCompositions, "тридцать за полсекунды — это шестьдесят в секунду")
  }

  @Test
  fun `a zero interval yields no rates instead of dividing by zero`() {
    val rates = ratesOf(
      previous = GraphTelemetry.Empty,
      current = GraphTelemetry.Empty.copy(measurePasses = 5),
      elapsedMillis = 0L
    )

    assertEquals(GraphTelemetry.Empty, rates)
  }

  @Test
  fun `every counter is converted, not just the first`() {
    val current = GraphTelemetry(
      canvasCompositions = 1,
      nodeCompositions = 2,
      overlayCompositions = 3,
      measurePasses = 4,
      placementPasses = 5,
      layerUpdates = 6,
      edgeDraws = 7,
      backdropDraws = 8,
      panEvents = 9
    )

    val rates = ratesOf(GraphTelemetry.Empty, current, elapsedMillis = 1000L)

    assertEquals(current, rates, "за секунду скорость равна приращению")
  }
}
