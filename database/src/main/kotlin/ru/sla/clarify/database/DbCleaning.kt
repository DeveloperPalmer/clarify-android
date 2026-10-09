package ru.sla.clarify.database

import ru.sla.log.log

// TODO @sla @DB @Cleanup Улучшить очистку БД
//   Сейчас удаляется всё сразу, для всех ключей сессии. Правильнее завести во всех таблицах колонку
//   'sessionKey' и удалять только строки этого ключа

/**
 * Чистит все таблицы [ChatDatabase] и только их: [SettingsDatabase] — отдельная база, и эта очистка
 * её не задевает.
 */
suspend fun ChatDatabase.cleanupBySessionKey(key: String) {
  log { "cleaning up data for session key=$key" }
  clearAllTables()
}
