package ru.sla.clarify.uikit.component.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

@Composable
fun GroupAvatar(
  size: Dp,
  name: String,
  colorSeed: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(size)
      .background(groupAvatarColor(colorSeed), CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = remember(name) { groupInitials(name) },
      style = if (size >= largeAvatarThreshold) {
        AppTheme.typography.headline2Bold
      } else {
        AppTheme.typography.label1Bold
      },
      color = AppTheme.colors.contentAccentSecondary
    )
  }
}

@Composable
private fun groupAvatarColor(seed: String): Color {
  val palette = listOf(
    AppTheme.colors.contentAccentPrimary,
    AppTheme.colors.contentBlue,
    AppTheme.colors.contentGoldPrimary,
    AppTheme.colors.brandPrimary
  )
  return palette[stableSeedHash(seed).mod(palette.size)]
}

private fun groupInitials(name: String): String {
  return name
    .split(whitespaceRegex)
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString(separator = "") { it.first().uppercase() }
}

internal fun stableSeedHash(seed: String): Int {
  var hash = 0
  for (char in seed) {
    hash = hash * HASH_MULTIPLIER + char.code
  }
  return hash
}

private val whitespaceRegex = Regex("\\s+")
private val largeAvatarThreshold = 64.dp
private const val HASH_MULTIPLIER = 31

@Preview
@Composable
private fun GroupAvatarPreviewLight() {
  PreviewColumn {
    GroupAvatarPreviewContent()
  }
}

@Preview
@Composable
private fun GroupAvatarPreviewDark() {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    GroupAvatarPreviewContent()
  }
}

@Composable
private fun GroupAvatarPreviewContent() {
  Row(
    modifier = Modifier.padding(16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    GroupAvatar(size = 96.dp, name = "Дизайн-команда Clarify", colorSeed = "grp-design")
    GroupAvatar(size = 40.dp, name = "Дизайн-команда Clarify", colorSeed = "grp-design")
    GroupAvatar(size = 40.dp, name = "Релизы", colorSeed = "grp-releases")
    GroupAvatar(size = 40.dp, name = "Команда мечты", colorSeed = "grp-dream")
    GroupAvatar(size = 40.dp, name = "Q&A", colorSeed = "grp-qa")
  }
}
