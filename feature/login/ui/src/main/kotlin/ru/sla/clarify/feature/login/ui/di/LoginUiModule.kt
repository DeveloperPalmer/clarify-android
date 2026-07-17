package ru.sla.clarify.feature.login.ui.di

import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Qualifier
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.login.domain.LoginScope
import ru.sla.clarify.feature.login.ui.screen.credentials.CredentialsScreen
import ru.sla.clarify.feature.login.ui.screen.credentials.CredentialsViewModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesTo(LoginScope::class)
interface LoginUiModule {
  @Provides
  @WiredScreen(Screen.Credentials)
  fun provideCredentialsScreen(model: CredentialsViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { CredentialsScreen(viewModel = it) }
  }
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

enum class Screen {
  Credentials
}
