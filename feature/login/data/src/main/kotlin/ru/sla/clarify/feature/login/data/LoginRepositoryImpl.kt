package ru.sla.clarify.feature.login.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.tasks.await
import ru.sla.clarify.auth.session.domain.entity.AccessToken
import ru.sla.clarify.auth.session.domain.entity.AuthTokens
import ru.sla.clarify.auth.session.domain.entity.RefreshToken
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.feature.login.domain.LoginRepository
import ru.sla.clarify.feature.login.domain.LoginScope
import ru.sla.clarify.feature.login.entity.AuthResult
import ru.sla.clarify.feature.login.entity.GoogleAuthError
import ru.sla.clarify.lib.google.authenticator.GoogleAuthenticator
import ru.sla.clarify.lib.google.firestore.Firestore
import javax.inject.Inject
import ru.sla.clarify.lib.google.authenticator.SignInResult as GoogleSignInResult

@SingleIn(LoginScope::class)
@ContributesBinding(LoginScope::class)
class LoginRepositoryImpl @Inject constructor(
  private val firestore: Firestore,
  private val googleAuthenticator: GoogleAuthenticator
) : LoginRepository {

  override suspend fun signInByGoogle(): AuthResult {
    return when (val signInResult = googleAuthenticator.auth()) {
      is GoogleSignInResult.Success -> {
        val user = signInResult.authResult
          .user
          ?: error("FirebaseAuth returned null user after Google sign-in")

        val firebaseIdToken = user.getIdToken(true)
          .await()
          .token
          ?: error("FirebaseAuth returned null ID token after Google sign-in")

        val userId = UserId(user.uid)

        val email = user.email?.lowercase()
          ?: error("FirebaseAuth returned null email after Google sign-in")

        val displayName = user.displayName
          ?: error("FirebaseAuth returned null displayName after Google sign-in")

        if (firestore.isUserExists(userId)) {
          firestore.patchUser(
            id = userId,
            email = email,
            displayName = displayName,
            photoUrl = user.photoUrl?.toString()
          )
        } else {
          firestore.postUser(
            id = userId,
            email = email,
            displayName = displayName,
            photoUrl = user.photoUrl?.toString()
          )
        }

        AuthResult(
          userId = userId,
          tokens = AuthTokens(
            updatedAt = System.currentTimeMillis(),
            accessToken = AccessToken(firebaseIdToken),
            refreshToken = RefreshToken(firebaseIdToken)
          )
        )
      }
      is GoogleSignInResult.Error -> {
        throw GoogleAuthError.Authentication(signInResult.error)
      }
      is GoogleSignInResult.CancelledByUser -> {
        throw GoogleAuthError.CancelledByUser(cause = null)
      }
    }
  }
}
