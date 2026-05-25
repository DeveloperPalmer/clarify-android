package ru.sla.clarify.uikit.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

@Composable
@Suppress("ReusedModifierInstance") // AppTheme doesn't accept modifiers
fun PreviewColumn(
  modifier: Modifier = Modifier,
  verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
  colorTheme: ColorTheme = ColorTheme.Light,
  content: @Composable ColumnScope.() -> Unit
) {
  AppTheme(currentTheme = colorTheme) {
    Column(
      verticalArrangement = verticalArrangement,
      modifier = modifier.background(Color.White),
      content = content
    )
  }
}
