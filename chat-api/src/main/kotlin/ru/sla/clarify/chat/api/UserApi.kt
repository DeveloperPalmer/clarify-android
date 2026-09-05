package ru.sla.clarify.chat.api

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId

/**
 * Пользователи: профиль, поиск по email, регистрация.
 *
 * Тема из `wiki/method/`: интерфейс отделён от остальных, потому что им не нужно знать
 * друг о друге, и это же деление даёт имя операции в спеке.
 */
interface UserApi {

  suspend fun readCurrentUser(): User

  suspend fun readUser(id: UserId): User?

  suspend fun readUserExists(id: UserId): Boolean

  suspend fun readUserExistsByEmail(email: Email): Boolean

  suspend fun readUserIdByEmail(email: Email): UserId?

  suspend fun readUsersByEmailPrefix(prefix: String, limit: Long): List<User>

  fun userLive(id: UserId): Flow<User?>

  suspend fun updateUser(
    id: UserId,
    email: String,
    displayName: String,
    photoUrl: String?
  )

  suspend fun createUser(
    id: UserId,
    email: String,
    displayName: String,
    photoUrl: String?
  )
}
