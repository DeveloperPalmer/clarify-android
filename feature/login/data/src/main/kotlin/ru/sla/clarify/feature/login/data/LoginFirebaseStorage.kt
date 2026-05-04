package ru.sla.clarify.feature.login.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import javax.inject.Inject

@SingleIn(AppScope::class)
class LoginFirebaseStorage @Inject constructor() {

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
  suspend fun registerUser(
    userId: Int,
    chatSignature: String
  ) {
    val users = remoteDB.collection(USERS_COLLECTIONS)

    val alreadyExists = users
      .whereEqualTo(USERS_FIELD_ID, userId)
      .limit(1)
      .await<QuerySnapshot>()
      .documents
      .isNotEmpty()

    if (alreadyExists) return

    val params = mapOf(
      USERS_FIELD_ID to userId,
      USERS_FIELD_CHAT_SIGNATURE to chatSignature,
    )

    users
      .add(params)
      .await()
  }
}

private suspend fun <T> Query.await(): T {
  return get().await() as T
}

private const val USERS_COLLECTIONS = "users"
private const val USERS_FIELD_ID = "id"
private const val USERS_FIELD_CHAT_SIGNATURE = "chatSignature"
