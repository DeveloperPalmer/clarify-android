package ru.sla.clarify.feature.chat.thread.ui.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import dagger.assisted.AssistedFactory
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.chat.thread.ui.screen.branch.BranchScreen
import ru.sla.clarify.feature.chat.thread.ui.screen.branch.BranchViewModel
import ru.sla.clarify.feature.chat.thread.ui.screen.thread.ThreadScreen
import ru.sla.clarify.feature.chat.thread.ui.screen.thread.ThreadViewModel
import javax.inject.Qualifier

@Module
@ContributesTo(ThreadScope::class)
object ThreadUiModule {
  @Provides
  @WiredScreen(Screen.Thread)
  fun provideThreadScreen(model: ThreadViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { ThreadScreen(viewModel = it) }
  }

  @Provides
  @WiredScreen(Screen.Branch)
  fun provideBranchScreenFactory(factory: BranchViewModelFactory): BranchWiredScreenFactory {
    return BranchWiredScreenFactory { branchId ->
      val viewModel = factory.create(branchId.value)
      WiredComposableScreen.bind(viewModel) { BranchScreen(viewModel = it) }
    }
  }
}

fun interface BranchWiredScreenFactory {
  fun create(branchId: Branch.Id): WiredComposableScreen
}

@AssistedFactory
interface BranchViewModelFactory {
  fun create(branchId: String): BranchViewModel
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

enum class Screen {
  Thread,
  Branch
}
