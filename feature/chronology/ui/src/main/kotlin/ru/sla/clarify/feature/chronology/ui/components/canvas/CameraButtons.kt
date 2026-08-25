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

/**
 * Две кнопки камеры: «к началу переписки» и «к текущему моменту» (§11.1 брифа).
 *
 * Обе уводят камеру перелётом, а не прыжком, и это требование спеки, а не украшение: камера,
 * телепортировавшаяся через всю историю, не оставляет зрителю ничего, из чего понять, куда он попал.
 * Само движение живёт в держателе камеры — кнопка знает только, что её нажали.
 *
 * Круги по 44 dp — минимальная цель касания, а не размер иконки: сама иконка внутри 18 dp.
 *
 * @param onStart нажата кнопка «к началу переписки»
 * @param onFront нажата кнопка «к текущему моменту»
 * @param onBoundsChanged куда встали кнопки и когда их не стало: полотно по этой зоне отличает
 *   палец на кнопке от пальца на графе — иначе съехавший с кнопки палец потащит за собой граф
 * @param modifier модификатор пары кнопок
 */
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
    modifier = modifier.onGloballyPositioned {
      currentOnBoundsChanged(ZONE_KEY, it.boundsInRoot())
    },
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

/**
 * Одна круглая кнопка камеры.
 *
 * @param iconResId иконка внутри круга
 * @param description что кнопка делает, для скринридера
 * @param onClick нажатие
 * @param modifier модификатор кнопки
 */
@Composable
private fun CameraButton(
  iconResId: Int,
  description: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      // 44 dp — минимальная цель касания; иконка внутри вчетверо меньше круга.
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

// Ключ зоны жеста: важно только то, что он один на пару кнопок и не совпадает с чужим. Читается
// дважды — при объявлении зоны и при её снятии.
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
