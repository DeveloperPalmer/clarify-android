package ru.sla.clarify.feature.chronology.ui.components.node

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.modifier.surface
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
 * **Тап по чипу переигрывает церемонию** (§12), и в этом весь смысл чипа: кнопку финализации жмут на
 * экране ветки, поэтому саму церемонию почти никто не видит вживую. Клик объявляется `surface` в той
 * же цепочке модификаторов, что и `clearAndSetSemantics`: тот чистит семантику потомков, но не
 * своего же узла, и действие уцелевает — тем же способом, каким тап живёт у плашки эпизода.
 *
 * Текст и иконка берут `contentAccentDark` в обеих темах, а не `contentPrimary`: заливка
 * `successSecondary` бледно-зелёная в светлой теме и насыщенно-зелёная в тёмной, и тёмное содержимое
 * читается на обеих, тогда как содержимое, меняющее светлоту вместе с темой, пропало бы в одной.
 *
 * Высоту над точкой слияния — 34 dp по §12 — задаёт раскладка, а не чип: где стоит узел, дело
 * полотна.
 *
 * **Ширина ограничена, и слово при нужде обрезается.** Чип втрое шире своего узла: паддинги, иконка
 * и слово дают около 87 dp против 24 dp точки слияния, а при двухсотпроцентном шрифте — под 137 dp.
 * В накопительную ось X эта ширина не входит, поэтому чип свисает по обе стороны от точки и на
 * наименьшем зазоре 40 dp достаёт до соседней плашки. Обрезка эллипсисом — то же решение, что у
 * имени ветки в [BranchNode], и по той же причине: расти вширь узлу на дорожке нельзя.
 *
 * @param contentDescription связная подпись для скринридера (§14): чип называет закрытую тему, а
 *   не место на линии, поэтому подпись у него своя, отдельная от точки слияния
 * @param modifier модификатор чипа
 * @param maxWidth наибольшая ширина чипа: слово за ней обрезается эллипсисом
 * @param onClick тап по чипу — переигрывание церемонии; `null` — чип не нажимается
 */
@Composable
internal fun MergedRequestNode(
  contentDescription: String,
  modifier: Modifier = Modifier,
  maxWidth: Dp = 120.dp,
  onClick: (() -> Unit)? = null
) {
  Row(
    modifier = modifier
      .clearAndSetSemantics { this.contentDescription = contentDescription }
      // Скругление 12 при высоте 24 и есть капсула: других форм в системе нет, и заводить их ради
      // одного чипа не потребовалось.
      .defaultMinSize(minHeight = 24.dp)
      .widthIn(max = maxWidth)
      .surface(
        backgroundColor = AppTheme.colors.successSecondary,
        shape = AppTheme.shapes.round12,
        onClick = onClick
      )
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
      color = AppTheme.colors.contentAccentDark,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Preview
@Composable
private fun MergedRequestNodePreviewLight() {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    MergedRequestNodePreviewContent()
  }
}

@Preview
@Composable
private fun MergedRequestNodePreviewDark() {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    MergedRequestNodePreviewContent()
  }
}

/**
 * Оба кадра: чип по своей ширине и чип, упёршийся в предел.
 *
 * Второй нужен затем, что обрезка проверяется только на нём: при двухсотпроцентном шрифте (§14
 * брифа) слово перерастает бюджет, и увидеть это можно, лишь поставив узкий кадр рядом с обычным.
 */
@Composable
private fun MergedRequestNodePreviewContent() {
  MergedRequestNode(contentDescription = "Закрыта")
  MergedRequestNode(contentDescription = "Закрыта", maxWidth = 60.dp)
}
