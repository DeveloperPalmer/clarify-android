package ru.sla.clarify.feature.chronology.ui.components.node

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

/**
 * Закрытый merge request над точкой слияния — «тема доведена до конца» (§12 брифа).
 *
 * Существует потому, что основной зритель слияния случаен: кнопку финализации жмут на экране ветки,
 * а не в графе, и церемонию почти никто не видит. Чип остаётся над точкой навсегда и возвращает
 * пропущенное — тап по нему проиграет церемонию заново, сколько угодно раз.
 *
 * Тапа у чипа пока нет: церемония не реализована, и обработчик повис бы неподключённым — ровно то,
 * из-за чего отложен и тап по узлам графа.
 *
 * Текст и иконка берут `contentAccentDark` в обеих темах, а не `contentPrimary`: заливка
 * `successSecondary` бледно-зелёная в светлой теме и насыщенно-зелёная в тёмной, и тёмное содержимое
 * читается на обеих, тогда как содержимое, меняющее светлоту вместе с темой, пропало бы в одной.
 *
 * Высоту над точкой слияния — 34 dp по §12 — задаёт раскладка, а не чип: где стоит узел, дело
 * полотна.
 *
 * @param modifier модификатор чипа
 */
@Composable
internal fun MergedRequestNode(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      // Скругление 12 при высоте 24 и есть капсула: других форм в системе нет, и заводить их ради
      // одного чипа не потребовалось.
      .defaultMinSize(minHeight = 24.dp)
      .background(AppTheme.colors.successSecondary, AppTheme.shapes.round12)
      .padding(horizontal = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Icon(
      modifier = Modifier.size(12.dp),
      painter = painterResource(R.drawable.ic_git_merged_24),
      tint = AppTheme.colors.contentAccentDark,
      contentDescription = null
    )
    Text(
      // Та же строка, что и у статуса ветки: чип и статус говорят об одном и том же состоянии темы,
      // и второй ресурс с тем же словом разошёлся бы с первым на первой же правке формулировки.
      text = stringResource(R.string.chronology_branch_status_merged),
      style = AppTheme.typography.label3Bold,
      color = AppTheme.colors.contentAccentDark
    )
  }
}

@Preview
@Composable
private fun MergedRequestNodePreviewLight() {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    MergedRequestNode()
  }
}

@Preview
@Composable
private fun MergedRequestNodePreviewDark() {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    MergedRequestNode()
  }
}
