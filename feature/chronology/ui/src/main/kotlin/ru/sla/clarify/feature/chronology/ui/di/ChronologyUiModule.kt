package ru.sla.clarify.feature.chronology.ui.di

import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope
import ru.sla.clarify.feature.chronology.ui.screen.chronology.ChronologyScreen
import ru.sla.clarify.feature.chronology.ui.screen.chronology.ChronologyViewModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesTo(ChronologyScope::class)
interface ChronologyUiModule {
  @Provides
  fun provideChronologyScreen(model: ChronologyViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { ChronologyScreen(viewModel = it) }
  }
}
