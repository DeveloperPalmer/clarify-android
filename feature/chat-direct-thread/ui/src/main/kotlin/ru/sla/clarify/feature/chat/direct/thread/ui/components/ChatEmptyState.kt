package ru.sla.clarify.feature.chat.direct.thread.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
internal fun ChatEmptyState(
  visible: Boolean,
  text: String,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    modifier = modifier,
    visible = visible
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = text,
        style = AppTheme.typography.body1
      )
    }
  }
}
