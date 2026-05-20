package ru.sla.clarify.auth.session.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.auth.session.domain.entity.AuthTokens
import ru.sla.clarify.auth.session.domain.entity.RefreshToken
import ru.sla.clarify.auth.session.domain.entity.SessionKey
import ru.sla.clarify.core.domain.entity.UserId

interface AuthSessionRepository {
  suspend fun refresh(refreshToken: RefreshToken)

  /**
   * Starts new auth session with specified tokens
   */
  suspend fun startNew(tokens: AuthTokens, userId: UserId)

  /**
   * Resets session, removing all stored secrets and tokens.
   * Does nothing if no session is currently active.
   *
   * Additionally cleans up data storage (DB) if [cleanupStorage] is true and session key is available
   */
  suspend fun reset(cleanupStorage: Boolean)

  /**
   * Resets only tokens, but not secrets. (F.e., if pin is set, session can be refreshed after calling this method)
   */
  suspend fun deleteTokens(key: SessionKey)

  suspend fun readTokens(key: SessionKey): AuthTokens?
  fun tokens(key: SessionKey): Flow<AuthTokens?>

  suspend fun readUserId(key: SessionKey): UserId?

  /**
   * Executes a code block and passes a currently active session [key] to it.
   *
   * @throws IllegalStateException if no key is currently active.
   */
  suspend fun <T> withKey(body: suspend AuthSessionRepository.(key: SessionKey) -> T): T

  /**
   * Returns an active session key if any exists
   */
  suspend fun readKey(): SessionKey?

  /**
   * Emits changes of an active session key
   */
  fun key(): Flow<SessionKey?>

  /**
   * Cleans up all data stored by in-memory and persisted storage DB for session [key]
   */
  suspend fun cleanupStorage(key: SessionKey)
}
