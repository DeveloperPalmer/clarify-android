package ru.sla.clarify.feature.chronology.routing.di

import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.ForScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope
import ru.sla.clarify.feature.chronology.domain.entity.TargetParams as ChronologyTargetParams

@SingleIn(ChronologyScope::class)
@MergeSubcomponent(ChronologyScope::class)
interface ChronologyFlowComponent {
  fun nodeFactory(): ChronologyFlowNodeFactory

  @MergeSubcomponent.Builder
  interface Builder {
    @BindsInstance
    fun params(params: ChronologyTargetParams): Builder

    fun build(): ChronologyFlowComponent
  }
}

@Module
@ContributesTo(ChronologyScope::class)
object ChronologyFlowModule {
  @Provides
  @SingleIn(ChronologyScope::class)
  @ForScope(ChronologyScope::class)
  fun provideCoroutineScope(@ForScope(DirectThreadScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }
}
