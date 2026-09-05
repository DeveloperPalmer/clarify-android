package ru.sla.clarify.app.data.channel

/**
 * Открытое соединение канала, в терминах текста, а не кадров WebSocket.
 *
 * Порт существует ради проверяемости: `MockEngine` Ktor умеет HTTP, но не WebSocket, и без этого
 * шва повтор подключения и работу с курсором пришлось бы проверять живым сервером — то есть не
 * проверять вовсе.
 */
interface ChannelConnection {

  suspend fun send(text: String)

  /** `null` означает, что соединение закрылось штатно и читать больше нечего. */
  suspend fun receive(): String?

  suspend fun close()
}
