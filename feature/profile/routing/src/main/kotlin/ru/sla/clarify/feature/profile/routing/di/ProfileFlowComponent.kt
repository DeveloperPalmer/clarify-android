package ru.sla.clarify.feature.profile.routing.di

import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.ForScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.profile.domain.di.ProfileScope

@SingleIn(ProfileScope::class)
@MergeSubcomponent(ProfileScope::class)
interface ProfileFlowComponent {
  fun nodeFactory(): ProfileFlowNodeFactory
}

@ContributesTo(ProfileScope::class)
@Module
object ProfileFlowModule {
  @Provides
  @SingleIn(ProfileScope::class)
  @ForScope(ProfileScope::class)
  fun provideCoroutineScope(@ForScope(ConversationScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }
}
