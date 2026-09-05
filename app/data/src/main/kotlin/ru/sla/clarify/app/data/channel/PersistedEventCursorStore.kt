package ru.sla.clarify.app.data.channel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.domain.entity.SessionKey
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.database.PersistedDB
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Курсор лежит в персистентном хранилище, рядом с ключом сессии и токенами.
 *
 * Требование к месту ровно одно: пережить убийство процесса. В `InMemoryDB` курсор его не
 * переживёт, и каждый холодный старт превращался бы в полную выборку — то есть в то самое,
 * от чего курсор и заведён.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class PersistedEventCursorStore @Inject constructor(
  private val database: PersistedDB
) : EventCursorStore {

  override suspend fun read(key: SessionKey): Long {
    return withContext(Dispatchers.IO) {
      database.settingsQueries
        .selectByKey(buildCursorKey(key))
        .executeAsOneOrNull()
        ?.toLongOrNull()
        ?: 0L
    }
  }

  override suspend fun save(key: SessionKey, seq: Long) {
    withContext(Dispatchers.IO) {
      database.settingsQueries.insertOrReplace(buildCursorKey(key), seq.toString())
    }
  }

  override suspend fun clear(key: SessionKey) {
    withContext(Dispatchers.IO) {
      database.settingsQueries.deleteByKey(buildCursorKey(key))
    }
  }

  private fun buildCursorKey(key: SessionKey): String {
    return "event_cursor|" + key.value
  }
}
