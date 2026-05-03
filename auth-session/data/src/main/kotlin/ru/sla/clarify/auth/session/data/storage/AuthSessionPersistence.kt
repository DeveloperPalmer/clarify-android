package ru.sla.clarify.auth.session.data.storage

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.auth.session.domain.SessionKeyProvider
import ru.sla.clarify.auth.session.domain.entity.AuthTokens
import ru.sla.clarify.auth.session.domain.entity.SessionKey
import ru.sla.clarify.auth.session.domain.entity.UserId

interface AuthSessionPersistence : SessionKeyProvider {
  suspend fun saveTokens(key: SessionKey, tokens: AuthTokens)
  suspend fun readTokens(key: SessionKey): AuthTokens?
  suspend fun deleteTokens(key: SessionKey)

  fun <R> produceUserId(transform: (UserId) -> Flow<R>): Flow<R>
  suspend fun saveUserId(key: SessionKey, userId: UserId)
  suspend fun readUserId(key: SessionKey): UserId?
  suspend fun deleteUserId(key: SessionKey)

  fun tokens(key: SessionKey): Flow<AuthTokens?>

  /**
   * Executes a code block and passes a currently active session [key] to it.
   *
   * @throws IllegalStateException if no key is currently active.
   */
  suspend fun <T> withKey(body: suspend AuthSessionPersistence.(key: SessionKey) -> T): T

  /**
   * Generates a new [SessionKey] and marks it as currently active.
   *
   * [SessionKey] does not change for a single user session: from login until logout.
   * It doesn't change between token refreshes.
   */
  suspend fun generateKeyAndSetActive(): SessionKey

  /**
   * Returns an active session key if any exists
   */
  override suspend fun readKey(): SessionKey?

  /**
   * Emits changes of an active session key
   */
  override fun key(): Flow<SessionKey?>

  /**
   * Clears active session key
   */
  suspend fun clearKey()
}
