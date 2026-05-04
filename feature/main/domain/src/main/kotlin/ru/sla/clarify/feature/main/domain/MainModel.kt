package ru.sla.clarify.feature.main.domain

import ru.sla.clarify.auth.session.domain.AuthSessionRepository
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.main.domain.di.MainScope
import ru.sla.clarify.google.authenticator.GoogleAuthenticator
import javax.inject.Inject

@SingleIn(MainScope::class)
class MainModel @Inject constructor(
  private val googleAuthenticator: GoogleAuthenticator,
  private val authSessionRepository: AuthSessionRepository
) : ReactiveModel() {

  val signOut = task(name = "signOut") { ->
    googleAuthenticator.signOut()
    authSessionRepository.reset(cleanupStorage = true)
  }
}
