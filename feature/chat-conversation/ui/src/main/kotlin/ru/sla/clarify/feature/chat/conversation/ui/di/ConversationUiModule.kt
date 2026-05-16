package ru.sla.clarify.feature.chat.conversation.ui.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.conversation.ui.screen.main.ChatListScreen
import ru.sla.clarify.feature.chat.conversation.ui.screen.main.ChatListViewModel
import javax.inject.Qualifier

@Module
@ContributesTo(ConversationScope::class)
object ConversationUiModule {
  @Provides
  @WiredScreen(Screen.Main)
  fun provideMainScreen(model: ChatListViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { ChatListScreen(viewModel = it) }
  }
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

enum class Screen {
  Main
}
