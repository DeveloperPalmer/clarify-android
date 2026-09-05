package ru.sla.clarify.auth.session.data.refresh

import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.domain.TokenRefresher
import ru.sla.clarify.auth.session.domain.entity.AuthTokens
import ru.sla.clarify.auth.session.domain.entity.RefreshRejectedException
import ru.sla.clarify.auth.session.domain.entity.RefreshToken
import ru.sla.clarify.core.domain.di.scope.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

/**
 * Отказывает всегда: обменять refresh-токен не на что, пока в API нет такой операции.
 *
 * Это не заглушка ради компиляции, а единственный честный ответ. Раз протухший access обновить
 * нечем, сессия закончилась — и сказать об этом отказом лучше, чем сделать вид, что обновление
 * прошло.
 *
 * Заменяется реализацией, вызывающей операцию обновления, как только та появится.
 */
@ContributesBinding(AppScope::class)
class UnavailableTokenRefresher @Inject constructor() : TokenRefresher {

  override suspend fun refresh(token: RefreshToken): AuthTokens {
    throw RefreshRejectedException(cause = null)
  }
}
