package ru.sla.clarify.feature.chat.conversation.routing.di

import com.squareup.anvil.annotations.MergeSubcomponent
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.direct.thread.routing.di.ThreadFlowComponent
import ru.sla.clarify.feature.chat.group.thread.routing.di.GroupThreadFlowComponent
import ru.sla.clarify.feature.profile.routing.di.ProfileFlowComponent

@SingleIn(ConversationScope::class)
@MergeSubcomponent(ConversationScope::class)
interface ConversationFlowComponent {
  fun nodeFactory(): ConversationFlowNodeFactory
  fun profileFlowComponent(): ProfileFlowComponent
  fun directThreadFlowComponent(): ThreadFlowComponent.Builder
  fun groupThreadFlowComponent(): GroupThreadFlowComponent.Builder
}
