package ru.sla.clarify.auth.session.data.refresh

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.sla.clarify.auth.session.domain.entity.AccessToken
import ru.sla.clarify.auth.session.domain.entity.RefreshRejectedException

/**
 * Пока обновление невозможно, 401 обязан заканчиваться логаутом, а не зависанием.
 * Уезжает вместе с [UnavailableTokenRefresher], когда обновление станет настоящим.
 */
class UnavailableTokenRefresherTest {

  @Test
  fun `today a refresh cannot succeed and ends the session`() = runTest {
    val persistence = FakeAuthSessionPersistence(
      tokens = authTokens(access = "stale", refresh = "r1")
    )
    val refresher = AccessTokenRefresher(persistence, UnavailableTokenRefresher())

    assertThrows<RefreshRejectedException> {
      refresher.refresh(AccessToken("stale"))
    }
    assertNull(persistence.readKey())
  }
}
