package ru.sla.clarify.feature.chat.conversation.routing.di

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(ConversationScope::class)
@ContributesSubcomponent(ConversationScope::class)
interface ConversationFlowComponent {
  val nodeFactory: ConversationFlowNodeFactory

  @Provides
  fun provideConversationFlowComponent(): ConversationFlowComponent = this

  @Provides
  @SingleIn(ConversationScope::class)
  @ForScope(ConversationScope::class)
  fun provideCoroutineScope(@ForScope(AppFlowScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }

  @ContributesSubcomponent.Factory(AppFlowScope::class)
  interface Factory {
    fun createConversationFlowComponent(): ConversationFlowComponent
  }
}
