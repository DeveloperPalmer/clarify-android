package ru.sla.clarify.feature.chat.routing.di

import com.squareup.anvil.annotations.MergeSubcomponent
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.domain.di.ChatScope

@SingleIn(ChatScope::class)
@MergeSubcomponent(ChatScope::class)
interface ChatFlowComponent {
  fun nodeFactory(): ChatFlowNodeFactory
}
