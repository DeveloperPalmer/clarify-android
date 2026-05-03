package ru.sla.clarify.auth.session.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.auth.session.domain.entity.SessionKey

interface SessionKeyProvider {
  /**
   * Returns an active session key if any exists
   */
  suspend fun readKey(): SessionKey?

  /**
   * Emits changes of an active session key
   */
  fun key(): Flow<SessionKey?>
}
