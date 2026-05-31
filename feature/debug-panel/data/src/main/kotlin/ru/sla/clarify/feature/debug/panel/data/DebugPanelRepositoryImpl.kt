package ru.sla.clarify.feature.debug.panel.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.feature.debug.panel.domain.DebugPanelRepository
import ru.sla.clarify.feature.debug.panel.domain.di.DebugPanelScope
import ru.sla.clarify.feature.debug.panel.domain.entity.DebugUserException
import ru.sla.clarify.feature.debug.panel.domain.entity.UserJsonError
import ru.sla.clarify.lib.google.firestore.Firestore
import javax.inject.Inject

@SingleIn(DebugPanelScope::class)
@ContributesBinding(DebugPanelScope::class)
class DebugPanelRepositoryImpl @Inject constructor(
  private val firestore: Firestore
) : DebugPanelRepository {

  override suspend fun createUser(user: User) = withContext(Dispatchers.IO) {
    if (firestore.isUserExistsByEmail(user.email)) {
      throw DebugUserException(UserJsonError.UserAlreadyExist)
    }
    firestore.postUser(
      id = user.id,
      email = user.email.value,
      displayName = user.displayName,
      photoUrl = user.photoUrl
    )
  }
}
