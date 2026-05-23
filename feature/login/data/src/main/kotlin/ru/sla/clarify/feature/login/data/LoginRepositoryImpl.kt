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
import ru.sla.clarify.lib.google.authenticator.GoogleAuthenticator
import ru.sla.clarify.lib.google.authenticator.SignInResult
import ru.sla.clarify.lib.google.firestore.Firestore
import javax.inject.Inject

@SingleIn(LoginScope::class)
@ContributesBinding(LoginScope::class)
class LoginRepositoryImpl @Inject constructor(
  private val firestore: Firestore,
  private val googleAuthenticator: GoogleAuthenticator
) : LoginRepository {

  override suspend fun signIn(): AuthResult {
    return when (val signInResult = googleAuthenticator.auth()) {
      is SignInResult.Success -> {
        val user = signInResult.authResult
          .user
          ?: error("FirebaseAuth returned null user after Google sign-in")

        val firebaseIdToken = user.getIdToken(true)
          .await()
          .token
          ?: error("FirebaseAuth returned null ID token after Google sign-in")

        val userId = UserId(user.uid)

        if (firestore.isUserExists(userId)) {
          firestore.patchUser(
            id = userId,
            displayName = user.displayName,
            photoUrl = user.photoUrl?.toString(),
            email = user.email?.lowercase()
          )
        } else {
          firestore.postUser(
            id = userId,
            displayName = user.displayName,
            photoUrl = user.photoUrl?.toString(),
            email = user.email?.lowercase()
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
      is SignInResult.Error -> {
        throw signInResult.error
      }
      is SignInResult.CancelledByUser -> {
        error("cancel by user")
      }
    }
  }
}
