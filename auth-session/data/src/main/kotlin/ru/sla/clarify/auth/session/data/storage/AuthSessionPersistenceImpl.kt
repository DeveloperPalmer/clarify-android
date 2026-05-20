package ru.sla.clarify.auth.session.data.storage

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.sla.clarify.auth.session.domain.entity.AccessToken
import ru.sla.clarify.auth.session.domain.entity.AuthTokens
import ru.sla.clarify.auth.session.domain.entity.RefreshToken
import ru.sla.clarify.auth.session.domain.entity.SessionKey
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.domain.mapDistinctNotNullChanges
import ru.sla.clarify.core.domain.randomUuid
import ru.sla.clarify.database.PersistedDB
import javax.inject.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AuthSessionPersistenceImpl @Inject constructor(
  private val database: PersistedDB
) : AuthSessionPersistence {

  override fun <R> produceUserId(transform: (UserId) -> Flow<R>): Flow<R> {
    return key()
      .filterNotNull()
      .mapDistinctNotNullChanges { readUserId(it) }
      .flatMapLatest { transform(it) }
  }

  override suspend fun saveTokens(key: SessionKey, tokens: AuthTokens) {
    withContext(Dispatchers.IO) {
      database.settingsQueries.save(
        buildTokensSettingsKey(key),
        serializeTokens(tokens)
      )
    }
  }

  override fun tokens(key: SessionKey): Flow<AuthTokens?> {
    return database.settingsQueries
      .get(buildTokensSettingsKey(key))
      .asFlow()
      .mapToOneOrNull(Dispatchers.IO)
      .map { dbRecord ->
        dbRecord?.let { deserializeTokens(it) } ?: return@map null
      }
  }

  override suspend fun readTokens(key: SessionKey): AuthTokens? {
    return withContext(Dispatchers.IO) {
      val dbRecord = database.settingsQueries
        .get(buildTokensSettingsKey(key))
        .executeAsOneOrNull()
      dbRecord?.let { deserializeTokens(it) }
    }
  }

  override suspend fun saveUserId(key: SessionKey, userId: UserId) {
    withContext(Dispatchers.IO) {
      database.settingsQueries.save(
        buildUserIdSettingsKey(key),
        userId.value
      )
    }
  }

  override suspend fun readUserId(key: SessionKey): UserId? {
    return withContext(Dispatchers.IO) {
      database.settingsQueries
        .get(buildUserIdSettingsKey(key))
        .executeAsOneOrNull()
        ?.let(::UserId)
    }
  }

  override suspend fun deleteUserId(key: SessionKey) {
    withContext(Dispatchers.IO) {
      database.settingsQueries
        .delete(buildUserIdSettingsKey(key))
    }
  }

  private fun buildTokensSettingsKey(sessionKey: SessionKey): String {
    return PREF_KEY_TOKENS + PREF_KEY_SEPARATOR + sessionKey.value
  }

  private fun buildUserIdSettingsKey(sessionKey: SessionKey): String {
    return PREF_KEY_USER_ID + PREF_KEY_SEPARATOR + sessionKey.value
  }

  private fun serializeTokens(tokens: AuthTokens): String {
    return tokens.accessToken.value + PREF_KEY_SEPARATOR +
      tokens.refreshToken.value + PREF_KEY_SEPARATOR +
      tokens.updatedAt
  }

  private fun deserializeTokens(dbRecord: String): AuthTokens {
    val tokens = dbRecord.split(PREF_KEY_SEPARATOR)
    check(tokens.size == 3) { "expected tokens pref value to consist of 3 parts, got ${tokens.size}" }
    return AuthTokens(
      AccessToken(tokens[0]),
      RefreshToken(tokens[1]),
      updatedAt = tokens[2].toLong()
    )
  }

  override suspend fun deleteTokens(key: SessionKey) {
    withContext(Dispatchers.IO) {
      database.settingsQueries
        .delete(buildTokensSettingsKey(key))
    }
  }

  override suspend fun <T> withKey(body: suspend AuthSessionPersistence.(key: SessionKey) -> T): T {
    return body(readKey() ?: error("expected active session key"))
  }

  override suspend fun generateKeyAndSetActive(): SessionKey {
    val key = SessionKey(randomUuid())
    writeActiveKey(key)
    return key
  }

  override suspend fun clearKey() {
    deleteActiveKey()
  }

  override fun key(): Flow<SessionKey?> {
    return database.settingsQueries
      .get(PREF_KEY_ACTIVE_SESSION_KEY)
      .asFlow()
      .mapToOneOrNull(Dispatchers.IO)
      .map { it?.let(::SessionKey) }
  }

  override suspend fun readKey(): SessionKey? {
    return withContext(Dispatchers.IO) {
      database.settingsQueries
        .get(PREF_KEY_ACTIVE_SESSION_KEY)
        .executeAsOneOrNull()
        ?.let { SessionKey(it) }
    }
  }

  private suspend fun writeActiveKey(key: SessionKey) {
    withContext(Dispatchers.IO) {
      database.settingsQueries
        .save(PREF_KEY_ACTIVE_SESSION_KEY, key.value)
    }
  }

  private suspend fun deleteActiveKey() {
    withContext(Dispatchers.IO) {
      database.settingsQueries
        .delete(PREF_KEY_ACTIVE_SESSION_KEY)
    }
  }
}

private const val PREF_KEY_ACTIVE_SESSION_KEY = "session_key"
private const val PREF_KEY_TOKENS = "tokens"
private const val PREF_KEY_USER_ID = "user_id"
private const val PREF_KEY_SEPARATOR = "::::"
