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
  val white = Color(0xFFFFFFFF)

  // Primary tones
  val primary10 = Color(0xFF00210B)
  val primary20 = Color(0xFF00390F)
  val primary30 = Color(0xFF00531E)
  val primary40 = Color(0xFF1F6B3A)
  val primary80 = Color(0xFF8BD99F)
  val primary90 = Color(0xFFA6F2B7)

  // Secondary tones
  val secondary10 = Color(0xFF101F10)
  val secondary20 = Color(0xFF243524)
  val secondary30 = Color(0xFF3A4B39)
  val secondary40 = Color(0xFF52634F)
  val secondary80 = Color(0xFFB9CCB4)
  val secondary90 = Color(0xFFD5E8CF)

  // Tertiary tones
  val tertiary10 = Color(0xFF001F22)
  val tertiary20 = Color(0xFF003739)
  val tertiary30 = Color(0xFF1F4D51)
  val tertiary40 = Color(0xFF38656A)
  val tertiary80 = Color(0xFFA0CFD4)
  val tertiary90 = Color(0xFFBBEBF0)

  // Error tones
  val error10 = Color(0xFF410002)
  val error20 = Color(0xFF690005)
  val error40 = Color(0xFFBA1A1A)
  val error30 = Color(0xFF93000A)
  val error80 = Color(0xFFFFB4AB)
  val error90 = Color(0xFFFFDAD6)

  // Neutral tones
  val neutral5 = Color(0xFF0B0F0B)
  val neutral10 = Color(0xFF101410)
  val neutral15 = Color(0xFF181D17)
  val neutral20 = Color(0xFF1C211B)
  val neutral25 = Color(0xFF262B25)
  val neutral30 = Color(0xFF313630)
  val neutral35 = Color(0xFF363A35)
  val neutral70 = Color(0xFFF7FBF2)
  val neutral80 = Color(0xFFF1F5EC)
  val neutral85 = Color(0xFFD8DBD2)
  val neutral90 = Color(0xFFE0E4DC)
  val neutral95 = Color(0xFFE6E9E1)
  val neutral99 = Color(0xFFECEFE7)

  // Neutral variant
  val neutralVariant30 = Color(0xFF424940)
  val neutralVariant50 = Color(0xFF727970)
  val neutralVariant60 = Color(0xFF8C928A)
  val neutralVariant80 = Color(0xFFC2C9BB)
  val neutralVariant90 = Color(0xFFDEE5D7)
}
