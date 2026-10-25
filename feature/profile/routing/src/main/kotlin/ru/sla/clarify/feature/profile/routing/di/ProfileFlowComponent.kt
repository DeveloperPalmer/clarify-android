package ru.sla.clarify.feature.profile.routing.di

import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.ForScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.profile.domain.di.ProfileScope
import ru.sla.clarify.feature.profile.ui.di.ProfileUiModule

@SingleIn(ProfileScope::class)
@Subcomponent(
  modules = [
    ProfileFlowModule::class,
    ProfileUiModule::class
  ]
)
interface ProfileFlowComponent {
  fun nodeFactory(): ProfileFlowNodeFactory
}

@Module
object ProfileFlowModule {
  @Provides
  @SingleIn(ProfileScope::class)
  @ForScope(ProfileScope::class)
  fun provideCoroutineScope(@ForScope(ConversationScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }
}
