package ru.sla.clarify.uikit.component

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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun Avatar(
  photoUrl: String?,
  fallbackInitial: String,
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
  highlighted: Boolean = false
) {
  Box(
    modifier = modifier
      .size(size)
      .surface(
        shape = CircleShape,
        backgroundColor = if (highlighted) {
          AppTheme.colors.errorPrimary
        } else {
          AppTheme.colors.textPrimary
        }
      ),
    contentAlignment = Alignment.Center
  ) {
    if (!photoUrl.isNullOrBlank()) {
      AsyncImage(
        model = photoUrl,
        contentDescription = null,
        modifier = Modifier
          .size(size)
          .clip(CircleShape),
        contentScale = ContentScale.Crop
      )
    } else {
      Text(
        text = fallbackInitial.take(1).uppercase(),
        style = AppTheme.typography.caption,
        color = AppTheme.colors.backgroundSecondary
      )
    }
  }
}
