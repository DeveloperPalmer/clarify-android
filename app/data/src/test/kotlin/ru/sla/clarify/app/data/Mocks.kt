package ru.sla.clarify.app.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import ru.kode.pathfinder.Environment
import ru.kode.pathfinder.EnvironmentId
import ru.kode.pathfinder.PathFinder
import ru.sla.clarify.auth.session.domain.AuthSessionRepository
import ru.sla.clarify.auth.session.domain.entity.AccessToken
import ru.sla.clarify.auth.session.domain.entity.AuthTokens
import ru.sla.clarify.auth.session.domain.entity.RefreshToken
import ru.sla.clarify.auth.session.domain.entity.SessionKey

internal fun authTokens(access: String, refresh: String): AuthTokens {
  return AuthTokens(
    accessToken = AccessToken(access),
    refreshToken = RefreshToken(refresh),
    updatedAt = 0L
  )
}

internal fun jsonHeaders(): Headers {
  return headersOf(HttpHeaders.ContentType, "application/json")
}

/**
 * PathFinder мокается, а не создаётся настоящий: его `create` — suspend и требует хранилища на
 * SQLDelight, то есть Android. Транспорту от него нужен ровно один адрес.
 */
internal fun pathFinderOf(baseUrl: String): PathFinder {
  val pathFinder = mockk<PathFinder>()
  every { pathFinder.currentEnvironment } returns Environment(
    id = EnvironmentId("test"),
    name = "Test",
    baseUrl = baseUrl
  )
  return pathFinder
}

/**
 * Клиент, собранный той же фабрикой, что уходит в приложение, — подменён только движок.
 * Обновление всегда отдаёт токен `fresh`, поэтому по заголовку второго запроса видно, дошёл ли
 * до него свежий токен.
 *
 * Сессия мокается: её собственное поведение, включая единственность обновления, стережёт
 * `SessionTest` в `auth-session`, а здесь проверяется транспорт вокруг неё.
 */
internal fun clientOf(tokens: AuthTokens?, handler: MockRequestHandler): HttpClient {
  val key = SessionKey("s1")
  val session = mockk<AuthSessionRepository>()
  coEvery { session.readKey() } returns key.takeIf { tokens != null }
  coEvery { session.readTokens(key) } returns tokens
  coEvery { session.refresh(any()) } returns authTokens(access = "fresh", refresh = "r2")

  return ServerHttpClientFactory(
    pathFinder = pathFinderOf("https://server.test/"),
    authSessionRepository = session
  ).create(MockEngine(handler))
}
