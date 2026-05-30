package ru.sla.clarify.feature.debug.panel.domain

import ru.sla.clarify.core.domain.entity.User

interface DebugPanelRepository {
  suspend fun createUser(user: User)
}
