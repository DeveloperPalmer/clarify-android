package ru.sla.clarify.google.authenticator

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import ru.sla.clarify.app.domain.buildconfig.BuildConfigProvider
import ru.sla.clarify.app.domain.buildconfig.BuildType
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.core.domain.di.scope.ActivityContext
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.log.log
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SingleIn(AppFlowScope::class)
class GoogleAuthenticator @Inject constructor(
  @ActivityContext
  private val context: Activity,
  private val buildConfig: BuildConfigProvider
) {

  private val credentialManager = CredentialManager.create(context)
  private val firebaseAuth = Firebase.auth

  @Suppress("TooGenericExceptionCaught")
  suspend fun auth(): SignInResult {
    return try {
      SignInResult.Success(signInInternal(buildConfig.buildType.googleClientId))
    } catch (_: GetCredentialCancellationException) {
      SignInResult.CancelledByUser
    } catch (e: CancellationException) {
      throw e
    } catch (exception: Exception) {
      SignInResult.Error(exception)
    } finally {
      firebaseAuth.signOut()
    }
  }

  suspend fun signOut() {
    firebaseAuth.signOut()
    try {
      val clearRequest = ClearCredentialStateRequest()
      credentialManager.clearCredentialState(clearRequest)
    } catch (e: ClearCredentialException) {
      log { "Couldn't clear user credentials: ${e.localizedMessage}" }
      throw e
    }
  }

  private suspend fun signInInternal(clientId: String): AuthResult {
    val googleAuthResult = runCatching {
      val signInWithGoogle = GetSignInWithGoogleOption.Builder(clientId)
        .build()

      val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInWithGoogle)
        .build()
      credentialManager.getCredential(
        context = context,
        request = request
      )
    }.getOrElse { e ->
      if (e is GetCredentialCancellationException) {
        throw e
      } else {
        credentialManager.getCredential(
          request = createGoogleIdRequest(clientId),
          context = context
        )
      }
    }

    if (googleAuthResult.credential is CustomCredential) {
      val googleId = GoogleIdTokenCredential.createFrom(googleAuthResult.credential.data)
      val credential = GoogleAuthProvider.getCredential(googleId.idToken, null)

      val authResult = firebaseAuth.signInWithCredential(credential).await()
      if (authResult.user?.isAnonymous == true) error("Randomly generated e-mail")
      return authResult
    } else {
      throw GoogleIdTokenParsingException()
    }
  }

  private fun createGoogleIdRequest(clientId: String): GetCredentialRequest {
    val googleId = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(false)
      .setServerClientId(clientId)
      .build()

    return GetCredentialRequest.Builder().addCredentialOption(googleId).build()
  }
}

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
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

private val BuildType.googleClientId: String
  get() = when (this) {
    BuildType.Dev,
    BuildType.Internal -> "875404594316-80e040u6aujsvmja5ermohge93bj5qvo.apps.googleusercontent.com"
    BuildType.Release -> "875404594316-80e040u6aujsvmja5ermohge93bj5qvo.apps.googleusercontent.com"
  }

sealed interface SignInResult {
  data object CancelledByUser : SignInResult
  data class Success(val authResult: AuthResult) : SignInResult
  data class Error(val error: Throwable) : SignInResult
}
