package ru.sla.clarify.uikit.component.icon

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun IconAction(
  iconResId: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  iconTint: Color = AppTheme.colors.contentSecondary,
  contentDescription: String? = null
) {
  IconButton(
    modifier = modifier,
    onClick = onClick
  ) {
    Icon(
      painter = painterResource(iconResId),
      tint = iconTint,
      contentDescription = contentDescription
    )
  }
}
