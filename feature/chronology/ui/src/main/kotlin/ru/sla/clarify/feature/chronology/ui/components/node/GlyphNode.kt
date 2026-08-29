package ru.sla.clarify.feature.chronology.ui.components.node

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

/**
 * Узел графа на **первом уровне детализации** — в обзоре.
 *
 * От узла здесь остаётся кружок на линии дорожки: текста нет вовсе, потому что на этом уровне видна
 * вся переписка целиком и читать в ней нечего — по глифам прослеживают форму разговора, а не его
 * содержание. Уровень записан в описании, а не в имени: перевод узла на другой уровень тогда стоит
 * правки одной строки, а не переименования, расползающегося по превью, состоянию и мапперу.
 *
 * Заливка непрозрачная, а не пустая, и это не вкус: глиф стоит **на** линии дорожки, и просвечивающая
 * сквозь него магистраль превратила бы кружок в перечёркнутый.
 *
 * Цвет приходит параметром, потому что это идентичность ветки (§7 брифа): на магистрали глиф
 * нейтрален, на ветке несёт её цвет. Знать, на какой он дорожке, узлу неоткуда.
 *
 * @param color цвет обводки: идентичность дорожки, на которой стоит узел
 * @param contentDescription связная подпись для скринридера (§14): на первом уровне детализации от
 *   узла остаётся кружок, и подпись — единственное, чем он себя называет
 * @param modifier модификатор узла
 */
@Composable
internal fun GlyphNode(
  color: Color,
  contentDescription: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clearAndSetSemantics { this.contentDescription = contentDescription }
      // 14 dp — размер из макета обзора. Он же задаёт и обводку: тоньше 2 dp кольцо на светлом
      // фоне пропадает, толще — заливка перестаёт читаться и глиф выглядит точкой.
      .size(14.dp)
      .background(AppTheme.colors.backgroundPrimary, CircleShape)
      .border(2.dp, color, CircleShape)
  )
}

@Preview
@Composable
private fun GlyphNodePreviewLight(
  @PreviewParameter(GlyphNodePreviewProvider::class)
  glyph: GlyphNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    GlyphNode(color = glyph.lane.toPreviewColor(), contentDescription = "Эпизод")
  }
}

@Preview
@Composable
private fun GlyphNodePreviewDark(
  @PreviewParameter(GlyphNodePreviewProvider::class)
  glyph: GlyphNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    GlyphNode(color = glyph.lane.toPreviewColor(), contentDescription = "Эпизод")
  }
}

@Immutable
private data class GlyphNodePreview(val lane: Int)

/**
 * Кадры превью [GlyphNode]: магистраль и две ветки.
 *
 * Двух веток мало для палитры и достаточно для проверки: кадр существует, чтобы увидеть, что цвет
 * действительно параметризован, а не зашит в компонент.
 */
@Immutable
private class GlyphNodePreviewProvider : PreviewParameterProvider<GlyphNodePreview> {
  override val values = sequenceOf(
    GlyphNodePreview(lane = 0),
    GlyphNodePreview(lane = 1),
    GlyphNodePreview(lane = 2)
  )
}

/**
 * Цвет дорожки для кадра превью.
 *
 * @return нейтральный для магистрали, из палитры идентичности для веток
 */
@Composable
private fun Int.toPreviewColor(): Color {
  return when (this) {
    0 -> AppTheme.colors.contentTertiary
    1 -> AppTheme.colors.graphLane1
    else -> AppTheme.colors.graphLane4
  }
}
