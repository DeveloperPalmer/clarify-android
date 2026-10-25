package ru.sla.clarify.feature.chronology.ui.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope
import ru.sla.clarify.feature.chronology.ui.screen.chronology.ChronologyScreen
import ru.sla.clarify.feature.chronology.ui.screen.chronology.ChronologyViewModel

@ContributesTo(ChronologyScope::class)
@Module
object ChronologyUiModule {
  @Provides
  fun provideChronologyScreen(model: ChronologyViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { ChronologyScreen(viewModel = it) }
  }
}
