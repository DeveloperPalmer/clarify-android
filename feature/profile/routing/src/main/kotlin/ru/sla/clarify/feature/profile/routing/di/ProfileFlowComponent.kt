package ru.sla.clarify.feature.profile.routing.di

import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.profile.domain.di.ProfileScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(ProfileScope::class)
@ContributesSubcomponent(ProfileScope::class)
interface ProfileFlowComponent {
  val nodeFactory: ProfileFlowNodeFactory

  @Provides
  fun provideProfileFlowComponent(): ProfileFlowComponent = this

  @ContributesSubcomponent.Factory(ConversationScope::class)
  interface Factory {
    fun createProfileFlowComponent(): ProfileFlowComponent
  }
}
