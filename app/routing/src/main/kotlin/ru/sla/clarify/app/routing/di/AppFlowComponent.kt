package ru.sla.clarify.app.routing.di

import android.app.Activity
import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.core.domain.di.scope.ActivityContext
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.event.ViewEventsHostMediator
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppFlowScope::class)
@ContributesSubcomponent(AppFlowScope::class)
interface AppFlowComponent {
  val nodeFactory: AppFlowNodeFactory
  val viewEventsHostMediator: ViewEventsHostMediator

  @Provides
  fun provideAppFlowComponent(): AppFlowComponent = this

  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    fun createAppFlowComponent(
      @ActivityContext
      activity: Activity,
      eventSink: FlowEventSink
    ): AppFlowComponent
  }
}
