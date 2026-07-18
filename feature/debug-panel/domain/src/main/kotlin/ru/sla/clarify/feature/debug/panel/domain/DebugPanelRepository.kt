package ru.sla.clarify.feature.debug.panel.domain

import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.toggle.AppFeature

interface DebugPanelRepository {
  suspend fun createUser(user: User)
  suspend fun setFeatureToggle(feature: AppFeature, isEnabled: Boolean)
}
