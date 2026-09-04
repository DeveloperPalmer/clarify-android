package ru.sla.clarify.feature.chronology.ui.components.node

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chronology.ui.entity.BranchColor
import ru.sla.clarify.feature.chronology.ui.mapper.toColor
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

@Composable
internal fun ForkNode(
  color: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .size(24.dp)
        .background(AppTheme.colors.backgroundPrimary, CircleShape)
        .border(2.dp, color, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        modifier = Modifier.size(14.dp),
        painter = painterResource(R.drawable.ic_git_fork_24),
        tint = color,
        contentDescription = null
      )
    }
  }
}

@Preview
@Composable
private fun ForkNodePreviewLight(
  @PreviewParameter(ForkNodePreviewProvider::class)
  fork: ForkNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    ForkNodePreviewContent(fork)
  }
}

@Preview
@Composable
private fun ForkNodePreviewDark(
  @PreviewParameter(ForkNodePreviewProvider::class)
  fork: ForkNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    ForkNodePreviewContent(fork)
  }
}

@Composable
private fun ForkNodePreviewContent(fork: ForkNodePreview) {
  ForkNode(
    // Через настоящий маппер, а не через свой цвет: кадр заодно проверяет, что соседние ветки
    // получают разные оттенки.
    color = fork.color.toColor(AppTheme.colors)
  )
}

@Immutable
private data class ForkNodePreview(val color: BranchColor)

/**
 * Кадры превью [ForkNode]: разные оттенки.
 *
 * Направления в кадрах больше нет — его показывает ребро ухода, которого в превью узла не
 * существует. Оттенков три, потому что единственное, что здесь проверяется, — что цвет приходит
 * параметром и берётся из палитры идентичности.
 */
@Immutable
private class ForkNodePreviewProvider : PreviewParameterProvider<ForkNodePreview> {
  override val values = sequenceOf(
    ForkNodePreview(color = BranchColor.First),
    ForkNodePreview(color = BranchColor.Second),
    ForkNodePreview(color = BranchColor.Fifth)
  )
}
