package ru.sla.clarify.uikit.component.popup

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun PopupScrim(
  visible: Boolean,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val animationSpec = AppTheme.motion.smallestTween<Float>()
  val revealProgress = remember { Animatable(0f) }
  val interactionSource = remember { MutableInteractionSource() }
  val dismissOnTap = if (visible) {
    Modifier.clickable(
      interactionSource = interactionSource,
      indication = null,
      onClick = onDismiss
    )
  } else {
    Modifier
  }
  LaunchedEffect(visible) {
    revealProgress.animateTo(
      animationSpec = animationSpec,
      targetValue = if (visible) 1f else 0f
    )
  }
  Box(
    modifier = modifier
      .fillMaxSize()
      .then(dismissOnTap)
      .graphicsLayer {
        alpha = revealProgress.value
      }
      .background(AppTheme.colors.backgroundTertiary.copy(0.25f))
  )
}
