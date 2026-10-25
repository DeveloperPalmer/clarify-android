package ru.sla.clarify.feature.chat.conversation.ui.di

import dagger.Module
import dagger.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.conversation.ui.screen.conversation.ConversationScreen
import ru.sla.clarify.feature.chat.conversation.ui.screen.conversation.ConversationViewModel
import javax.inject.Qualifier

@Module
object ConversationUiModule {
  @Provides
  @WiredScreen(Screen.Conversation)
  fun provideConversationScreen(model: ConversationViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { ConversationScreen(viewModel = it) }
  }
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

enum class Screen {
  Conversation
}
