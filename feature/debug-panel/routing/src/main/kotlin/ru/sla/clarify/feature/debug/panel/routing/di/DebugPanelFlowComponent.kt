package ru.sla.clarify.feature.debug.panel.routing.di

import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.ForScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.feature.debug.panel.data.di.DebugPanelDataModule
import ru.sla.clarify.feature.debug.panel.domain.di.DebugPanelScope
import ru.sla.clarify.feature.debug.panel.ui.di.DebugPanelUiModule

@SingleIn(DebugPanelScope::class)
@Subcomponent(
  modules = [
    DebugPanelDataModule::class,
    DebugPanelFlowModule::class,
    DebugPanelUiModule::class
  ]
)
interface DebugPanelFlowComponent {
  fun nodeFactory(): DebugPanelFlowNodeFactory

  @Subcomponent.Builder
  interface Builder {
    @BindsInstance
    fun eventSink(sink: FlowEventSink): Builder

    fun build(): DebugPanelFlowComponent
  }
}

@Module
object DebugPanelFlowModule {
  @Provides
  @SingleIn(DebugPanelScope::class)
  @ForScope(DebugPanelScope::class)
  fun provideCoroutineScope(@ForScope(AppScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }
}
