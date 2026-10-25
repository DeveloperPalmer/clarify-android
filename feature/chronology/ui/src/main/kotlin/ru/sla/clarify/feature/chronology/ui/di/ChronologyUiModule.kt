package ru.sla.clarify.feature.chronology.ui.di

import dagger.Module
import dagger.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chronology.ui.screen.chronology.ChronologyScreen
import ru.sla.clarify.feature.chronology.ui.screen.chronology.ChronologyViewModel

@Module
object ChronologyUiModule {
  @Provides
  fun provideChronologyScreen(model: ChronologyViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { ChronologyScreen(viewModel = it) }
  }
}
