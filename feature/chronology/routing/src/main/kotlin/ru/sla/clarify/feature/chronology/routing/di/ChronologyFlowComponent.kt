package ru.sla.clarify.feature.chronology.routing.di

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import ru.sla.clarify.feature.chronology.domain.entity.TargetParams as ChronologyTargetParams

@SingleIn(ChronologyScope::class)
@ContributesSubcomponent(ChronologyScope::class)
interface ChronologyFlowComponent {
  val nodeFactory: ChronologyFlowNodeFactory

  @Provides
  fun provideChronologyFlowComponent(): ChronologyFlowComponent = this

  @Provides
  @SingleIn(ChronologyScope::class)
  @ForScope(ChronologyScope::class)
  fun provideCoroutineScope(@ForScope(DirectThreadScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }

  @ContributesSubcomponent.Factory(DirectThreadScope::class)
  interface Factory {
    fun createChronologyFlowComponent(params: ChronologyTargetParams): ChronologyFlowComponent
  }
}
