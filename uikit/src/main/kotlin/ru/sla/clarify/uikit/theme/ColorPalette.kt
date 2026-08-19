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
  // region Палитра (gray / purple / red / green / cyan / gold / дорожки графа)

  val gray0 = Color(0xFFFFFFFF)
  val gray100 = Color(0xFFF5F5F5)
  val gray115 = Color(0xFFEDEDED)
  val gray130 = Color(0xFFE7E7E7)
  val gray150 = Color(0xFFC7C7C7)
  val gray200 = Color(0xFFB2B2B2)
  val gray250 = Color(0xFF747478)
  val gray300 = Color(0xFF565860)
  val gray350 = Color(0xFF4A4E5D)
  val gray400 = Color(0xFF62646A)
  val gray450 = Color(0xFF404040)
  val gray500 = Color(0xFF2C2C2C)
  val gray550 = Color(0xFF1D1D1D)
  val gray600 = Color(0xFF1D1E21)
  val gray700 = Color(0xFF171825)
  val gray800 = Color(0xFF1C1E24)
  val gray900 = Color(0xFF141414)
  val gray1000 = Color(0xFF000000)

  val purple0 = Color(0xFFECDCFB)
  val purple100 = Color(0xFF9B3AFC)
  val purple150 = Color(0xFF9730FE)
  val purple200 = Color(0xFF9326FF)
  val purple250 = Color(0xFFBE9BFF)
  val purple300 = Color(0xFF9934FE)
  val purple400 = Color(0xFF8000FF)
  val purple500 = Color(0xFF7520FF)
  val purple900 = Color(0xFF2D2634)

  val red0 = Color(0xFFFFF0F0)
  val red100 = Color(0xFFFF3939)
  val red200 = Color(0xFFD21515)
  val red900 = Color(0xFF3A1B1B)

  val green0 = Color(0xFFCCEBD5)
  val green100 = Color(0xFF5EC97C)
  val green200 = Color(0xFF008224)
  val green900 = Color(0xFF116729)

  val cyan200 = Color(0xFF05C2CE)

  val gold200 = Color(0xFFDFA616)

  // Идентичность дорожки графа хронологии. Оттенки подобраны вне семантики: зелёный, золотой,
  // фиолетовый и красный уже заняты статусами, поэтому цвет ветки не может их использовать.
  // Первая дорожка переиспользует cyan200 — дубликат хекса в палитре заводить нельзя.
  val blue200 = Color(0xFF3B7DE8)
  val teal200 = Color(0xFF1AA39A)
  val magenta200 = Color(0xFFC74B93)
  val terracotta200 = Color(0xFFB4713E)
  val slate200 = Color(0xFF7A8CA0)

  // endregion
}
