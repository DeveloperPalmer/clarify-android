package ru.sla.clarify.feature.chat.group.thread.routing.di

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.group.thread.domain.di.GroupThreadScope
import ru.sla.clarify.feature.chat.group.thread.domain.entity.TargetParams
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(GroupThreadScope::class)
@ContributesSubcomponent(GroupThreadScope::class)
interface GroupThreadFlowComponent {
  val nodeFactory: GroupThreadFlowNodeFactory

  @Provides
  fun provideGroupThreadFlowComponent(): GroupThreadFlowComponent = this

  @Provides
  @SingleIn(GroupThreadScope::class)
  @ForScope(GroupThreadScope::class)
  fun provideCoroutineScope(@ForScope(ConversationScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }

  @ContributesSubcomponent.Factory(ConversationScope::class)
  interface Factory {
    fun createGroupThreadFlowComponent(params: TargetParams): GroupThreadFlowComponent
  }
}
