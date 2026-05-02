package ru.sla.clarify.feature.main.routing.di

import com.squareup.anvil.annotations.MergeSubcomponent
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.main.domain.di.MainScope

@MergeSubcomponent(MainScope::class)
@SingleIn(MainScope::class)
interface MainFlowComponent {
  fun nodeFactory(): MainFlowNodeFactory
}
