package ru.sla.clarify.database

import ru.sla.log.log

// TODO @sla @DB @Cleanup Улучшить очистку БД
//   Сейчас удаляется всё сразу, для всех ключей сессии. Правильнее завести во всех таблицах колонку
//   'sessionKey' и удалять только строки этого ключа

/**
 * Чистит все таблицы кэша чата. Settings живёт в [SettingsDatabase] (в ней хранится сама сессия —
 * токены/userId, ей управляет AuthSessionPersistence) и здесь не трогается.
 */
suspend fun ChatDatabase.cleanupBySessionKey(key: String) {
  log { "cleaning up data for session key=$key" }
  clearAllTables()
}
