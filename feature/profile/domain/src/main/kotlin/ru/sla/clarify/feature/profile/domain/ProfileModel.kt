package ru.sla.clarify.feature.profile.domain

import ru.sla.clarify.auth.session.domain.AuthSessionRepository
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.profile.domain.di.ProfileScope
import ru.sla.clarify.lib.google.authenticator.GoogleAuthenticator
import javax.inject.Inject

@SingleIn(ProfileScope::class)
class ProfileModel @Inject constructor(
  private val googleAuthenticator: GoogleAuthenticator,
  private val authSessionRepository: AuthSessionRepository
) : ReactiveModel() {

  val signOut = task<Unit>(name = "signOut") {
    googleAuthenticator.signOut()
    authSessionRepository.reset(cleanupStorage = true)
  }
}
