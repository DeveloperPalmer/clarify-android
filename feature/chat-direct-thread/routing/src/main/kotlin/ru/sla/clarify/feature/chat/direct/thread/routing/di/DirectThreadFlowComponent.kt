package ru.sla.clarify.feature.chat.direct.thread.routing.di

import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.TargetParams
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(DirectThreadScope::class)
@ContributesSubcomponent(DirectThreadScope::class)
interface DirectThreadFlowComponent {
  val nodeFactory: DirectThreadFlowNodeFactory

  @Provides
  fun provideDirectThreadFlowComponent(): DirectThreadFlowComponent = this

  @ContributesSubcomponent.Factory(ConversationScope::class)
  interface Factory {
    fun createDirectThreadFlowComponent(params: TargetParams): DirectThreadFlowComponent
  }
}
