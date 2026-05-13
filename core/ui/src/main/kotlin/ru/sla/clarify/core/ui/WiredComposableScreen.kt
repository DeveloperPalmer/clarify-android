package ru.sla.clarify.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import ru.kode.amvi.viewmodel.ViewIntents
import ru.kode.amvi.viewmodel.ViewModel

/**
 * A Composable screen with ViewModel already bound
 */
interface WiredComposableScreen {
  companion object {
    fun <VS : Any, VI : ViewIntents, VM : ViewModel<VS, VI>> bind(
      viewModel: VM,
      screen: @Composable (VM) -> Unit
    ): WiredComposableScreen {
      return BasicWiredComposableScreen(viewModel, screen)
    }
  }

  @Composable
  fun Content(modifier: Modifier)

  fun destroy()
}

private class BasicWiredComposableScreen<VM : ViewModel<*, *>>(
  @Stable
  private val viewModel: VM,
  private val screen: @Composable (VM) -> Unit
) : WiredComposableScreen {
  @Composable
  override fun Content(modifier: Modifier) {
    screen(viewModel)
  }

  override fun destroy() {
    viewModel.destroy()
  }

  override fun toString(): String {
    return viewModel.toString()
  }
}
