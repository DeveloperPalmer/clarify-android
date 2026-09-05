package ru.sla.clarify.app.data.channel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.sla.clarify.auth.session.domain.SessionKeyProvider
import ru.sla.clarify.auth.session.domain.entity.SessionKey
import java.io.IOException

/**
 * Сторож канала. Главное здесь — что курсор двигается **после** записи: сдвинутый раньше, он
 * оставляет дыру, которую уже нечем переспросить.
 */
class EventChannelTest {

  private val json = Json { ignoreUnknownKeys = true }
  private val key = SessionKey("s1")

  private fun channelOf(
    cursors: EventCursorStore,
    connections: List<ChannelConnection>,
    applier: ServerEventApplier<TestEvent> = FakeServerEventApplier(),
    sessionKeys: SessionKeyProvider = sessionKeysOf(key)
  ): EventChannel<TestEvent> {
    val queue = ArrayDeque(connections)
    return EventChannel(
      transport = { queue.removeFirstOrNull() ?: FakeChannelConnection(emptyList()) },
      cursors = cursors,
      sessionKeys = sessionKeys,
      codec = ServerEventCodec(json, TestEvent.serializer()),
      applier = applier,
      json = json
    )
  }

  private fun commitFrame(seq: Long, id: String): String {
    return """{"events":[{"seq":$seq,"event":{"type":"commit_created","id":"$id"}}]}"""
  }

  @Test
  fun `a subscription asks to continue from the stored cursor`() = runTest {
    val connection = FakeChannelConnection(listOf(commitFrame(seq = 43L, id = "c1")))
    val job = backgroundScope.launch { channelOf(FakeEventCursorStore(seq = 42L), listOf(connection)).run() }
    runCurrent()
    job.cancel()

    assertEquals(listOf("""{"lastSeq":42}"""), connection.sent)
  }

  @Test
  fun `events reach the applier in order`() = runTest {
    val applier = FakeServerEventApplier()
    val connection = FakeChannelConnection(
      listOf(
        """
        {"events":[
          {"seq":11,"event":{"type":"commit_created","id":"c1"}},
          {"seq":12,"event":{"type":"branch_created","id":"b1"}}
        ]}
        """.trimIndent()
      )
    )

    val job = backgroundScope.launch {
      channelOf(FakeEventCursorStore(seq = 10L), listOf(connection), applier).run()
    }
    runCurrent()
    job.cancel()

    assertEquals(listOf(11L, 12L), applier.applied.map { it.seq })
    assertEquals(
      listOf(TestEvent.CommitCreated("c1"), TestEvent.BranchCreated("b1")),
      applier.applied.map { it.event }
    )
  }

  @Test
  fun `a failed apply does not move the cursor`() = runTest {
    val cursors = FakeEventCursorStore(seq = 10L)
    val channel = channelOf(
      cursors = cursors,
      connections = listOf(FakeChannelConnection(listOf(commitFrame(seq = 11L, id = "c1")))),
      applier = FakeServerEventApplier(failWith = IllegalStateException("disk is full"))
    )

    // Отказ записи выходит наружу и заканчивает канал: курсор не сдвинут, поэтому перезапуск
    // продолжит с того же места, ничего не потеряв
    assertThrows<IllegalStateException> { channel.run() }
    assertEquals(emptyList<Long>(), cursors.saved)
  }

  @Test
  fun `an expired cursor asks the applier to resync`() = runTest {
    val applier = FakeServerEventApplier()
    val cursors = FakeEventCursorStore(seq = 5L)
    val connection = FakeChannelConnection(
      listOf("""{"resyncRequired":true,"resyncFromSeq":900,"events":[]}""")
    )

    val job = backgroundScope.launch { channelOf(cursors, listOf(connection), applier).run() }
    runCurrent()
    job.cancel()

    assertEquals(1, applier.resyncCount)
    assertEquals(listOf(900L), cursors.saved)
  }

  @Test
  fun `a dropped connection is retried from the cursor it left off at`() = runTest {
    val first = FakeChannelConnection(
      listOf(commitFrame(seq = 11L, id = "c1")),
      failWith = IOException("connection reset")
    )
    val second = FakeChannelConnection(listOf(commitFrame(seq = 12L, id = "b1")))

    val job = backgroundScope.launch {
      channelOf(FakeEventCursorStore(seq = 10L), listOf(first, second)).run()
    }
    runCurrent()
    // Первый отказ ждёт две секунды: секунда — это пауза после штатного закрытия, дальше удвоение
    advanceTimeBy(2_001)
    runCurrent()
    job.cancel()

    assertTrue(first.closed)
    // Повтор идёт с 11: первый кадр записан и курсор сдвинут, второй раз его не переспрашивают
    assertEquals(listOf("""{"lastSeq":11}"""), second.sent)
  }

  @Test
  fun `a new session key does not inherit the previous cursor`() = runTest {
    val first = FakeChannelConnection(listOf(commitFrame(seq = 43L, id = "c1")))
    val second = FakeChannelConnection(emptyList())
    val keys = MutableStateFlow<SessionKey?>(key)

    val job = backgroundScope.launch {
      channelOf(
        cursors = FakeEventCursorStore(seq = 42L, key = key),
        connections = listOf(first, second),
        sessionKeys = FakeSessionKeys(keys)
      ).run()
    }
    runCurrent()
    keys.value = SessionKey("s2")
    runCurrent()
    job.cancel()

    assertEquals(listOf("""{"lastSeq":42}"""), first.sent)
    // Новый пользователь начинает с нуля, а не с чужого места
    assertEquals(listOf("""{"lastSeq":0}"""), second.sent)
  }

  @Test
  fun `without a session the channel does not open a connection`() = runTest {
    val connection = FakeChannelConnection(emptyList())
    val channel = channelOf(
      cursors = FakeEventCursorStore(),
      connections = listOf(connection),
      sessionKeys = sessionKeysOf(null)
    )

    channel.run()

    assertTrue(connection.sent.isEmpty())
  }
}
