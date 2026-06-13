package ru.sla.clarify.feature.chat.branch.ui.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.branch.domain.di.BranchScope
import ru.sla.clarify.feature.chat.branch.ui.screen.BranchScreen
import ru.sla.clarify.feature.chat.branch.ui.screen.BranchViewModel

@Module
@ContributesTo(BranchScope::class)
object BranchUiModule {
  @Provides
  fun provideBranchScreen(model: BranchViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { BranchScreen(viewModel = it) }
  }
}
