package ru.sla.clarify.feature.login.domain

import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import javax.inject.Inject

@SingleIn(LoginScope::class)
class LoginModel @Inject constructor(
  private val loginRepository: LoginRepository
) : ReactiveModel() {

  val signIn = task<Unit>(name = "signIn") {
    loginRepository.signIn()
  }
}
