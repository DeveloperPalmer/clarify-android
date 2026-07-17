package ru.sla.clarify.feature.profile.routing.di

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.profile.domain.di.ProfileScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(ProfileScope::class)
@ContributesSubcomponent(ProfileScope::class)
interface ProfileFlowComponent {
  val nodeFactory: ProfileFlowNodeFactory

  @Provides
  fun provideProfileFlowComponent(): ProfileFlowComponent = this

  @Provides
  @SingleIn(ProfileScope::class)
  @ForScope(ProfileScope::class)
  fun provideCoroutineScope(@ForScope(ConversationScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }

  @ContributesSubcomponent.Factory(ConversationScope::class)
  interface Factory {
    fun createProfileFlowComponent(): ProfileFlowComponent
  }
}
