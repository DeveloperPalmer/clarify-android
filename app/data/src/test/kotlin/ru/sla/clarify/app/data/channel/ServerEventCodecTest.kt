package ru.sla.clarify.app.data.channel

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.sla.clarify.app.data.entity.ServerException

/**
 * Сторож двухшагового разбора. Незнакомый тип события — норма: сервер обновляется раньше
 * клиента. Знакомый, но не разобравшийся, — не норма: это разошедшаяся копия спеки.
 */
class ServerEventCodecTest {

  private val codec = ServerEventCodec(Json { ignoreUnknownKeys = true }, TestEvent.serializer())

  @Test
  fun `a discriminator value maps to exactly one event class`() {
    val frame = codec.decode(
      """{"events":[{"seq":1,"event":{"type":"commit_created","id":"c1"}}]}"""
    )

    assertEquals(listOf(TestEvent.CommitCreated("c1")), frame.events.map { it.event })
    assertEquals(listOf(1L), frame.events.map { it.seq })
  }

  @Test
  fun `an unknown event type does not fail the surrounding frame`() {
    val frame = codec.decode(
      """
      {"events":[
        {"seq":11,"event":{"type":"commit_created","id":"c1"}},
        {"seq":12,"event":{"type":"reaction_added","id":"r1"}},
        {"seq":13,"event":{"type":"branch_created","id":"b1"}}
      ]}
      """.trimIndent()
    )

    assertEquals(
      listOf(TestEvent.CommitCreated("c1"), TestEvent.BranchCreated("b1")),
      frame.events.map { it.event }
    )
    assertEquals(listOf("reaction_added"), frame.skippedTypes)
  }

  @Test
  fun `a skipped event still counts towards the frame sequence range`() {
    val frame = codec.decode(
      """
      {"events":[
        {"seq":11,"event":{"type":"reaction_added","id":"r1"}},
        {"seq":12,"event":{"type":"commit_created","id":"c1"}}
      ]}
      """.trimIndent()
    )

    // Номера берутся по сырому кадру: иначе пропущенное событие на первой позиции выглядело бы
    // разрывом и стоило бы полной выборки
    assertEquals(11L, frame.firstSeq)
    assertEquals(12L, frame.lastSeq)
  }

  @Test
  fun `a known event that does not decode fails loudly`() {
    assertThrows<ServerException.Malformed> {
      codec.decode("""{"events":[{"seq":1,"event":{"type":"commit_created"}}]}""")
    }
  }

  @Test
  fun `an event without a discriminator is skipped, not decoded`() {
    val frame = codec.decode("""{"events":[{"seq":1,"event":{"id":"c1"}}]}""")

    assertEquals(emptyList<TestEvent>(), frame.events.map { it.event })
    assertEquals(listOf("<none>"), frame.skippedTypes)
  }

  @Test
  fun `a resync frame carries no events`() {
    val frame = codec.decode("""{"resyncRequired":true,"events":[]}""")

    assertEquals(true, frame.resyncRequired)
    assertEquals(emptyList<Long>(), frame.events.map { it.seq })
  }

  @Test
  fun `the known type list comes from the serializer, not a hand written copy`() {
    // Ломается, если kotlinx изменит форму дескриптора sealed-иерархии: тогда кодек перестанет
    // узнавать типы и молча пропустит вообще всё
    val frame = codec.decode(
      """{"events":[{"seq":1,"event":{"type":"branch_created","id":"b1"}}]}"""
    )

    assertEquals(listOf(TestEvent.BranchCreated("b1")), frame.events.map { it.event })
  }
}
