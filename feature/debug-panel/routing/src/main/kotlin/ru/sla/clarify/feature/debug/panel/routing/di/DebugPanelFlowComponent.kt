package ru.sla.clarify.feature.debug.panel.routing.di

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.feature.debug.panel.domain.di.DebugPanelScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(DebugPanelScope::class)
@ContributesSubcomponent(DebugPanelScope::class)
interface DebugPanelFlowComponent {
  val nodeFactory: DebugPanelFlowNodeFactory

  @Provides
  fun provideDebugPanelFlowComponent(): DebugPanelFlowComponent = this

  @Provides
  @SingleIn(DebugPanelScope::class)
  @ForScope(DebugPanelScope::class)
  fun provideCoroutineScope(@ForScope(AppScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }

  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    fun createDebugPanelFlowComponent(eventSink: FlowEventSink): DebugPanelFlowComponent
  }
}
