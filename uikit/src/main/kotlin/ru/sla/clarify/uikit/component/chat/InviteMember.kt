package ru.sla.clarify.uikit.component.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun InviteMember(
  text: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center
  ) {
    Text(
      modifier = Modifier
        .widthIn(max = maxCapsuleWidth)
        .background(AppTheme.colors.backgroundSecondary, AppTheme.shapes.round12)
        .padding(horizontal = 14.dp, vertical = 5.dp),
      text = text,
      style = AppTheme.typography.label3,
      color = AppTheme.colors.contentSecondary,
      textAlign = TextAlign.Center
    )
  }
}

private val maxCapsuleWidth = 280.dp
