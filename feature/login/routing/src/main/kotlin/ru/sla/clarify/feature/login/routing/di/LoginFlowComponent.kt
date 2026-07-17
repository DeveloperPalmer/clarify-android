package ru.sla.clarify.feature.login.routing.di

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.feature.login.domain.LoginScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(LoginScope::class)
@ContributesSubcomponent(LoginScope::class)
interface LoginFlowComponent {
  val nodeFactory: LoginFlowNodeFactory

  @Provides
  fun provideLoginFlowComponent(): LoginFlowComponent = this

  @Provides
  @SingleIn(LoginScope::class)
  @ForScope(LoginScope::class)
  fun provideCoroutineScope(@ForScope(AppFlowScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }

  @ContributesSubcomponent.Factory(AppFlowScope::class)
  interface Factory {
    fun createLoginFlowComponent(): LoginFlowComponent
  }
}
