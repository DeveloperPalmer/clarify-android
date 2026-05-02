package ru.sla.clarify.uikit.theme

import androidx.compose.ui.graphics.Color

// IMPORTANT: Do not ever make this "public"!
//
// feature screens and components and uikit components should never use palette colors directly,
// they should always work only with "AppColors".
// If your component has color which is absent from "AppColors", this means that designer made an error,
// you must contact designer team and ask them to use only "roles" (tokens) from uikit and never use "new" colors
// directly. If no corresponding role is present it must be created in UiKit by designers
// (but it's better to reuse some old one ideally)
//
// IMPORTANT: Do not ever make this "public"! See ^^^
internal object ColorPalette {
  val Red = Color(0xFFFF0000)
  val Green = Color(0xFF00FF00)
  val Blue = Color(0xFF0000FF)
}
