package ru.sla.clarify.feature.chronology.routing.di

import com.squareup.anvil.annotations.MergeSubcomponent
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope

@MergeSubcomponent(ChronologyScope::class)
@SingleIn(ChronologyScope::class)
interface ChronologyFlowComponent {
  fun nodeFactory(): ChronologyFlowNodeFactory
}
