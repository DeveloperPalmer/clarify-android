package ru.sla.clarify.feature.chat.group.thread.routing.di

import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.group.thread.domain.di.GroupThreadScope
import ru.sla.clarify.feature.chat.group.thread.domain.entity.TargetParams
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(GroupThreadScope::class)
@ContributesSubcomponent(GroupThreadScope::class)
interface GroupThreadFlowComponent {
  val nodeFactory: GroupThreadFlowNodeFactory

  @Provides
  fun provideGroupThreadFlowComponent(): GroupThreadFlowComponent = this

  @ContributesSubcomponent.Factory(ConversationScope::class)
  interface Factory {
    fun createGroupThreadFlowComponent(params: TargetParams): GroupThreadFlowComponent
  }
}
