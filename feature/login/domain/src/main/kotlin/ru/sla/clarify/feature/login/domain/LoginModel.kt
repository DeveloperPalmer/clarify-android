package ru.sla.clarify.feature.login.domain

import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.domain.AuthSessionRepository
import ru.sla.clarify.core.domain.ReactiveModel
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(LoginScope::class)
class LoginModel @Inject constructor(
  private val loginRepository: LoginRepository,
  private val authSessionRepository: AuthSessionRepository
) : ReactiveModel() {

  val signInByGoogle = task<Unit>(name = "signInByGoogle") {
    val result = loginRepository.signInByGoogle()
    authSessionRepository.startNew(
      userId = result.userId,
      tokens = result.tokens
    )
  }
}
