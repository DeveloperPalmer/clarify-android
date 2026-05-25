package ru.sla.clarify.uikit.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import ru.sla.clarify.uikit.theme.VSpacer

@Composable
fun Divider(
  modifier: Modifier = Modifier,
  color: Color = AppTheme.colors.surfaceVariant
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(1.dp)
      .background(color = color)
  )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun DividerPreviewLight() {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    DividerPreview()
  }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun DividerPreviewDark() {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    DividerPreview()
  }
}

@Composable
private fun DividerPreview() {
  Column(
    modifier = Modifier
      .background(AppTheme.colors.surface)
  ) {
    VSpacer(24.dp)
    Divider(
      Modifier.padding(horizontal = 16.dp)
    )
    VSpacer(24.dp)
    Divider(
      Modifier.padding(horizontal = 24.dp)
    )
    VSpacer(48.dp)
    Divider(
      Modifier.padding(
        start = 48.dp,
        end = 16.dp
      )
    )
    VSpacer(24.dp)
    Divider(
      Modifier.padding(
        start = 68.dp,
        end = 16.dp
      )
    )
    VSpacer(48.dp)
    Divider(
      Modifier.padding(
        start = 16.dp,
        end = 48.dp
      )
    )
    VSpacer(24.dp)
    Divider(
      Modifier.padding(
        start = 16.dp,
        end = 68.dp
      )
    )
    VSpacer(24.dp)
  }
}
