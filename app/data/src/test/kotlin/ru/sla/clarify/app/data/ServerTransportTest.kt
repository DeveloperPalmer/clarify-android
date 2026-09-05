package ru.sla.clarify.app.data

import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.sla.clarify.app.data.entity.ServerException
import java.io.IOException

/**
 * Сторож границы транспорта: наружу выходят только [ServerException], а 401 стоит одного
 * повтора со свежим токеном, а не отказа вызывающему.
 */
class ServerTransportTest {

  @Test
  fun `a request carries the bearer token of the current session`() = runTest {
    val seen = mutableListOf<String?>()
    val client = clientOf(
      tokens = authTokens(access = "a1", refresh = "r1"),
      handler = { request ->
        seen += request.headers[HttpHeaders.Authorization]
        respond("{}", HttpStatusCode.OK, jsonHeaders())
      }
    )

    client.get("/user")

    assertEquals(listOf("Bearer a1"), seen)
  }

  @Test
  fun `an unauthorized response is retried once with the refreshed token`() = runTest {
    val seen = mutableListOf<String?>()
    val client = clientOf(
      tokens = authTokens(access = "stale", refresh = "r1"),
      handler = { request ->
        seen += request.headers[HttpHeaders.Authorization]
        when (seen.size) {
          1 -> respond("", HttpStatusCode.Unauthorized, jsonHeaders())
          else -> respond("{}", HttpStatusCode.OK, jsonHeaders())
        }
      }
    )

    client.get("/user")

    assertEquals(listOf("Bearer stale", "Bearer fresh"), seen)
  }

  @Test
  fun `an unauthorized response without a session surfaces as unauthorized`() = runTest {
    val client = clientOf(
      tokens = null,
      handler = { respond("", HttpStatusCode.Unauthorized, jsonHeaders()) }
    )

    assertThrows<ServerException.Unauthorized> { client.get("/user") }
  }

  @Test
  fun `an unauthorized retry is not retried again`() = runTest {
    var attempts = 0
    val client = clientOf(
      tokens = authTokens(access = "stale", refresh = "r1"),
      handler = {
        attempts++
        respond("", HttpStatusCode.Unauthorized, jsonHeaders())
      }
    )

    assertThrows<ServerException.Unauthorized> { client.get("/user") }
    // Первый заход и ровно один повтор: свежий токен, отвергнутый сразу же, — это конец сессии,
    // а не повод ходить по кругу
    assertEquals(2, attempts)
  }

  @Test
  fun `a named server error surfaces as a typed api failure`() = runTest {
    val client = clientOf(
      tokens = null,
      handler = {
        respond(
          """{"code":"conversation_not_found","message":"no such conversation"}""",
          HttpStatusCode.NotFound,
          jsonHeaders()
        )
      }
    )

    val error = assertThrows<ServerException.ApiError> { client.get("/conversation/1") }
    assertEquals(404, error.status)
    assertEquals("conversation_not_found", error.code)
    assertEquals("no such conversation", error.serverMessage)
  }

  @Test
  fun `a server error without a code keeps its status`() = runTest {
    val client = clientOf(
      tokens = null,
      handler = { respond("<html>bad gateway</html>", HttpStatusCode.BadGateway, jsonHeaders()) }
    )

    val error = assertThrows<ServerException.ApiError> { client.get("/conversation/1") }
    assertEquals(502, error.status)
    assertNull(error.code)
  }

  @Test
  fun `a websocket handshake is not mistaken for a failure`() = runTest {
    val client = clientOf(
      tokens = authTokens(access = "a1", refresh = "r1"),
      handler = { respond("", HttpStatusCode.SwitchingProtocols, jsonHeaders()) }
    )

    // 101 не входит в 2xx, и без отдельной оговорки перехватчик развернул бы рукопожатие канала
    // в ServerException.Api — на каждом подключении
    assertEquals(HttpStatusCode.SwitchingProtocols, client.get("/events").status)
  }

  @Test
  fun `a broken connection surfaces as unreachable`() = runTest {
    val client = clientOf(
      tokens = null,
      handler = { throw IOException("connection reset") }
    )

    assertThrows<ServerException.Unreachable> { client.get("/user") }
  }
}
