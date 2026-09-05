package ru.sla.clarify.auth.session.domain.entity

/**
 * Сервер отверг refresh-токен: сессия закончилась и восстановить её нечем.
 *
 * Своё исключение, а не транспортное: `auth-session` о транспорте не знает и знать не должен —
 * иначе модуль сессии стал бы зависеть от того, кто ходит по сети. Транспорт ловит это на своей
 * границе и переводит в собственные термины.
 */
class RefreshRejectedException(cause: Throwable?) : RuntimeException("refresh token rejected", cause)
