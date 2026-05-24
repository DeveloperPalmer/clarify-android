package ru.sla.clarify.feature.login.ui.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.login.domain.LoginScope
import ru.sla.clarify.feature.login.ui.screen.credentials.CredentialsScreen
import ru.sla.clarify.feature.login.ui.screen.credentials.CredentialsViewModel
import javax.inject.Qualifier

@Module
@ContributesTo(LoginScope::class)
object LoginUiModule {
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
