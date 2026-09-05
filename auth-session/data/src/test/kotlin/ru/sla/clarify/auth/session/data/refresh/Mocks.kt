package ru.sla.clarify.auth.session.data.refresh

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.auth.session.domain.entity.AccessToken
import ru.sla.clarify.auth.session.domain.entity.AuthTokens
import ru.sla.clarify.auth.session.domain.entity.RefreshToken
import ru.sla.clarify.auth.session.domain.entity.SessionKey
import ru.sla.clarify.core.domain.entity.UserId

internal fun authTokens(access: String, refresh: String): AuthTokens {
  return AuthTokens(
    accessToken = AccessToken(access),
    refreshToken = RefreshToken(refresh),
    updatedAt = 0L
  )
}

/**
 * Сессия в памяти. Тестовый диспетчер однопоточный, поэтому обычных полей довольно — гонку
 * здесь создаёт не параллелизм, а точки приостановки.
 *
 * Методы, до которых обновление токена не дотягивается, честно падают: молчаливая заглушка
 * позволила бы тесту пройти на пути, которого никто не проверял.
 */
internal class FakeAuthSessionPersistence(
  private var key: SessionKey? = SessionKey("s1"),
  private var tokens: AuthTokens? = null
) : AuthSessionPersistence {

  var saveCount: Int = 0
    private set

  override suspend fun readKey(): SessionKey? = key

  override suspend fun clearKey() {
    key = null
  }

  override suspend fun readTokens(key: SessionKey): AuthTokens? = tokens

  override suspend fun saveTokens(key: SessionKey, tokens: AuthTokens) {
    saveCount++
    this.tokens = tokens
  }

  override suspend fun deleteTokens(key: SessionKey) {
    tokens = null
  }

  override fun tokens(key: SessionKey): Flow<AuthTokens?> = flowOf(tokens)

  override fun key(): Flow<SessionKey?> = flowOf(key)

  override fun <R> produceUserId(transform: (UserId) -> Flow<R>): Flow<R> {
    error("not reached by token refresh")
  }

  override suspend fun saveUserId(key: SessionKey, userId: UserId) {
    error("not reached by token refresh")
  }

  override suspend fun readUserId(key: SessionKey): UserId? {
    error("not reached by token refresh")
  }

  override suspend fun deleteUserId(key: SessionKey) {
    error("not reached by token refresh")
  }

  override suspend fun <T> withKey(body: suspend AuthSessionPersistence.(key: SessionKey) -> T): T {
    error("not reached by token refresh")
  }

  override suspend fun generateKeyAndSetActive(): SessionKey {
    error("not reached by token refresh")
  }
}
