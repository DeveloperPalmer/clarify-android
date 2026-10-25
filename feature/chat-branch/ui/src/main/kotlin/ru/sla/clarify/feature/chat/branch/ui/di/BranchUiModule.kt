package ru.sla.clarify.feature.chat.branch.ui.di

import dagger.Module
import dagger.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.branch.ui.screen.BranchScreen
import ru.sla.clarify.feature.chat.branch.ui.screen.BranchViewModel

@Module
object BranchUiModule {
  @Provides
  fun provideBranchScreen(model: BranchViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { BranchScreen(viewModel = it) }
  }
}
