package ru.sla.clarify.uikit.component.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun Avatar(
  size: Dp,
  photoUrl: String?,
  fallbackInitial: String,
  modifier: Modifier = Modifier
) {
  if (!photoUrl.isNullOrBlank()) {
    AsyncImage(
      modifier = Modifier
        .size(size)
        .clip(CircleShape),
      model = photoUrl,
      contentDescription = null,
      contentScale = ContentScale.Crop
    )
  } else {
    Box(
      modifier = modifier
        .size(size)
        .background(AppTheme.colors.cardSecondary, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = fallbackInitial.take(1).uppercase(),
        style = AppTheme.typography.title3,
        color = AppTheme.colors.contentPrimary
      )
    }
  }
}
