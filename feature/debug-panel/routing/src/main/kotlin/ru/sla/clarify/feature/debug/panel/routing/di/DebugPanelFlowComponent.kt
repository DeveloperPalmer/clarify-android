package ru.sla.clarify.feature.debug.panel.routing.di

import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.feature.debug.panel.domain.di.DebugPanelScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(DebugPanelScope::class)
@ContributesSubcomponent(DebugPanelScope::class)
interface DebugPanelFlowComponent {
  val nodeFactory: DebugPanelFlowNodeFactory

  @Provides
  fun provideDebugPanelFlowComponent(): DebugPanelFlowComponent = this

  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    fun createDebugPanelFlowComponent(eventSink: FlowEventSink): DebugPanelFlowComponent
  }
}
