package ru.sla.clarify.feature.login.domain

import ru.sla.clarify.auth.session.domain.AuthSessionRepository
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import javax.inject.Inject

@SingleIn(LoginScope::class)
class LoginModel @Inject constructor(
  private val loginRepository: LoginRepository,
  private val authSessionRepository: AuthSessionRepository
) : ReactiveModel() {

  val signIn = task<Unit>(name = "signIn") {
    val result = loginRepository.signIn()
    authSessionRepository.startNew(
      userId = result.userId,
      tokens = result.tokens
    )
  }
}
