package ru.sla.clarify.auth.session.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.domain.entity.AuthSessionState
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.entity.ApiError
import ru.sla.clarify.core.domain.entity.ConnectivityError
import ru.sla.clarify.core.domain.logError
import ru.sla.log.asLog
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
class AuthSessionModel @Inject constructor(
  private val sessionRepository: AuthSessionRepository
) : ReactiveModel() {

  val reset = task<Unit> {
    try {
      sessionRepository.reset(cleanupStorage = true)
    } catch (e: ApiError) {
      logError { e.asLog() }
    } catch (e: ConnectivityError) {
      logError { e.asLog() }
    }
  }

  val sessionState: Flow<AuthSessionState> = sessionRepository.key().flatMapLatest { key ->
    if (key == null) {
      flowOf(AuthSessionState.Inactive)
    } else {
      sessionRepository
        .tokens(key)
        .map { if (it != null) AuthSessionState.Active else AuthSessionState.Inactive }
    }
  }
}
