package ru.sla.clarify.feature.debug.panel.domain

import arrow.core.getOrElse
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.debug.panel.domain.di.DebugPanelScope
import ru.sla.clarify.feature.debug.panel.domain.entity.DebugUserException
import ru.sla.clarify.feature.debug.panel.domain.entity.TestUser
import javax.inject.Inject

@SingleIn(DebugPanelScope::class)
class DebugPanelModel @Inject constructor(
  private val repository: DebugPanelRepository
) : ReactiveModel() {

  val createUser = task<String, Unit>(name = "createUser") { raw ->
    val user = TestUser(raw).getOrElse { throw DebugUserException(it.first()) }
    repository.createUser(user)
  }
}
