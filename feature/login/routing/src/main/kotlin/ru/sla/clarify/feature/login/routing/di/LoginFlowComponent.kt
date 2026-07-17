package ru.sla.clarify.feature.login.routing.di

import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.feature.login.domain.LoginScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(LoginScope::class)
@ContributesSubcomponent(LoginScope::class)
interface LoginFlowComponent {
  val nodeFactory: LoginFlowNodeFactory

  @Provides
  fun provideLoginFlowComponent(): LoginFlowComponent = this

  @ContributesSubcomponent.Factory(AppFlowScope::class)
  interface Factory {
    fun createLoginFlowComponent(): LoginFlowComponent
  }
}
