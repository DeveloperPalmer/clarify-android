package ru.sla.clarify.feature.chat.direct.thread.routing.di

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.TargetParams
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(DirectThreadScope::class)
@ContributesSubcomponent(DirectThreadScope::class)
interface DirectThreadFlowComponent {
  val nodeFactory: DirectThreadFlowNodeFactory

  @Provides
  fun provideDirectThreadFlowComponent(): DirectThreadFlowComponent = this

  @Provides
  @SingleIn(DirectThreadScope::class)
  @ForScope(DirectThreadScope::class)
  fun provideCoroutineScope(@ForScope(ConversationScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }

  @ContributesSubcomponent.Factory(ConversationScope::class)
  interface Factory {
    fun createDirectThreadFlowComponent(params: TargetParams): DirectThreadFlowComponent
  }
}
