package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel

/**
 * Уровни вынесены в чистые функции ровно затем, чтобы их можно было проверить без Compose и без
 * устройства: полоса, порог, посадка и вписывание — обычная арифметика.
 *
 * Тест сторожит три дефекта, каждый из которых виден только глазом. Первый — скачок охвата на стыке:
 * наивная схема «порог на сыром масштабе, масштаб сохраняется» показывает после перехода вшестеро
 * больше истории, чем показывала до. Второй — мигание уровня на границе: посадка ровно на порог, с
 * которого только что ушли, возвращает обратно от любого дрожания пальца. Третий — обзор, ушедший
 * ниже читаемости глифа.
 *
 * Числа взяты с демо-набора: охват уровня эпизодов 7540 dp против 1210 dp на обзоре — отношение
 * 6.23 при отношении пределов масштаба 6.25.
 */
class GraphLodTest {

  @Test
  fun `a pinch below the floor drops into the overview`() {
    val band = graphLevelBandOf(GraphLevel.Episodes, fitScale = 0.05f)

    assertEquals(
      GraphLevel.Overview,
      graphLevelSwitchOf(GraphLevel.Episodes, requestedScale = 0.39f, band = band),
      "ниже полосы уровня масштаб означает не упор, а переход"
    )
    assertNull(
      graphLevelSwitchOf(GraphLevel.Episodes, requestedScale = 0.41f, band = band),
      "внутри полосы уровень не трогается"
    )
  }

  @Test
  fun `a pinch above the overview ceiling climbs back to episodes`() {
    val band = graphLevelBandOf(GraphLevel.Overview, fitScale = 0.305f)

    assertEquals(
      GraphLevel.Episodes,
      graphLevelSwitchOf(GraphLevel.Overview, requestedScale = 2.51f, band = band),
      "выше потолка обзора живут эпизоды"
    )
    assertNull(graphLevelSwitchOf(GraphLevel.Overview, requestedScale = 2.49f, band = band))
    assertNull(
      graphLevelSwitchOf(GraphLevel.Overview, requestedScale = 0.1f, band = band),
      "ниже обзора уровня нет: там масштаб просто упирается"
    )
  }

  @Test
  fun `landing keeps the visible span within a per cent`() {
    val band = graphLevelBandOf(GraphLevel.Overview, fitScale = 0.305f)

    val landing = levelLandingOf(
      scaleBefore = 0.3f,
      spanBefore = 7540f,
      spanAfter = 1210f,
      band = band,
      margin = 0.08f
    )

    assertEquals(
      0.3f * 7540f,
      landing * 1210f,
      0.01f * 0.3f * 7540f,
      "охват — это `масштаб · охват уровня`, и переход обязан оставить его тем же"
    )
  }

  @Test
  fun `landing stays a margin clear of the threshold it just crossed`() {
    val band = graphLevelBandOf(GraphLevel.Overview, fitScale = 0.305f)

    val landing = levelLandingOf(
      scaleBefore = 0.4f,
      spanBefore = 7540f,
      spanAfter = 1210f,
      band = band,
      margin = 0.08f
    )

    assertEquals(
      2.3f,
      landing,
      1e-4f,
      "сохранение охвата просит 2.49×, но посадка на самом потолке и есть мигание на границе"
    )
    assertTrue(landing < band.max, "запас обязан остаться внутри полосы")
  }

  @Test
  fun `a jitter at the boundary cannot flip the level twice`() {
    val episodes = graphLevelBandOf(GraphLevel.Episodes, fitScale = 0.052f)
    val overview = graphLevelBandOf(GraphLevel.Overview, fitScale = 0.305f)

    assertEquals(
      GraphLevel.Overview,
      graphLevelSwitchOf(GraphLevel.Episodes, requestedScale = 0.399f, band = episodes)
    )
    val landing = levelLandingOf(
      scaleBefore = 0.399f,
      spanBefore = 7540f,
      spanAfter = 1210f,
      band = overview,
      margin = 0.08f
    )

    assertNull(
      graphLevelSwitchOf(GraphLevel.Overview, requestedScale = landing * 1.05f, band = overview),
      "пять процентов щипка — это дрожание пальца, и уровень от него мигать не должен"
    )
    assertEquals(
      GraphLevel.Episodes,
      graphLevelSwitchOf(GraphLevel.Overview, requestedScale = landing * 1.1f, band = overview),
      "а осознанный щипок через запас возвращает обратно"
    )
  }

  @Test
  fun `the overview floor sinks to fit-all but never below the glyph limit`() {
    assertEquals(
      0.305f,
      graphLevelBandOf(GraphLevel.Overview, fitScale = 0.305f).min,
      1e-4f,
      "на демо-наборе упор обзора приходится ровно на вписанный граф"
    )
    assertEquals(
      0.2f,
      graphLevelBandOf(GraphLevel.Overview, fitScale = 0.077f).min,
      1e-4f,
      "переписка в двести эпизодов вписалась бы на 0.077×, где глиф вырождается в полтора пикселя"
    )
    assertEquals(
      0.4f,
      graphLevelBandOf(GraphLevel.Overview, fitScale = 0.9f).min,
      1e-4f,
      "короткая история вписывается и так: ниже общего предела обзор не опускается"
    )
  }

  @Test
  fun `fit scale takes the tighter of the two axes`() {
    val fit = fitScaleOf(
      bounds = Rect(left = 0f, top = 0f, right = 1000f, bottom = 500f),
      viewport = IntSize(width = 400, height = 400)
    )

    assertEquals(
      0.4f,
      fit,
      1e-4f,
      "вписать по времени, вылезая за экран по дорожкам, значит не вписать вовсе"
    )
  }

  @Test
  fun `an empty graph has no fit scale and no switch`() {
    val band = graphLevelBandOf(GraphLevel.Episodes, fitScale = 1f)

    assertEquals(
      1f,
      fitScaleOf(bounds = Rect.Zero, viewport = IntSize(width = 400, height = 400)),
      "у вырожденного содержимого вписывать нечего, и деления на ноль здесь быть не должно"
    )
    assertNull(
      graphLevelSwitchOf(GraphLevel.Episodes, requestedScale = Float.NaN, band = band),
      "вырожденное событие пинча отдаёт NaN, и сравнение с ним молча ложно в обе стороны"
    )
  }

  @Test
  fun `the counter scale keeps the outgoing plate its screen size`() {
    val counter = counterScaleOf(from = 0.4f, to = 2.3f)

    assertEquals(
      200f * 0.4f,
      200f * 2.3f * counter,
      1e-3f,
      "плашка 200 dp обязана остаться теми же 80 dp экрана, какими была в момент перехода"
    )
    assertEquals(1f, counterScaleOf(from = 0.4f, to = 0f), "вырожденный масштаб не должен делить")
  }
}
