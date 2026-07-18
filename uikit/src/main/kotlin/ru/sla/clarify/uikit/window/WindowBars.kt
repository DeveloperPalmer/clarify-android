package ru.sla.clarify.uikit.window

import android.os.Build
import android.view.Window
import androidx.annotation.ColorInt

/**
 * Sets the navigation bar color on platform versions where it still has an effect.
 *
 * Since API 35 (VANILLA_ICE_CREAM) edge-to-edge is enforced and [Window.setNavigationBarColor]
 * became a deprecated no-op — the system draws the bars instead. On API 31..34 the call still
 * drives the look, so it is kept behind a version guard. Centralising it here keeps the
 * unavoidable `@Suppress("DEPRECATION")` in a single place.
 *
 * [Window.setNavigationBarContrastEnforced] is NOT a no-op on API 35+: it still controls the
 * scrim the system draws behind 3-button navigation (default is `true`), so it must be applied
 * on every version.
 */
@Suppress("DEPRECATION")
fun Window.setNavigationBarColorCompat(
  @ColorInt color: Int,
  contrastEnforced: Boolean = true
) {
  isNavigationBarContrastEnforced = contrastEnforced
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) return
  navigationBarColor = color
}
