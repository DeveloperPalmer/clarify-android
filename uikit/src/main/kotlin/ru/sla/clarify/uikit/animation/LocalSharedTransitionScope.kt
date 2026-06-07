package ru.sla.clarify.uikit.animation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope> {
  error("No SharedTransitionScope provided. Wrap the root content in SharedTransitionLayout.")
}
