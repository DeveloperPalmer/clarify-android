package ru.sla.clarify.app.data.channel

import ru.sla.clarify.auth.session.domain.entity.SessionKey

/**
 * Место, где `lastSeq` переживает убийство процесса.
 *
 * Курсор привязан к ключу сессии: вход под другим пользователем не должен наследовать чужой
 * номер, иначе клиент попросит догон с места, до которого этому пользователю дела нет.
 */
interface EventCursorStore {

  /** Ноль означает «ничего ещё не получено» — им же начинается и первый запуск. */
  suspend fun read(key: SessionKey): Long

  suspend fun save(key: SessionKey, seq: Long)

  suspend fun clear(key: SessionKey)
}
