package ru.sla.clarify.app.routing.di

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.ActivityContext
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.event.ViewEventsHostMediator
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppFlowScope::class)
@ContributesSubcomponent(AppFlowScope::class)
interface AppFlowComponent {
  val nodeFactory: AppFlowNodeFactory
  val viewEventsHostMediator: ViewEventsHostMediator

  @Provides
  fun provideAppFlowComponent(): AppFlowComponent = this

  @Provides
  @SingleIn(AppFlowScope::class)
  @ForScope(AppFlowScope::class)
  fun provideCoroutineScope(@ForScope(AppScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }

  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    fun createAppFlowComponent(
      @ActivityContext
      activity: Activity,
      eventSink: FlowEventSink
    ): AppFlowComponent
  }
}
