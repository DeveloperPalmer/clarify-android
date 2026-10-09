package ru.sla.clarify.auth.session.data.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.domain.entity.AccessToken
import ru.sla.clarify.auth.session.domain.entity.AuthTokens
import ru.sla.clarify.auth.session.domain.entity.RefreshToken
import ru.sla.clarify.auth.session.domain.entity.SessionKey
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.domain.mapDistinctNotNullChanges
import ru.sla.clarify.core.domain.randomUuid
import ru.sla.clarify.database.SettingsDatabase
import ru.sla.clarify.database.entity.SettingsEntity
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AuthSessionPersistenceImpl @Inject constructor(
  private val database: SettingsDatabase
) : AuthSessionPersistence {

  override fun <R> produceUserId(transform: (UserId) -> Flow<R>): Flow<R> {
    return key()
      .filterNotNull()
      .mapDistinctNotNullChanges { readUserId(it) }
      .flatMapLatest { transform(it) }
  }

  override suspend fun saveTokens(key: SessionKey, tokens: AuthTokens) {
    database.settingsDao().insertOrReplace(
      SettingsEntity(
        key = buildTokensKey(key),
        value = serializeTokens(tokens)
      )
    )
  }

  override fun tokens(key: SessionKey): Flow<AuthTokens?> {
    return database.settingsDao()
      .observe(buildTokensKey(key))
      .map { dbRecord ->
        dbRecord?.let { deserializeTokens(it) } ?: return@map null
      }
  }

  override suspend fun readTokens(key: SessionKey): AuthTokens? {
    val dbRecord = database.settingsDao().select(buildTokensKey(key))
    return dbRecord?.let { deserializeTokens(it) }
  }

  override suspend fun saveUserId(key: SessionKey, userId: UserId) {
    database.settingsDao().insertOrReplace(
      SettingsEntity(
        key = buildUserIdKey(key),
        value = userId.value
      )
    )
  }

  override suspend fun readUserId(key: SessionKey): UserId? {
    return database.settingsDao()
      .select(buildUserIdKey(key))
      ?.let(::UserId)
  }

  override suspend fun deleteUserId(key: SessionKey) {
    database.settingsDao().delete(buildUserIdKey(key))
  }

  private fun buildTokensKey(sessionKey: SessionKey): SettingsEntity.Key {
    return SettingsEntity.Key(PREF_KEY_TOKENS + PREF_KEY_SEPARATOR + sessionKey.value)
  }

  private fun buildUserIdKey(sessionKey: SessionKey): SettingsEntity.Key {
    return SettingsEntity.Key(PREF_KEY_USER_ID + PREF_KEY_SEPARATOR + sessionKey.value)
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
    database.settingsDao().delete(buildTokensKey(key))
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
    return database.settingsDao()
      .observe(PREF_KEY_ACTIVE_SESSION_KEY)
      .map { it?.let(::SessionKey) }
  }

  override suspend fun readKey(): SessionKey? {
    return database.settingsDao()
      .select(PREF_KEY_ACTIVE_SESSION_KEY)
      ?.let { SessionKey(it) }
  }

  private suspend fun writeActiveKey(key: SessionKey) {
    database.settingsDao().insertOrReplace(
      SettingsEntity(
        key = PREF_KEY_ACTIVE_SESSION_KEY,
        value = key.value
      )
    )
  }

  private suspend fun deleteActiveKey() {
    database.settingsDao().delete(PREF_KEY_ACTIVE_SESSION_KEY)
  }
}

private val PREF_KEY_ACTIVE_SESSION_KEY = SettingsEntity.Key("session_key")
private const val PREF_KEY_TOKENS = "tokens"
private const val PREF_KEY_USER_ID = "user_id"
private const val PREF_KEY_SEPARATOR = "::::"
