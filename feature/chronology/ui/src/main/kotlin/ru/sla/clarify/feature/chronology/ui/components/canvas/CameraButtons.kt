package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

@Composable
internal fun CameraButtons(
  onStart: () -> Unit,
  onFront: () -> Unit,
  onBoundsChanged: (key: Any, bounds: Rect) -> Unit,
  modifier: Modifier = Modifier
) {
  val currentOnBoundsChanged by rememberUpdatedState(onBoundsChanged)
  DisposableEffect(Unit) {
    onDispose { currentOnBoundsChanged(ZONE_KEY, Rect.Zero) }
  }
  Column(
    modifier = modifier
      .onGloballyPositioned { currentOnBoundsChanged(ZONE_KEY, it.boundsInRoot()) },
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    CameraButton(
      iconResId = R.drawable.ic_skip_start_24,
      description = stringResource(R.string.chronology_camera_start),
      onClick = onStart
    )
    CameraButton(
      iconResId = R.drawable.ic_target_24,
      description = stringResource(R.string.chronology_camera_front),
      onClick = onFront
    )
  }
}

@Composable
private fun CameraButton(
  iconResId: Int,
  description: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(44.dp)
      .surface(
        backgroundColor = AppTheme.colors.cardPrimary,
        shape = CircleShape,
        elevation = AppTheme.elevation.small,
        onClick = onClick
      ),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      modifier = Modifier.size(18.dp),
      painter = painterResource(iconResId),
      tint = AppTheme.colors.contentPrimary,
      contentDescription = description
    )
  }
}

private val ZONE_KEY = Any()

@Preview
@Composable
private fun CameraButtonsPreviewLight() {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    CameraButtons(onStart = { }, onFront = { }, onBoundsChanged = { _, _ -> })
  }
}

@Preview
@Composable
private fun CameraButtonsPreviewDark() {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    CameraButtons(onStart = { }, onFront = { }, onBoundsChanged = { _, _ -> })
  }
}
