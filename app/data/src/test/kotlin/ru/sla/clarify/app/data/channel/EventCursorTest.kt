package ru.sla.clarify.app.data.channel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Сторож курсора. Первый запуск здесь — не край, а самое частое состояние: на новом устройстве
 * курсор равен нулю, и он обязан идти той же дорогой, что и просроченный.
 */
class EventCursorTest {

  @Test
  fun `a cursor older than retention asks for a resync`() {
    val decision = decideCursor(
      current = 500L,
      frame = frameOf(resyncRequired = true, resyncFromSeq = 900L)
    )

    // Курсор встаёт туда, где лента идёт сейчас: иначе после полной выборки канал переподписался
    // бы со старым номером и получил бы тот же отказ — и так по кругу
    assertEquals(CursorDecision.Resync(fromSeq = 900L), decision)
  }

  @Test
  fun `a first launch takes the same path as an expired cursor`() {
    val expired = decideCursor(current = 500L, frame = frameOf(resyncRequired = true))
    val firstLaunch = decideCursor(current = 0L, frame = frameOf(resyncRequired = true))

    assertEquals(expired, firstLaunch)
  }

  @Test
  fun `a first launch accepts a frame that starts at the very first event`() {
    val decision = decideCursor(current = 0L, frame = frameOf(firstSeq = 1L, lastSeq = 7L))

    assertEquals(CursorDecision.Advance(7L), decision)
  }

  @Test
  fun `a gap in seq forces a resync instead of a silent skip`() {
    val decision = decideCursor(current = 10L, frame = frameOf(firstSeq = 14L, lastSeq = 20L))

    assertEquals(CursorDecision.Resync(fromSeq = 20L), decision)
  }

  @Test
  fun `an empty frame leaves the cursor where it was`() {
    val decision = decideCursor(current = 10L, frame = frameOf())

    assertEquals(CursorDecision.Advance(10L), decision)
  }

  @Test
  fun `a skipped unknown event does not read as a gap`() {
    // Кадр с событиями 11..13, из которых 12 — незнакомого типа: разобранных событий два,
    // но нумерация непрерывна, и полной выборки это стоить не должно
    val frame = frameOf(firstSeq = 11L, lastSeq = 13L)

    assertEquals(CursorDecision.Advance(13L), decideCursor(current = 10L, frame = frame))
  }
}
