package ru.sla.clarify.uikit.modifier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
  onClick: (() -> Unit)? = null
): Modifier {
  return this
    .shadow(elevation, shape)
    .then(if (border != null) Modifier.border(border, shape) else Modifier)
    .background(color = backgroundColor, shape = shape)
    .clip(shape)
    .then(
      if (onClick != null) {
        Modifier.clickable(onClick = onClick, enabled = enabled)
      } else {
        Modifier
      }
    )
}

@Suppress("LongParameterList") // complex logic
fun Modifier.surface(
  backgroundBrush: Brush,
  shape: Shape,
  border: BorderStroke? = null,
  enabled: Boolean = true,
  elevation: Dp = 0.dp,
  onClick: (() -> Unit)? = null
): Modifier {
  return this
    .shadow(elevation, shape)
    .then(if (border != null) Modifier.border(border, shape) else Modifier)
    .background(brush = backgroundBrush, shape = shape)
    .clip(shape)
    .then(
      if (onClick != null) {
        Modifier.clickable(onClick = onClick, enabled = enabled)
      } else {
        Modifier
      }
    )
}
