package ru.sla.clarify.feature.chronology.ui.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope
import ru.sla.clarify.feature.chronology.ui.screen.chronology.ChronologyScreen
import ru.sla.clarify.feature.chronology.ui.screen.chronology.ChronologyViewModel
import javax.inject.Qualifier

@Module
@ContributesTo(ChronologyScope::class)
object ChronologyUiModule {
  @Provides
  @WiredScreen(Screen.Chronology)
  fun provideChronologyScreen(model: ChronologyViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { ChronologyScreen(viewModel = it) }
  }
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

enum class Screen {
  Chronology
}
