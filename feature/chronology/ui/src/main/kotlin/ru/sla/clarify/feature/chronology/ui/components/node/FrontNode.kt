package ru.sla.clarify.feature.chronology.ui.components.node

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

/**
 * Фронт — «сейчас», крайний правый узел магистрали (§6.7 брифа).
 *
 * Стоит на **всех уровнях детализации**: §5 брифа числит «вы здесь» и фронт в скелете смысла,
 * который не исчезает ни при каком упрощении.
 *
 * Гало нарисовано концентрическими кругами с падающей альфой, а не размытием. `Modifier.blur` при
 * `minSdk 31` доступен, но §16 брифа рекомендует именно круги: при панорамировании они не стоят
 * ничего, тогда как размытие пересчитывается каждый кадр.
 *
 * Пульсации гало здесь нет — §6.7 описывает её как бесконечную анимацию 1.0 → 1.6, и она приедет
 * вместе с решением по reduced motion (§14): отключать её придётся, а флага пока нет.
 *
 * Растворение магистрали правее фронта рисует полотно: это свойство линии, а не узла.
 *
 * @param modifier модификатор узла
 */
@Composable
internal fun FrontNode(modifier: Modifier = Modifier) {
  val accentColor = AppTheme.colors.contentAccentPrimary
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        // Гало выходит за коробку узла и места в раскладке не занимает: точка обязана стоять на
        // линии магистрали центром, а не краем разросшейся коробки.
        .drawBehind {
          val radius = size.minDimension / 2
          // От большего к меньшему: круги накладываются, и альфа набирается к центру сама.
          drawCircle(accentColor.copy(alpha = 0.06f), radius = radius * 1.6f)
          drawCircle(accentColor.copy(alpha = 0.10f), radius = radius * 1.3f)
          drawCircle(accentColor.copy(alpha = 0.14f), radius = radius * 1.1f)
        }
        .size(12.dp)
        .background(accentColor, CircleShape)
    )
    Text(
      // Подпись висит над точкой и в измерение по вертикали не входит: коробку по высоте задаёт
      // строка, а точка обязана остаться в её центре.
      modifier = Modifier.offset(y = (-18).dp),
      // Капслок применяется здесь, а не в стиле: `overline` задаёт только метрики разряжённого
      // начертания, а решение кричать принимает точка использования.
      text = stringResource(R.string.chronology_front_caption).uppercase(),
      style = AppTheme.typography.overline,
      color = AppTheme.colors.contentTertiary
    )
  }
}

@Preview
@Composable
private fun FrontNodePreviewLight() {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    // Подпись выходит за коробку узла вверх, и без поля кадр обрезал бы ровно её.
    FrontNode(modifier = Modifier.padding(vertical = 24.dp))
  }
}

@Preview
@Composable
private fun FrontNodePreviewDark() {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    FrontNode(modifier = Modifier.padding(vertical = 24.dp))
  }
}
