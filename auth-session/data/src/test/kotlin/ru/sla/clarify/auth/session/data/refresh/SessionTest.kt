package ru.sla.clarify.auth.session.data.refresh

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.sla.clarify.auth.session.domain.entity.AccessToken
import ru.sla.clarify.auth.session.domain.entity.RefreshRejectedException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Сторож обновления токена: единственное место сессии, где гонка стоит не повтора запроса,
 * а разлогина пользователя.
 */
class SessionTest {

  @Test
  fun `an expired access token is refreshed once for concurrent calls`() = runTest {
    val stale = authTokens(access = "stale", refresh = "r1")
    val persistence = FakeAuthSessionPersistence(tokens = stale)
    val calls = AtomicInteger()
    val refresher = AccessTokenRefresher(
      persistence = persistence,
      refresher = {
        calls.incrementAndGet()
        // Обмен на сервере не мгновенный — за это время в мьютекс упираются остальные вызовы
        delay(100)
        authTokens(access = "fresh", refresh = "r2")
      }
    )

    val results = (1..8)
      .map { async { refresher.refresh(stale.accessToken) } }
      .awaitAll()

    assertEquals(1, calls.get())
    assertEquals(1, persistence.saveCount)
    assertTrue(results.all { it?.accessToken == AccessToken("fresh") })
  }

  @Test
  fun `a rejected refresh token clears the session key`() = runTest {
    val persistence = FakeAuthSessionPersistence(
      tokens = authTokens(access = "stale", refresh = "rejected")
    )
    val refresher = AccessTokenRefresher(
      persistence = persistence,
      refresher = { throw RefreshRejectedException(cause = null) }
    )

    assertThrows<RefreshRejectedException> {
      refresher.refresh(AccessToken("stale"))
    }
    // Ключа нет — значит AuthSessionModel увидит Inactive и приложение уйдёт на логин
    assertNull(persistence.readKey())
  }

  @Test
  fun `a refresh without a stored session asks the server for nothing`() = runTest {
    val calls = AtomicInteger()
    val refresher = AccessTokenRefresher(
      persistence = FakeAuthSessionPersistence(key = null, tokens = null),
      refresher = {
        calls.incrementAndGet()
        authTokens(access = "fresh", refresh = "r2")
      }
    )

    assertNull(refresher.refresh(AccessToken("stale")))
    assertEquals(0, calls.get())
  }
}
