package ru.sla.clarify.auth.session.data.refresh

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.auth.session.domain.TokenRefresher
import ru.sla.clarify.auth.session.domain.entity.AccessToken
import ru.sla.clarify.auth.session.domain.entity.AuthTokens
import ru.sla.clarify.auth.session.domain.entity.RefreshRejectedException
import ru.sla.clarify.core.domain.di.scope.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Обновляет пару токенов ровно один раз на всех, кто упёрся в один и тот же протухший access.
 *
 * Единственность держится не флагом «идёт обновление», а сравнением: под мьютексом проверяется,
 * тот ли access-токен всё ещё лежит в хранилище. Пришедший вторым видит уже другой и уходит с
 * чужим результатом, не сходив на сервер. Флаг для этого не годится — он отвечает на вопрос
 * «обновляется ли прямо сейчас», а нужен ответ на «обновилось ли уже».
 *
 * Дверь в обновление ровно одна, и это она. Второй вход — например метод на репозитории — обошёл
 * бы мьютекс, и гарантия единственности перестала бы существовать, оставшись на словах.
 */
@SingleIn(AppScope::class)
@Inject
class AccessTokenRefresher(
  private val persistence: AuthSessionPersistence,
  private val refresher: TokenRefresher
) {

  private val mutex = Mutex()

  /**
   * @param staleAccessToken токен, с которым вызывающий получил 401.
   * @return свежая пара либо `null`, если сессии уже нет.
   * @throws RefreshRejectedException если сервер отверг refresh-токен. К этому моменту ключ
   *   сессии уже стёрт, и приложение увидит `AuthSessionState.Inactive`.
   */
  suspend fun refresh(staleAccessToken: AccessToken): AuthTokens? {
    return mutex.withLock {
      val key = persistence.readKey() ?: return@withLock null
      val current = persistence.readTokens(key) ?: return@withLock null
      if (current.accessToken != staleAccessToken) {
        // Пока ждали мьютекс, пару обновил параллельный вызов — своего запроса не делаем
        return@withLock current
      }
      val fresh = try {
        refresher.refresh(current.refreshToken)
      } catch (e: RefreshRejectedException) {
        // Сессия кончилась: гасим её здесь, а не оставляем мёртвую пару лежать. За ключом следит
        // AuthSessionModel, и его исчезновение и есть сигнал «нужен логин»
        persistence.deleteTokens(key)
        persistence.clearKey()
        throw e
      }
      persistence.saveTokens(key, fresh)
      fresh
    }
  }
}
