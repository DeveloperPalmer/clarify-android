package ru.sla.clarify.feature.chronology.ui.components.node

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
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
 * Непрочитанное глиф показывает **гало и только им**: бейдж со счётчиком входит в измерение узла, и
 * коробка перестала бы быть четырнадцатью пикселями — а на обзоре именно из этого числа сложена вся
 * арифметика раскладки. Гало же рисуется за узлом и места не занимает, поэтому §8 на уровне, где
 * узлов больше всего, не отменяется.
 *
 * @param color цвет обводки: идентичность дорожки, на которой стоит узел
 * @param modifier модификатор узла
 * @param unreadCount счётчик непрочитанных; ноль — читать нечего. Отдельного флага нет намеренно:
 *   два параметра, которые всегда двигаются вместе, — способ ошибиться
 */
@Composable
internal fun GlyphNode(
  color: Color,
  modifier: Modifier = Modifier,
  unreadCount: Long = 0
) {
  val accentColor = AppTheme.colors.contentAccentPrimary
  Box(
    modifier = modifier
      // Гало рисуется до того, как узлу задан размер, и в раскладке места не занимает — то же
      // правило, что у плашки эпизода. Скругление равно половине глифа: у круга «скругление плашки»
      // это его радиус, и меньшее значение срезало бы гало углами.
      .drawBehind { if (unreadCount > 0) drawUnreadHalo(accentColor, 7.dp) }
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
    GlyphNode(
      modifier = Modifier.padding(UNREAD_HALO_WIDTH),
      color = glyph.lane.toPreviewColor(),
      unreadCount = glyph.unreadCount
    )
  }
}

@Preview
@Composable
private fun GlyphNodePreviewDark(
  @PreviewParameter(GlyphNodePreviewProvider::class)
  glyph: GlyphNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    GlyphNode(
      modifier = Modifier.padding(UNREAD_HALO_WIDTH),
      color = glyph.lane.toPreviewColor(),
      unreadCount = glyph.unreadCount
    )
  }
}

@Immutable
private data class GlyphNodePreview(val lane: Int, val unreadCount: Long = 0)

/**
 * Кадры превью [GlyphNode]: магистраль, две ветки и непрочитанное.
 *
 * Двух веток мало для палитры и достаточно для проверки: кадр существует, чтобы увидеть, что цвет
 * действительно параметризован, а не зашит в компонент. Последний кадр сторожит состояние, которое
 * иначе существовало бы только в коде: непрочитанный глиф на полотне рассмотреть нечем — на обзоре
 * он занимает четыре пикселя.
 */
@Immutable
private class GlyphNodePreviewProvider : PreviewParameterProvider<GlyphNodePreview> {
  override val values = sequenceOf(
    GlyphNodePreview(lane = 0),
    GlyphNodePreview(lane = 1),
    GlyphNodePreview(lane = 2),
    GlyphNodePreview(lane = 1, unreadCount = 3)
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
