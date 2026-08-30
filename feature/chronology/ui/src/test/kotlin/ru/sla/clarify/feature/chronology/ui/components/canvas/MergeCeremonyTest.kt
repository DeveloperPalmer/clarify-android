package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.geometry.Offset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.atlas.entity.Branch
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdge
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdgeRole
import ru.sla.clarify.feature.chronology.ui.entity.MergeCeremonyFrame

/**
 * Сторожит раскадровку церемонии слияния — семь кадров §12 на одной шкале.
 *
 * Проверять здесь можно **всё**, и это главное свойство решения: церемония нарисована долями, а не
 * пикселями, поэтому `PathMeasure` и `Path` — стабы `android.graphics`, не покрываемые тестом, —
 * в арифметику не попали вовсе.
 *
 * Границы кадров проверяются с двух сторон. Кадр, у которого проверено только начало, молча
 * продолжает играть за своим концом, а увидеть это можно лишь на устройстве.
 */
class MergeCeremonyTest {

  @Test
  fun `the dash runs up from one to three and keeps that pace`() {
    assertEquals(1f, frameAt(0f).dashSpeed, "до церемонии пунктир бежит своим обычным ходом")
    assertEquals(2f, frameAt(75f).dashSpeed, "разгон линеен: половина кадра 1 — половина прибавки")
    assertEquals(3f, frameAt(150f).dashSpeed, "к концу кадра 1 бег втрое быстрее")
    assertEquals(3f, frameAt(400f).dashSpeed, "и держится весь такт кадра 2, а не откатывается")
  }

  @Test
  fun `the line turns gold on the first frame and hands the colour back on the last`() {
    assertEquals(0f, frameAt(0f).gold, "на нулевой отметке линия ещё своего цвета")
    assertEquals(1f, frameAt(150f).gold, "на границе кадра 1 — полностью золотая")
    assertEquals(1f, frameAt(1100f).gold, "и остаётся такой до выдоха")
    assertEquals(0f, frameAt(1200f).gold, "а выдохом цвет возвращается: золото — событие, не ветка")
  }

  @Test
  fun `the ring arrives on the third frame and not before`() {
    assertEquals(0f, frameAt(399f).ring, "до 400 мс места на магистрали нет вовсе")
    assertEquals(1f, frameAt(550f).ring, "к концу кадра 3 кольцо проявлено целиком")
  }

  @Test
  fun `the ring pulses only while it waits for the line`() {
    assertEquals(0f, frameAt(399f).ringPulse, "кольца ещё нет — пульсировать нечему")
    assertTrue(frameAt(475f).ringPulse > 0f, "посреди ожидания пульсация идёт")
    // 725, а не 700: фаза циклическая, и на стыке циклов она законно равна нулю. Проба, взятая
    // ровно на границе, проверяла бы не «пульсация идёт», а «сейчас начало цикла».
    assertTrue(frameAt(725f).ringPulse > 0f, "и не бросает кольцо, пока линия ещё в пути")
    assertEquals(0f, frameAt(850f).ringPulse, "с ударом ожидание кончается")
  }

  @Test
  fun `the return route is not drawn before its frame and is whole after it`() {
    assertEquals(0f, frameAt(550f).reach, "до 550 мс возврат не начат")
    assertEquals(1f, frameAt(850f).reach, "после 850 мс нарисован целиком")
    assertTrue(
      frameAt(700f).reach > 0.5f,
      "и затухает без разгона: к середине кадра пройдено больше половины пути"
    )
  }

  @Test
  fun `the impact holds the icon at six tenths at its start and at full size at its end`() {
    assertEquals(0f, frameAt(850f).impact, "удар начинается пустым кольцом")
    assertEquals(1f, frameAt(950f).impact, "и кончается залитой точкой с иконкой в полный размер")
  }

  @Test
  fun `the wave spreads from nothing and is spent by the exhale`() {
    assertEquals(0f, frameAt(950f).wave, "волна расходится от точки слияния, а не от края")
    assertEquals(1f, frameAt(1100f).wave, "и к 1100 мс разошлась полностью")
  }

  @Test
  fun `the exhale runs the whole way and only on the last frame`() {
    assertEquals(0f, frameAt(1100f).exhale, "до кадра 7 линия в полную силу")
    assertEquals(1f, frameAt(1200f).exhale, "а выдох доводит её до покоя слитой ветки целиком")
  }

