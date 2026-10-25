package ru.sla.clarify.app.data.channel

import com.squareup.anvil.annotations.ContributesBinding
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import ru.kode.pathfinder.PathFinder
import ru.sla.clarify.app.data.mapper.toWebSocketUrl
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import javax.inject.Inject

/**
 * Канал на том же клиенте, что и команды: один пул соединений, одни таймауты, один токен.
 *
 * Адрес читается при каждом открытии, а не запоминается: между разрывом и повтором стенд мог
 * смениться в дебаг-панели.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class KtorChannelTransport @Inject constructor(
  private val client: HttpClient,
  private val pathFinder: PathFinder
) : ChannelTransport {

  override suspend fun open(): ChannelConnection {
    val url = pathFinder.currentEnvironment.toWebSocketUrl(EVENTS_PATH)
    return KtorChannelConnection(client.webSocketSession(url))
  }
}

// Путь канала на сервере. Придёт из спеки; до тех пор — допущение.
private const val EVENTS_PATH = "/events"
