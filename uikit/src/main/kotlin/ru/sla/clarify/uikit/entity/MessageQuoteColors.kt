package ru.sla.clarify.uikit.entity

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class MessageQuoteColors(
  val background: Color,
  val accent: Color,
  val author: Color,
  val text: Color
)
