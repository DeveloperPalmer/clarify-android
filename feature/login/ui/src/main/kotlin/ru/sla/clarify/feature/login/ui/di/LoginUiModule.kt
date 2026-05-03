package ru.sla.clarify.feature.login.ui.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.login.domain.LoginScope
import ru.sla.clarify.feature.login.ui.screen.splashintro.SplashIntroScreen
import ru.sla.clarify.feature.login.ui.screen.splashintro.SplashIntroViewModel
import javax.inject.Qualifier

@Module
@ContributesTo(LoginScope::class)
object LoginUiModule {
  @Provides
  @WiredScreen(Screen.SplashIntro)
  fun provideSplashIntroScreen(model: SplashIntroViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { SplashIntroScreen(viewModel = it) }
  }
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

enum class Screen {
  SplashIntro
}
