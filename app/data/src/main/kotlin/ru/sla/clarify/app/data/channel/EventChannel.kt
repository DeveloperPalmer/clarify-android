package ru.sla.clarify.app.data.channel

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ru.sla.clarify.app.data.CONTRACT_VERSION
import ru.sla.clarify.app.data.entity.SubscribeFrame
import ru.sla.clarify.auth.session.domain.SessionKeyProvider
import ru.sla.clarify.auth.session.domain.entity.SessionKey
import ru.sla.log.LogPriority
import ru.sla.log.asLog
import ru.sla.log.log
import java.io.IOException
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

/**
 * Единственное соединение, по которому приходят все события сервера.
 *
 * Обобщён по типу события: тип генерируется по спеке, и каналу его устройство ни к чему —
 * ему хватает кодека.
 *
 * Единственность имеет цену: разрыв — это разрыв всего сразу, а не одной ленты. Поэтому повтор
 * подключения здесь не украшение, а часть работы.
 */
class EventChannel<E>(
  private val transport: ChannelTransport,
  private val cursors: EventCursorStore,
  private val sessionKeys: SessionKeyProvider,
  private val codec: ServerEventCodec<E>,
  private val applier: ServerEventApplier<E>,
  private val json: Json
) {

  /**
   * Не возвращается, пока не отменят: канал живёт столько же, сколько приложение.
   *
   * Слушается именно поток ключа, а не однократное чтение. Смена ключа — вход под другим
   * пользователем — обрывает текущую сессию канала: продолжать чужим курсором нельзя. Заодно
   * запуск до логина не заканчивается ничем, а ждёт ключа.
   *
   * Отказ [ServerEventApplier] выходит отсюда наружу и канал заканчивает. Это выбор: курсор при
   * этом не сдвинут, поэтому перезапуск продолжит ровно с того же места, ничего не потеряв, —
   * а вечный повтор на детерминированной ошибке записи спрятал бы её насовсем.
   */
  suspend fun run() {
    sessionKeys.key().collectLatest { key ->
      if (key != null) {
        reconnectWhile(key)
      }
    }
  }

  private suspend fun reconnectWhile(key: SessionKey) {
    var failures = 0
    while (true) {
      failures = try {
        runSession(key)
        0
      } catch (e: IOException) {
        log(LogPriority.Warn) { e.asLog("event channel dropped") }
        failures + 1
      }
      delay(backoffMillis(failures).milliseconds)
    }
  }

  private suspend fun runSession(key: SessionKey) {
    var cursor = cursors.read(key)
    val connection = transport.open()
    try {
      val hello = SubscribeFrame(
        lastSeq = cursor,
        contractVersion = CONTRACT_VERSION
      )
      connection.send(json.encodeToString(SubscribeFrame.serializer(), hello))
      while (true) {
        val text = connection.receive() ?: return
        cursor = applyFrame(key, cursor, text)
      }
    } finally {
      // NonCancellable обязателен: отмена доходит сюда обычной отменой, и без него suspend-вызов
      // close() бросил бы сразу, не закрыв ничего, — сокет пережил бы того, кто его открыл
      withContext(NonCancellable) {
        try {
          connection.close()
        } catch (_: IOException) {
          // Закрытие уже мёртвого сокета — не новость и не ошибка
        }
      }
    }
  }

  private suspend fun applyFrame(key: SessionKey, cursor: Long, text: String): Long {
    val frame = codec.decode(text)
    if (frame.skippedTypes.isNotEmpty()) {
      log(LogPriority.Info) { "server is ahead of this client: ${frame.skippedTypes}" }
    }
    // Сначала применить, потом сдвинуть курсор — и в обеих ветках одинаково. Курсор означает
    // «клиент это записал»; сдвинутый раньше записи, он оставляет дыру, которую уже не переспросить
    val next = when (val decision = decideCursor(cursor, frame)) {
      is CursorDecision.Resync -> {
        applier.resync()
        decision.fromSeq
      }
      is CursorDecision.Advance -> {
        if (frame.events.isNotEmpty()) {
          applier.apply(frame.events)
        }
        decision.toSeq
      }
    }
    if (next != cursor) {
      cursors.save(key, next)
    }
    return next
  }
}

/**
 * Пауза перед повтором: секунда, затем удвоение до половины минуты.
 *
 * Нуля здесь нет намеренно. Штатно закрытое сервером соединение возвращается сюда так же, как
 * оборванное, и мгновенный повтор превратил бы «сервер закрывает сразу» в горячий цикл. Верхняя
 * граница — ради телефона: канал, ломящийся наружу каждую секунду в самолётном режиме, к утру
 * съест батарею.
 */
private fun backoffMillis(failures: Int): Long {
  return min(1_000L shl min(failures, 5), 30_000L)
}
