package ru.sla.clarify.auth.session.data.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.domain.AuthSessionRepository
import ru.sla.clarify.auth.session.domain.entity.AuthTokens
import ru.sla.clarify.auth.session.domain.entity.RefreshToken
import ru.sla.clarify.auth.session.domain.entity.SessionKey
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.PersistedDB
import ru.sla.clarify.database.cleanupBySessionKey
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@ContributesBinding(AppScope::class)
class AuthSessionRepositoryImpl @Inject constructor(
  private val persistedDB: PersistedDB,
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
  ): T {
    return body(readKey() ?: error("expected active session key"))
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
    // Пока ничего не делаем
    // Добавить метод обновления токена
  }

  override suspend fun startNew(tokens: AuthTokens, userId: UserId) {
    val key = authSessionPersistence.generateKeyAndSetActive()
    authSessionPersistence.saveTokens(key, tokens)
    authSessionPersistence.saveUserId(key, userId)
  }

  override suspend fun reset(cleanupStorage: Boolean) {
    val key = authSessionPersistence.readKey() ?: return
    // Пока ничего не делаем
    // Добавить метод пост-логаута
    authSessionPersistence.deleteTokens(key)
    authSessionPersistence.deleteUserId(key)
    // TODO @sla @DB @Cleanup нужен ли вообще cleanup?
    //   Если бы все наши таблицы содержали и ссылались на "sessionKey", то логин под новым пользователем
    //   просто использовал бы значения, привязанные к новому ключу — никаких пересечений, никакой очистки
    //   не требуется (разве что если данные действительно чувствительные)
    if (cleanupStorage) {
      cleanupStorage(key)
    }
    authSessionPersistence.clearKey()
  }

  override suspend fun cleanupStorage(key: SessionKey) {
    withContext(Dispatchers.IO) {
      persistedDB.cleanupBySessionKey(key.value)
    }
  }
}
