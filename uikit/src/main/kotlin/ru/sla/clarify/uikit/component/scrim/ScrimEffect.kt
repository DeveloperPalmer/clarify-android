package ru.sla.clarify.uikit.component.scrim

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun ScrimEffect(
  visible: Boolean,
  onFinish: () -> Unit,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    modifier = modifier,
    visible = visible,
    enter = fadeIn(tween(SCRIM_DURATION_MILLIS)),
    exit = fadeOut(tween(SCRIM_DURATION_MILLIS))
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(AppTheme.colors.backgroundTertiary)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onFinish
        )
    )
  }
}

private const val SCRIM_DURATION_MILLIS = 220
