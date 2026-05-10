package ru.sla.clarify.feature.chat.routing.di

import com.squareup.anvil.annotations.MergeSubcomponent
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.domain.di.ChatScope
import ru.sla.clarify.feature.chronology.routing.di.ChronologyFlowComponent

@SingleIn(ChatScope::class)
@MergeSubcomponent(ChatScope::class)
interface ChatFlowComponent {
  fun nodeFactory(): ChatFlowNodeFactory
  fun chronologyComponent(): ChronologyFlowComponent
}
