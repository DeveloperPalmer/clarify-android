package ru.sla.clarify.feature.login.data

import com.google.android.gms.tasks.Task
import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.sla.clarify.auth.session.domain.AuthSessionRepository
import ru.sla.clarify.auth.session.domain.entity.AccessToken
import ru.sla.clarify.auth.session.domain.entity.AuthTokens
import ru.sla.clarify.auth.session.domain.entity.RefreshToken
import ru.sla.clarify.auth.session.domain.entity.UserId
import ru.sla.clarify.feature.login.domain.LoginRepository
import ru.sla.clarify.feature.login.domain.LoginScope
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

@ContributesBinding(LoginScope::class)
class LoginRepositoryImpl @Inject constructor(
  private val googleAuthenticator: GoogleAuthenticator,
  private val authSessionRepository: AuthSessionRepository
) : LoginRepository {

  override suspend fun signIn() {
    when (val signInResult = googleAuthenticator.auth()) {
      is SignInResult.Success -> {
        val user = signInResult.authResult
          .user
          ?: error("FirebaseAuth returned null user after Google sign-in")

        val firebaseIdToken = user.getIdToken(true)
          .await()
          .token
          ?: error("FirebaseAuth returned null ID token after Google sign-in")

        // Temporary token mapping until backend-issued auth tokens are introduced.
        authSessionRepository.startNew(
          userId = UserId(user.uid.stableUserIdValue()),
          tokens = AuthTokens(
            accessToken = AccessToken(firebaseIdToken),
            refreshToken = RefreshToken(firebaseIdToken),
            updatedAt = System.currentTimeMillis()
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

  private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
      val exception = task.exception
      when {
        task.isSuccessful -> {
          continuation.resume(task.result)
        }
        exception != null -> {
          continuation.resumeWithException(exception)
        }
        else -> {
          continuation.resumeWithException(IllegalStateException("Task failed without exception"))
        }
      }
    }
  }

  private fun String.stableUserIdValue(): Int {
    val hash = hashCode()
    return if (hash == Int.MIN_VALUE) 0 else abs(hash)
  }
}
