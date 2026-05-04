package ru.sla.clarify.database.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import ru.sla.clarify.auth.session.domain.AuthSessionRepository
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.database.UserDetails
import javax.inject.Inject

@SingleIn(AppScope::class)
class Firestore @Inject constructor(
  private val authSessionRepository: AuthSessionRepository
) {

  private val remoteDB = FirebaseFirestore.getInstance().apply {
    firestoreSettings = FirebaseFirestoreSettings.Builder()
      .build()
  }

  /**
   * Создаёт документ текущего пользователя в коллекции [USERS_COLLECTIONS] и заполняет
   * в нём поле [USERS_FIELD_ID]. Если документ для пользователя уже существует —
   * ничего не делает (идемпотентно).
   *
   * @throws IllegalStateException если активная сессия отсутствует.
   */
  suspend fun signIn(
    userId: Int,
    chatSignature: String
  ) = withContext(Dispatchers.IO) {
    val params = mapOf(
      USERS_FIELD_CHAT_SIGNATURE to chatSignature
    )

    remoteDB.collection(USERS_COLLECTIONS)
      .document("$userId")
      .set(params)
      .await()
  }

  suspend fun getUserDetails(): UserDetails? = withContext(Dispatchers.IO) {
    val userId = requireNotNull(authSessionRepository.withKey { readUserId(it) }) {
      "userId not found in persistence"
    }
    val document = remoteDB.collection(USERS_COLLECTIONS)
      .document("${userId.value}")
      .get()
      .await()

    UserDetails(
      key = 0,
      chatSignature = requireNotNull(document.getString("chatSignature"))
    )
  }

  companion object {
    const val GET_USER_DETAILS_CACHE_KEY = "GET_USER_DETAILS_CACHE_KEY"
  }
}

private const val USERS_COLLECTIONS = "users"
private const val USERS_FIELD_CHAT_SIGNATURE = "chatSignature"
