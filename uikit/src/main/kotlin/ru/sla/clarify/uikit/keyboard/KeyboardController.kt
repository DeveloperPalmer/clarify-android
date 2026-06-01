package ru.sla.clarify.uikit.keyboard

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.flow.first

@Stable
class KeyboardController internal constructor(
  private val density: Density,
  private val imeInsets: WindowInsets,
  private val keyboardController: SoftwareKeyboardController?
) {

  suspend fun awaitHide() {
    keyboardController?.hide()
    snapshotFlow { imeInsets.getBottom(density) }.first { it == 0 }
  }
}

@Composable
fun rememberKeyboardController(): KeyboardController {
  val keyboardController = LocalSoftwareKeyboardController.current
  val imeInsets = WindowInsets.ime
  val density = LocalDensity.current
  return remember(density, imeInsets, keyboardController) {
    KeyboardController(
      density = density,
      imeInsets = imeInsets,
      keyboardController = keyboardController
    )
  }
}
