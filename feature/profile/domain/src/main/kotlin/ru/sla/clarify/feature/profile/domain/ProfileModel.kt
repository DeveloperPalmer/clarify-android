package ru.sla.clarify.feature.profile.domain

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.domain.AuthSessionRepository
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.feature.profile.domain.di.ProfileScope
import ru.sla.clarify.lib.google.authenticator.GoogleAuthenticator
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(ProfileScope::class)
class ProfileModel @Inject constructor(
  private val googleAuthenticator: GoogleAuthenticator,
  private val authSessionRepository: AuthSessionRepository,
  @ForScope(ProfileScope::class) parentScope: CoroutineScope
) : ReactiveModel(parentScope) {

  val signOut = task<Unit>(name = "signOut") {
    googleAuthenticator.signOut()
    authSessionRepository.reset(cleanupStorage = true)
  }
}
