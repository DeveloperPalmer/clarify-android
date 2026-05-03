package ru.sla.clarify.feature.login.routing.di

import com.squareup.anvil.annotations.MergeSubcomponent
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.login.domain.LoginScope

@SingleIn(LoginScope::class)
@MergeSubcomponent(LoginScope::class)
interface LoginFlowComponent {
  fun nodeFactory(): LoginFlowNodeFactory
}
