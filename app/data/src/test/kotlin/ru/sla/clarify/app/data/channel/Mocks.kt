package ru.sla.clarify.app.data.channel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.sla.clarify.app.data.entity.DecodedFrame
import ru.sla.clarify.app.data.entity.SequencedEvent
import ru.sla.clarify.auth.session.domain.SessionKeyProvider
import ru.sla.clarify.auth.session.domain.entity.SessionKey
import java.io.IOException

/**
 * Иерархия событий той же формы, какую печатает генератор: sealed-конверт с плоским
 * дискриминатором. Кодек обобщён и о конкретном типе не знает, поэтому проверять его на этой
 * паре так же честно, как на настоящей.
 */
@Serializable
internal sealed interface TestEvent {

  @Serializable
  @SerialName("commit_created")
  data class CommitCreated(val id: String) : TestEvent

  @Serializable
  @SerialName("branch_created")
  data class BranchCreated(val id: String) : TestEvent
}

internal fun frameOf(
  resyncRequired: Boolean = false,
  resyncFromSeq: Long? = null,
  firstSeq: Long? = null,
  lastSeq: Long? = null
): DecodedFrame<TestEvent> {
  return DecodedFrame(
    resyncRequired = resyncRequired,
    resyncFromSeq = resyncFromSeq,
    events = emptyList(),
    skippedTypes = emptyList(),
    firstSeq = firstSeq,
    lastSeq = lastSeq
  )
}

/**
 * Курсор в памяти, честно по ключам: общий на все сессии соврал бы в тесте на смену пользователя,
 * а он тут ровно про то, что чужой курсор не наследуется.
 */
internal class FakeEventCursorStore(seq: Long = 0L, key: SessionKey = SessionKey("s1")) : EventCursorStore {
  private val seqByKey = mutableMapOf(key to seq)
  val saved = mutableListOf<Long>()

  override suspend fun read(key: SessionKey): Long = seqByKey[key] ?: 0L

  override suspend fun save(key: SessionKey, seq: Long) {
    seqByKey[key] = seq
    saved += seq
  }

  override suspend fun clear(key: SessionKey) {
    seqByKey.remove(key)
  }
}

internal class FakeSessionKeys(private val keys: Flow<SessionKey?>) : SessionKeyProvider {
  override suspend fun readKey(): SessionKey? = keys.firstOrNull()
  override fun key(): Flow<SessionKey?> = keys
}

internal fun sessionKeysOf(key: SessionKey?): FakeSessionKeys {
  return FakeSessionKeys(flowOf(key))
}

/**
 * Аппликатор, записывающий то, что до него дошло. [failWith] изображает отказ записи — случай,
 * ради которого курсор и двигается только после [apply].
 */
internal class FakeServerEventApplier(private val failWith: Throwable? = null) :
  ServerEventApplier<TestEvent> {

  val applied = mutableListOf<SequencedEvent<TestEvent>>()
  var resyncCount = 0
    private set

  override suspend fun apply(events: List<SequencedEvent<TestEvent>>) {
    failWith?.let { throw it }
    applied += events
  }

  override suspend fun resync() {
    resyncCount++
  }
}

/**
 * Транспорт, отдающий заготовленные соединения по порядку. [failOpenWith] роняет **первое**
 * открытие — тот обрыв, что случился до рукопожатия и потому приходит от транспорта его
 * собственным исключением, а не тем, что бросил движок.
 */
internal class FakeChannelTransport(
  connections: List<ChannelConnection>,
  private val failOpenWith: Throwable? = null
) : ChannelTransport {
  private val queue = ArrayDeque(connections)
  private var failedOpen = false

  override suspend fun open(): ChannelConnection {
    if (failOpenWith != null && !failedOpen) {
      failedOpen = true
      throw failOpenWith
    }
    return queue.removeFirstOrNull() ?: FakeChannelConnection(emptyList())
  }
}

/**
 * Соединение, отдающее заранее записанные кадры и затем закрывающееся. [failWith] позволяет
 * оборвать его так, как оборвалась бы сеть.
 */
internal class FakeChannelConnection(
  private val frames: List<String>,
  private val failWith: IOException? = null
) : ChannelConnection {
  val sent = mutableListOf<String>()
  var closed = false
    private set

  private var index = 0

  override suspend fun send(text: String) {
    sent += text
  }

  override suspend fun receive(): String? {
    if (index < frames.size) return frames[index++]
    failWith?.let { throw it }
    return null
  }

  override suspend fun close() {
    closed = true
  }
}
