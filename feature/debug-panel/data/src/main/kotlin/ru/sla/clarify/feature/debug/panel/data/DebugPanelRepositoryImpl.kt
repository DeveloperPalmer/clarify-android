package ru.sla.clarify.feature.debug.panel.data

import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.chat.api.UserApi
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.toggle.AppFeature
import ru.sla.clarify.feature.debug.panel.data.config.DebugConfig
import ru.sla.clarify.feature.debug.panel.domain.DebugPanelRepository
import ru.sla.clarify.feature.debug.panel.domain.di.DebugPanelScope
import ru.sla.clarify.feature.debug.panel.domain.entity.DebugUserException
import ru.sla.clarify.feature.debug.panel.domain.entity.UserJsonError
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(DebugPanelScope::class)
@ContributesBinding(DebugPanelScope::class)
class DebugPanelRepositoryImpl @Inject constructor(
  private val userApi: UserApi,
  private val debugConfig: DebugConfig
) : DebugPanelRepository {

  override suspend fun setFeatureToggle(feature: AppFeature, isEnabled: Boolean) {
    debugConfig.setFeatureValue(feature.key, isEnabled.toString())
  }

  override suspend fun createUser(user: User) {
    if (userApi.readUserExistsByEmail(user.email)) {
      throw DebugUserException(UserJsonError.UserAlreadyExist)
    }
    userApi.createUser(
      id = user.id,
      email = user.email.value,
      displayName = user.displayName,
      photoUrl = user.photoUrl
    )
  }
}
