package ru.kode.demo.feature.main.ui.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import ru.kode.demo.core.ui.WiredComposableScreen
import ru.kode.demo.feature.main.domain.di.MainScope
import ru.kode.demo.feature.main.ui.screen.main.MainScreen
import ru.kode.demo.feature.main.ui.screen.main.MainViewModel
import javax.inject.Qualifier

@Module
@ContributesTo(MainScope::class)
object MainUiModule {
  @Provides
  @WiredScreen(Screen.Main)
  fun provideMainScreen(model: MainViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { MainScreen(viewModel = it) }
  }
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

// All screens, provided by this UI module will be mentioned here
enum class Screen { Main /*, Screen1*/ }
