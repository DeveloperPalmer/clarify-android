package ru.sla.clarify.uikit.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ru.sla.clarify.uikit.R

private val Roboto = FontFamily(
  Font(R.font.roboto_regular, FontWeight.Normal),
  Font(R.font.roboto_medium, FontWeight.Medium),
  Font(R.font.roboto_bold, FontWeight.Bold)
)

@Immutable
// PlatformTextStyle with includeFontPadding is deprecated in compose 1.4, but default is "true" and it also is
// "undeprecated" in 1.5.x, see the release notes:
// https://developer.android.com/jetpack/androidx/releases/compose-ui#1.2.0-beta01
// IOW we need it
@Suppress("DEPRECATION")
class AppTypography {
  val display1: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Normal,
    fontSize      = 57.sp,
    lineHeight    = 64.sp,
    letterSpacing = (-0.25).sp,
    platformStyle = defaultFontPadding,
  )
  val display1Bold: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Bold,
    fontSize      = 57.sp,
    lineHeight    = 64.sp,
    letterSpacing = (-0.25).sp,
    platformStyle = defaultFontPadding
  )
  val display2: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Normal,
    fontSize      = 45.sp,
    lineHeight    = 52.sp,
    letterSpacing = 0.sp,
    platformStyle = defaultFontPadding
  )
  val display2Bold: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Bold,
    fontSize      = 45.sp,
    lineHeight    = 52.sp,
    letterSpacing = 0.sp,
    platformStyle = defaultFontPadding
  )
  val display3: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Normal,
    fontSize      = 36.sp,
    lineHeight    = 44.sp,
    letterSpacing = 0.sp,
    platformStyle = defaultFontPadding
  )
  val display3Bold: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Bold,
    fontSize      = 36.sp,
    lineHeight    = 44.sp,
    letterSpacing = 0.sp,
    platformStyle = defaultFontPadding
  )
  val headline1: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Normal,
    fontSize      = 32.sp,
    lineHeight    = 40.sp,
    letterSpacing = 0.sp,
    platformStyle = defaultFontPadding
  )
  val headline1Bold: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Bold,
    fontSize      = 32.sp,
    lineHeight    = 40.sp,
    letterSpacing = 0.sp,
    platformStyle = defaultFontPadding
  )
  val headline2: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Normal,
    fontSize      = 28.sp,
    lineHeight    = 36.sp,
    letterSpacing = 0.sp,
    platformStyle = defaultFontPadding
  )
  val headline2Bold: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Bold,
    fontSize      = 28.sp,
    lineHeight    = 36.sp,
    letterSpacing = 0.sp,
    platformStyle = defaultFontPadding
  )
  val headline3: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Normal,
    fontSize      = 24.sp,
    lineHeight    = 32.sp,
    letterSpacing = 0.sp,
    platformStyle = defaultFontPadding
  )
  val headline3Bold: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Bold,
    fontSize      = 24.sp,
    lineHeight    = 32.sp,
    letterSpacing = 0.sp,
    platformStyle = defaultFontPadding
  )
  val title1: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Normal,
    fontSize      = 22.sp,
    lineHeight    = 28.sp,
    letterSpacing = 0.sp,
    platformStyle = defaultFontPadding
  )
  val title1Bold: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Bold,
    fontSize      = 22.sp,
    lineHeight    = 28.sp,
    letterSpacing = 0.sp,
    platformStyle = defaultFontPadding
  )
  val title2: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Medium,
    fontSize      = 16.sp,
    lineHeight    = 24.sp,
    letterSpacing = 0.15.sp,
    platformStyle = defaultFontPadding
  )
  val title2Bold: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Bold,
    fontSize      = 16.sp,
    lineHeight    = 24.sp,
    letterSpacing = 0.15.sp,
    platformStyle = defaultFontPadding
  )
  val title3: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Medium,
    fontSize      = 14.sp,
    lineHeight    = 20.sp,
    letterSpacing = 0.1.sp,
    platformStyle = defaultFontPadding
  )
  val title3Bold: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Bold,
    fontSize      = 14.sp,
    lineHeight    = 20.sp,
    letterSpacing = 0.1.sp,
    platformStyle = defaultFontPadding
  )
  val body1: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Normal,
    fontSize      = 16.sp,
    lineHeight    = 24.sp,
    letterSpacing = 0.5.sp,
    platformStyle = defaultFontPadding
  )
  val body1Bold: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Bold,
    fontSize      = 16.sp,
    lineHeight    = 24.sp,
    letterSpacing = 0.5.sp,
    platformStyle = defaultFontPadding
  )
  val body2: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Normal,
    fontSize      = 14.sp,
    lineHeight    = 20.sp,
    letterSpacing = 0.25.sp,
    platformStyle = defaultFontPadding
  )
  val body2Bold: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Bold,
    fontSize      = 14.sp,
    lineHeight    = 20.sp,
    letterSpacing = 0.25.sp,
    platformStyle = defaultFontPadding
  )
  val body3: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Normal,
    fontSize      = 12.sp,
    lineHeight    = 16.sp,
    letterSpacing = 0.4.sp,
    platformStyle = defaultFontPadding
  )
  val body3Bold: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Bold,
    fontSize      = 12.sp,
    lineHeight    = 16.sp,
    letterSpacing = 0.4.sp,
    platformStyle = defaultFontPadding
  )
  val label1: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Medium,
    fontSize      = 14.sp,
    lineHeight    = 20.sp,
    letterSpacing = 0.1.sp,
    platformStyle = defaultFontPadding
  )
  val label1Bold: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Bold,
    fontSize      = 14.sp,
    lineHeight    = 20.sp,
    letterSpacing = 0.1.sp,
    platformStyle = defaultFontPadding
  )
  val label2: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Medium,
    fontSize      = 12.sp,
    lineHeight    = 16.sp,
    letterSpacing = 0.5.sp,
    platformStyle = defaultFontPadding
  )
  val label2Bold: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Bold,
    fontSize      = 12.sp,
    lineHeight    = 16.sp,
    letterSpacing = 0.5.sp,
    platformStyle = defaultFontPadding
  )
  val label3: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Medium,
    fontSize      = 11.sp,
    lineHeight    = 16.sp,
    letterSpacing = 0.5.sp,
    platformStyle = defaultFontPadding
  )
  val label3Bold: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Bold,
    fontSize      = 11.sp,
    lineHeight    = 16.sp,
    letterSpacing = 0.5.sp,
    platformStyle = defaultFontPadding
  )
  val caption: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Normal,
    fontSize      = 11.sp,
    lineHeight    = 14.sp,
    letterSpacing = 0.4.sp,
    platformStyle = defaultFontPadding
  )
  val captionBold: TextStyle = TextStyle(
    fontFamily    = Roboto,
    fontWeight    = FontWeight.Bold,
    fontSize      = 11.sp,
    lineHeight    = 14.sp,
    letterSpacing = 0.4.sp,
    platformStyle = defaultFontPadding
  )
}

internal val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
  error("No AppTypography provided")
}

private val defaultFontPadding = PlatformTextStyle(includeFontPadding = false)
