package ru.sla.clarify.feature.debug.panel.routing.di

import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.ForScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.feature.debug.panel.domain.di.DebugPanelScope

@SingleIn(DebugPanelScope::class)
@MergeSubcomponent(DebugPanelScope::class)
interface DebugPanelFlowComponent {
  fun nodeFactory(): DebugPanelFlowNodeFactory

  @MergeSubcomponent.Builder
  interface Builder {
    @BindsInstance
    fun eventSink(sink: FlowEventSink): Builder

    fun build(): DebugPanelFlowComponent
  }
}

@Module
@ContributesTo(DebugPanelScope::class)
object DebugPanelFlowModule {
  @Provides
  @SingleIn(DebugPanelScope::class)
  @ForScope(DebugPanelScope::class)
  fun provideCoroutineScope(@ForScope(AppScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }
}
