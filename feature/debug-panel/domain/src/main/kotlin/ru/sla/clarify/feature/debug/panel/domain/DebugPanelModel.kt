package ru.sla.clarify.feature.debug.panel.domain

import arrow.core.getOrElse
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.toggle.AppFeature
import ru.sla.clarify.feature.debug.panel.domain.di.DebugPanelScope
import ru.sla.clarify.feature.debug.panel.domain.entity.DebugUserException
import ru.sla.clarify.feature.debug.panel.domain.entity.TestUser
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(DebugPanelScope::class)
class DebugPanelModel @Inject constructor(
  private val repository: DebugPanelRepository,
  @ForScope(DebugPanelScope::class) parentScope: CoroutineScope
) : ReactiveModel(parentScope) {

  val createUser = task<String, Unit>(name = "createUser") { raw ->
    val user = TestUser(raw).getOrElse { throw DebugUserException(it.first()) }
    repository.createUser(user)
  }

  val setFeatureToggle = task<Pair<AppFeature, Boolean>, Unit>(name = "setFeatureToggle") { (feature, isEnabled) ->
    repository.setFeatureToggle(feature, isEnabled)
  }
}
