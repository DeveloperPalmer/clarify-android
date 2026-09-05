package ru.sla.clarify.app.data.channel

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.ClosedReceiveChannelException

internal class KtorChannelConnection(
  private val session: DefaultClientWebSocketSession
) : ChannelConnection {

  override suspend fun send(text: String) {
    session.send(Frame.Text(text))
  }

  override suspend fun receive(): String? {
    // Не-текстовые кадры пропускаем: ping/pong Ktor обрабатывает сам, а двоичных в протоколе нет
    while (true) {
      val frame = try {
        session.incoming.receive()
      } catch (_: ClosedReceiveChannelException) {
        return null
      }
      if (frame is Frame.Text) {
        return frame.readText()
      }
    }
  }

  override suspend fun close() {
    session.close()
  }
}
