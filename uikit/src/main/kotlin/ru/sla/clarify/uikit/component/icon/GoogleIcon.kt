package ru.sla.clarify.uikit.component.icon

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun GoogleIcon(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .size(24.dp)
      .surface(
        shape = CircleShape,
        border = BorderStroke(Dp.Hairline, AppTheme.colors.cardSecondary),
        backgroundColor = AppTheme.colors.cardPrimary
      )
      .padding(4.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = stringResource(R.string.G),
      color = AppTheme.colors.contentPrimary,
      style = AppTheme.typography.title3
    )
  }
}

@Preview
@Composable
private fun GoogleIconPreview() {
  PreviewColumn { GoogleIcon() }
}
