package ru.sla.clarify.uikit.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val LocalAppElevation = staticCompositionLocalOf<AppElevation> {
  error("No AppElevation provided")
}

@Immutable
class AppElevation {
  val smallest: Dp = 1.dp
  val small: Dp = 3.dp
  val medium: Dp = 6.dp
  val large: Dp = 8.dp
  val largest: Dp = 12.dp
}
