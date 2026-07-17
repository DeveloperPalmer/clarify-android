package ru.sla.clarify.feature.chat.branch.ui.di

import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.branch.domain.di.BranchScope
import ru.sla.clarify.feature.chat.branch.ui.screen.BranchScreen
import ru.sla.clarify.feature.chat.branch.ui.screen.BranchViewModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesTo(BranchScope::class)
interface BranchUiModule {
  @Provides
  fun provideBranchScreen(model: BranchViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { BranchScreen(viewModel = it) }
  }
}
