package ru.sla.clarify.app.data.channel

import ru.sla.clarify.auth.session.domain.entity.SessionKey
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.database.SettingsDatabase
import ru.sla.clarify.database.entity.SettingsEntity
import javax.inject.Inject

@SingleIn(AppScope::class)
class PersistedEventCursorStore @Inject constructor(
  private val database: SettingsDatabase
) : EventCursorStore {

  override suspend fun read(key: SessionKey): Long {
    return database.settingsDao()
      .select(buildCursorKey(key))
      ?.toLongOrNull()
      ?: 0L
  }

  override suspend fun save(key: SessionKey, seq: Long) {
    database.settingsDao().insertOrReplace(
      SettingsEntity(
        key = buildCursorKey(key),
        value = seq.toString()
      )
    )
  }

  override suspend fun clear(key: SessionKey) {
    database.settingsDao().delete(buildCursorKey(key))
  }

  private fun buildCursorKey(key: SessionKey): SettingsEntity.Key {
    return SettingsEntity.Key("event_cursor|" + key.value)
  }
}
