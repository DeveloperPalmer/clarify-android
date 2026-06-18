package ru.sla.clarify.uikit.window

import android.os.Build
import android.view.Window
import androidx.annotation.ColorInt

/**
 * Sets the navigation bar color on platform versions where it still has an effect.
 *
 * Since API 35 (VANILLA_ICE_CREAM) edge-to-edge is enforced and both
 * [Window.setNavigationBarColor] and [Window.setNavigationBarContrastEnforced] became
 * deprecated no-ops — the system draws the bars instead. On API 31..34 these calls still
 * drive the look, so they are kept behind a version guard. Centralising them here keeps
 * the unavoidable `@Suppress("DEPRECATION")` in a single place.
 */
@Suppress("DEPRECATION")
fun Window.setNavigationBarColorCompat(
  @ColorInt color: Int,
  contrastEnforced: Boolean = true
) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) return
  navigationBarColor = color
  isNavigationBarContrastEnforced = contrastEnforced
}
