package ru.sla.clarify.feature.chat.group.thread.routing.di

import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.ForScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.group.thread.data.di.GroupThreadDataModule
import ru.sla.clarify.feature.chat.group.thread.domain.di.GroupThreadScope
import ru.sla.clarify.feature.chat.group.thread.domain.entity.TargetParams
import ru.sla.clarify.feature.chat.group.thread.ui.di.GroupThreadUiModule

@SingleIn(GroupThreadScope::class)
@Subcomponent(
  modules = [
    GroupThreadDataModule::class,
    GroupThreadFlowModule::class,
    GroupThreadUiModule::class
  ]
)
interface GroupThreadFlowComponent {
  fun nodeFactory(): GroupThreadFlowNodeFactory

  @Subcomponent.Builder
  interface Builder {
    @BindsInstance
    fun params(target: TargetParams): Builder

    fun build(): GroupThreadFlowComponent
  }
}

@Module
object GroupThreadFlowModule {
  @Provides
  @SingleIn(GroupThreadScope::class)
  @ForScope(GroupThreadScope::class)
  fun provideCoroutineScope(@ForScope(ConversationScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }
}
