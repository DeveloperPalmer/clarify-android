package ru.sla.clarify.feature.profile.ui.di

import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Qualifier
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.profile.domain.di.ProfileScope
import ru.sla.clarify.feature.profile.ui.screen.main.ProfileScreen
import ru.sla.clarify.feature.profile.ui.screen.main.ProfileViewModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesTo(ProfileScope::class)
interface ProfileUiModule {
  @Provides
  @WiredScreen(Screen.Main)
  fun provideMainScreen(model: ProfileViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { ProfileScreen(viewModel = it) }
  }
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

// Все экраны, поставляемые этим UI-модулем, перечисляются здесь
enum class Screen {
  Main
}
