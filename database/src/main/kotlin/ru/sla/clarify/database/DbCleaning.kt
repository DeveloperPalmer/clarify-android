package ru.sla.clarify.database

import ru.sla.log.log

// TODO @sla @DB @Cleanup Improve DB cleanup
//   Everything is deleted globally, for all session keys. Correct approach would be for all tables to have
//   a 'sessionKey' column and delete only for this key

/**
 * Чистит все таблицы кэша чата. Settings живёт в [SettingsDatabase] (в ней хранится сама сессия —
 * токены/userId, ей управляет AuthSessionPersistence) и здесь не трогается.
 */
suspend fun ChatDatabase.cleanupBySessionKey(key: String) {
  log { "cleaning up data for session key=$key" }
  clearAllTables()
}
