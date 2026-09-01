package ru.sla.atlas.lod

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.atlas.entity.ScaleBand

/**
 * Уровни вынесены в чистые функции ровно затем, чтобы их можно было проверить без Compose и без
 * устройства: полоса, порог, посадка и вписывание — обычная арифметика.
 *
 * Тест сторожит три дефекта, каждый из которых виден только глазом. Первый — скачок охвата на стыке:
 * наивная схема «порог на сыром масштабе, масштаб сохраняется» показывает после перехода в разы
 * больше истории, чем показывала до. Второй — мигание уровня на границе: посадка ровно на порог, с
 * которого только что ушли, возвращает обратно от любого дрожания пальца. Третий — переход туда,
 * где уровня нет.
 *
 * Полосы задаются числами прямо здесь: откуда они у вызывающего взялись, арифметике безразлично, а
 * взяв их у схемы, тест проверял бы заодно и чужую таблицу.
 */
class DetailLevelsTest {

  @Test
  fun `a pinch below the floor drops to the coarser level`() {
    val band = MockLevels.bandOf(MockLevel.Fine, fitScale = 0.05f)

    assertEquals(
      MockLevel.Coarse,
      levelSwitchOf(MockLevel.Fine, requestedScale = 0.49f, band = band, scheme = MockLevels),
      "ниже полосы уровня масштаб означает не упор, а переход"
    )
    assertNull(
      levelSwitchOf(MockLevel.Fine, requestedScale = 0.51f, band = band, scheme = MockLevels),
      "внутри полосы уровень не трогается"
    )
  }

  @Test
  fun `a pinch above the ceiling climbs to the finer level`() {
    val band = MockLevels.bandOf(MockLevel.Coarse, fitScale = 0.3f)

    assertEquals(
      MockLevel.Fine,
      levelSwitchOf(MockLevel.Coarse, requestedScale = 2.01f, band = band, scheme = MockLevels),
      "выше потолка обзорного уровня живёт подробный"
    )
    assertNull(levelSwitchOf(MockLevel.Coarse, requestedScale = 1.99f, band = band, scheme = MockLevels))
    assertNull(
      levelSwitchOf(MockLevel.Coarse, requestedScale = 0.1f, band = band, scheme = MockLevels),
      "ниже самого обзорного уровня нет: там масштаб просто упирается"
    )
  }

  @Test
  fun `a canvas without detail levels never switches`() {
    val band = MockSingleLevel.bandOf(Unit, fitScale = 0.3f)

    assertNull(
      levelSwitchOf(Unit, requestedScale = 0.1f, band = band, scheme = MockSingleLevel),
      "уровень один, соседей нет — и никакой ветки в движке под этот случай не требуется"
    )
    assertNull(levelSwitchOf(Unit, requestedScale = 9f, band = band, scheme = MockSingleLevel))
  }

  @Test
  fun `landing keeps the visible span within a per cent`() {
    val landing = levelLandingOf(
      scaleBefore = 0.3f,
      spanBefore = 7540f,
      spanAfter = 1210f,
      band = ScaleBand(min = 0.305f, max = 2.5f),
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
    val band = ScaleBand(min = 0.305f, max = 2.5f)

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
    val coarse = ScaleBand(min = 0.305f, max = 2.5f)

    val landing = levelLandingOf(
      scaleBefore = 0.399f,
      spanBefore = 7540f,
      spanAfter = 1210f,
      band = coarse,
      margin = 0.08f
    )

    assertNull(
      levelSwitchOf(MockLevel.Coarse, landing * 1.05f, band = coarse, scheme = MockLevels),
      "пять процентов щипка — это дрожание пальца, и уровень от него мигать не должен"
    )
    assertEquals(
      MockLevel.Fine,
      levelSwitchOf(MockLevel.Coarse, landing * 1.1f, band = coarse, scheme = MockLevels),
      "а осознанный щипок через запас возвращает обратно"
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
      "вписать по одной оси, вылезая за экран по другой, значит не вписать вовсе"
    )
  }

  @Test
  fun `an empty graph has no fit scale and no switch`() {
    assertEquals(
      1f,
      fitScaleOf(bounds = Rect.Zero, viewport = IntSize(width = 400, height = 400)),
      "у вырожденного содержимого вписывать нечего, и деления на ноль здесь быть не должно"
    )
    assertNull(
      levelSwitchOf(
        level = MockLevel.Fine,
        requestedScale = Float.NaN,
        band = ScaleBand(min = 0.5f, max = 2f),
        scheme = MockLevels
      ),
      "вырожденное событие пинча отдаёт NaN, и сравнение с ним молча ложно в обе стороны"
    )
  }

  @Test
  fun `the counter scale keeps the outgoing view its screen size`() {
    val counter = counterScaleOf(from = 0.4f, to = 2.3f)

    assertEquals(
      200f * 0.4f,
      200f * 2.3f * counter,
      1e-3f,
      "узел 200 dp обязан остаться теми же 80 dp экрана, какими был в момент перехода"
    )
    assertEquals(1f, counterScaleOf(from = 0.4f, to = 0f), "вырожденный масштаб не должен делить")
  }
}
