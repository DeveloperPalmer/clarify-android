package ru.sla.clarify.feature.debug.panel.ui.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.debug.panel.domain.di.DebugPanelScope
import ru.sla.clarify.feature.debug.panel.ui.screen.main.DebugPanelScreen
import ru.sla.clarify.feature.debug.panel.ui.screen.main.DebugPanelViewModel
import javax.inject.Qualifier

@Module
@ContributesTo(DebugPanelScope::class)
object DebugPanelUiModule {
  @Provides
  @WiredScreen(Screen.Main)
  fun provideMainScreen(model: DebugPanelViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { DebugPanelScreen(viewModel = it) }
  }
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

// Все экраны, поставляемые этим UI-модулем, перечисляются здесь
enum class Screen {
  Main
}