  @Test
  fun `the frame past the end is the rest of a merged branch`() {
    val rest = frameAt(MERGE_CEREMONY_MILLIS)

    assertEquals(0f, rest.gold, "цвет — идентичность ветки")
    assertEquals(1f, rest.exhale, "линия приглушена")
    assertEquals(1f, rest.reach, "возврат нарисован")
    assertEquals(1f, rest.impact, "точка слияния залита")
    assertEquals(0f, rest.ringPulse, "и ничто больше не ждёт")
  }

  @Test
  fun `time outside the scale is pinned to its ends`() {
    assertEquals(frameAt(0f), frameAt(-500f), "отрицательное время — это ещё покой до церемонии")
    assertEquals(
      frameAt(MERGE_CEREMONY_MILLIS),
      frameAt(MERGE_CEREMONY_MILLIS + 500f),
      "а время за концом шкалы — уже покой после неё, а не доля больше единицы"
    )
  }
}

/**
 * Сторожит предикат бегущего пунктира — второй его источник.
 *
 * Пока источник был один, статус ветки, переигрывание церемонии по чипу **слитой** ветки оставляло
 * кадр 1 без пунктира, который он ускоряет. Отказ был молчаливым: ни падения, ни красного теста.
 */
class DashRunningTest {

  @Test
  fun `a graph with a branch ready to merge runs the dash`() {
    assertTrue(
      isDashRunning(listOf(edge(Branch.Status.Ready)), ceremonyPlaying = false),
      "бегущий пунктир §7 принадлежит готовой ветке, и она здесь есть"
    )
  }

  @Test
  fun `a graph with nothing ready and no ceremony leaves the dash alone`() {
    assertFalse(
      isDashRunning(
        listOf(edge(Branch.Status.Merged), edge(Branch.Status.Alive)),
        ceremonyPlaying = false
      ),
      "бесконечная анимация просит кадр, пока жива: без повода её быть не должно вовсе"
    )
  }

  @Test
  fun `a ceremony runs the dash even when no branch is ready`() {
    assertTrue(
      isDashRunning(listOf(edge(Branch.Status.Merged)), ceremonyPlaying = true),
      "переигрывают по чипу слитой ветки, и разгонять кадру 1 было бы нечего"
    )
  }
}

/**
 * Сторожит две развилки, на которых церемония решает судьбу ребра.
 *
 * Обе — про стыки: по какому признаку ребро признаётся своим и как прозрачность конца церемонии
 * сходится с прозрачностью покоя. Разойдись второе, линия дёрнулась бы скачком на последнем кадре.
 */
class CeremonyEdgeTest {

  @Test
  fun `an edge of the merging branch is the ceremony's own`() {
    assertTrue(
      isCeremonyEdge(edge(Branch.Status.Merged), Branch.Id("a")),
      "ребро ветки «a» принадлежит церемонии ветки «a»"
    )
  }

  @Test
  fun `an edge of another branch is left alone even under the same shade`() {
    assertFalse(
      isCeremonyEdge(edge(Branch.Status.Alive), Branch.Id("b")),
      "оттенок у веток может совпасть, идентификатор — нет"
    )
  }

  @Test
  fun `no ceremony claims no edge`() {
    assertFalse(
      isCeremonyEdge(edge(Branch.Status.Alive), ceremonyBranch = null),
      "пока церемония не играет, своих рёбер у неё нет"
    )
  }

  @Test
  fun `the ceremony hands the line over at exactly the alpha of rest`() {
    assertEquals(
      1f,
      ceremonyEdgeAlphaOf(frameAt(1100f)),
      "до выдоха линия идёт в полную силу, иначе кадру 7 не из чего гаснуть"
    )
    assertEquals(
      MERGED_EDGE_ALPHA,
      ceremonyEdgeAlphaOf(frameAt(MERGE_CEREMONY_MILLIS)),
      "а кончает ровно там, где рисует покой: иначе конец церемонии дёрнул бы линию"
    )
  }
}

private fun edge(status: Branch.Status): GraphEdge {
  return GraphEdge(
    points = listOf(Offset.Zero, Offset(100f, 0f)),
    hops = emptyList(),
    branchId = Branch.Id("a"),
    color = mockLaneColor(1),
    role = GraphEdgeRole.Branch,
    status = status
  )
}

/** Та же кривая, что `AppMotion.decelerate`: движение начинается на полной скорости и тормозит. */
private val DECELERATE: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)

private fun frameAt(elapsedMillis: Float): MergeCeremonyFrame {
  return mergeCeremonyFrameOf(elapsedMillis = elapsedMillis, decelerate = DECELERATE)
}
