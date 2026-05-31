package ru.sla.clarify.uikit.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp

fun Modifier.bottomShadow(elevation: Dp): Modifier {
  return this
    .drawWithContent {
      clipRect(
        left = -size.width,
        top = 0f,
        right = size.width * 2,
        bottom = size.height + elevation.toPx()
      ) {
        this@drawWithContent.drawContent()
      }
    }
    .shadow(elevation)
}
