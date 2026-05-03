package ru.sla.clarify.auth.session.data.storage

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.sla.clarify.auth.session.domain.AuthSessionRepository
import ru.sla.clarify.auth.session.domain.entity.AuthTokens
import ru.sla.clarify.auth.session.domain.entity.RefreshToken
import ru.sla.clarify.auth.session.domain.entity.SessionKey
import ru.sla.clarify.auth.session.domain.entity.UserId
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.cleanupBySessionKey
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class AuthSessionRepositoryImpl @Inject constructor(
  private val memoryDB: InMemoryDB,
  private val authSessionPersistence: AuthSessionPersistence
) : AuthSessionRepository {
  override suspend fun readTokens(key: SessionKey): AuthTokens? {
    return authSessionPersistence.readTokens(key)
  }

  override fun tokens(key: SessionKey): Flow<AuthTokens?> {
    return authSessionPersistence.tokens(key)
  }

  override suspend fun <T> withKey(
    body: suspend AuthSessionRepository.(key: SessionKey) -> T
  ): T = withContext(Dispatchers.Default) {
    body(readKey() ?: error("expected active session key"))
  }

  override suspend fun readKey(): SessionKey? {
    return authSessionPersistence.readKey()
  }

  override suspend fun readUserId(key: SessionKey): UserId? {
    return authSessionPersistence.readUserId(key)
  }

  override fun key(): Flow<SessionKey?> {
    return authSessionPersistence.key()
  }

  override suspend fun deleteTokens(key: SessionKey) {
    authSessionPersistence.deleteTokens(key)
    authSessionPersistence.deleteUserId(key)
  }

  override suspend fun refresh(refreshToken: RefreshToken) {
    // nothing to do
    // Add refresh method
  }

  override suspend fun startNew(tokens: AuthTokens, userId: UserId) {
    val key = authSessionPersistence.generateKeyAndSetActive()
    authSessionPersistence.saveTokens(key, tokens)
    authSessionPersistence.saveUserId(key, userId)
  }

  override suspend fun reset(cleanupStorage: Boolean) {
    val key = authSessionPersistence.readKey() ?: return
    // nothing to do
    // Add post logout method
    authSessionPersistence.deleteTokens(key)
    authSessionPersistence.deleteUserId(key)
    // TODO @sla @DB @Cleanup is cleanup actually needed?
    //   If our tables all contained and referenced "sessionKey", then logging in with a new user would simply
    //   use values related to a new key, no intersections, and no cleanup needed (only if data is really sensitive)
    if (cleanupStorage) {
      cleanupStorage(key)
    }
    authSessionPersistence.clearKey()
  }

  override suspend fun cleanupStorage(key: SessionKey) {
    withContext(Dispatchers.IO) {
      memoryDB.cleanupBySessionKey(key.value)
    }
  }
}
