package ru.sla.clarify.feature.main.domain

import ru.sla.clarify.auth.session.domain.AuthSessionRepository
import ru.sla.clarify.chat.ChatManager
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.main.domain.di.MainScope
import ru.sla.clarify.google.authenticator.GoogleAuthenticator
import javax.inject.Inject

@SingleIn(MainScope::class)
class MainModel @Inject constructor(
  private val chatManager: ChatManager,
  private val mainRepository: MainRepository,
  private val googleAuthenticator: GoogleAuthenticator,
  private val authSessionRepository: AuthSessionRepository
) : ReactiveModel() {

  val chatSignIn = task<Unit>(name = "chatSignIn") {
    mainRepository.fetchUserDetails(skipCache = true)

    val userDetails = mainRepository.getUserDetails()

    chatManager.signIn(
      chatSignature = userDetails.chatSignature
    )
  }

  val signOut = task<Unit>(name = "signOut") {
    chatManager.signOut()
    googleAuthenticator.signOut()
    authSessionRepository.reset(cleanupStorage = true)
  }
}
