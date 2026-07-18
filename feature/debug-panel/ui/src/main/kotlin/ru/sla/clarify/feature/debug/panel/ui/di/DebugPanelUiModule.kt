package ru.sla.clarify.feature.debug.panel.ui.di

import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Qualifier
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.debug.panel.domain.di.DebugPanelScope
import ru.sla.clarify.feature.debug.panel.ui.screen.featuretoggles.FeatureTogglesScreen
import ru.sla.clarify.feature.debug.panel.ui.screen.featuretoggles.FeatureTogglesViewModel
import ru.sla.clarify.feature.debug.panel.ui.screen.main.DebugPanelScreen
import ru.sla.clarify.feature.debug.panel.ui.screen.main.DebugPanelViewModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesTo(DebugPanelScope::class)
interface DebugPanelUiModule {
  @Provides
  @WiredScreen(Screen.Main)
  fun provideMainScreen(model: DebugPanelViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { DebugPanelScreen(viewModel = it) }
  }

  @Provides
  @WiredScreen(Screen.FeatureToggles)
  fun provideFeatureTogglesScreen(model: FeatureTogglesViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { FeatureTogglesScreen(viewModel = it) }
  }
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

// Все экраны, поставляемые этим UI-модулем, перечисляются здесь
enum class Screen {
  Main,
  FeatureToggles
}
