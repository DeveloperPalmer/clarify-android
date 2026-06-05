package ru.sla.clarify.uikit.modifier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Suppress("LongParameterList") // complex logic
fun Modifier.surface(
  backgroundColor: Color,
  shape: Shape,
  border: BorderStroke? = null,
  enabled: Boolean = true,
  elevation: Dp = 0.dp,
  onClick: (() -> Unit)? = null,
  onLongClick: (() -> Unit)? = null
): Modifier {
  return this
    .shadow(elevation, shape)
    .then(if (border != null) Modifier.border(border, shape) else Modifier)
    .background(
      shape = shape,
      color = backgroundColor
    )
    .clip(
      shape = shape
    )
    .then(
      if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
          enabled = enabled,
          onClick = { onClick?.invoke() },
          onLongClick = onLongClick
        )
      } else {
        Modifier
      }
    )
}
