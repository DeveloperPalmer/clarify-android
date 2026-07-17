package ru.sla.clarify.feature.chat.conversation.routing.di

import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(ConversationScope::class)
@ContributesSubcomponent(ConversationScope::class)
interface ConversationFlowComponent {
  val nodeFactory: ConversationFlowNodeFactory

  @Provides
  fun provideConversationFlowComponent(): ConversationFlowComponent = this

  @ContributesSubcomponent.Factory(AppFlowScope::class)
  interface Factory {
    fun createConversationFlowComponent(): ConversationFlowComponent
  }
}
