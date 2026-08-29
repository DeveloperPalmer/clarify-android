package ru.sla.clarify.feature.chronology.ui.components.node

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chronology.ui.entity.MergeNodeState
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

/**
 * Точка слияния на магистрали — тема закрыта и возвращена в родителя (§6.6 брифа).
 *
 * Стоит на **всех уровнях детализации**: §5 брифа числит слияние в скелете смысла, который не
 * исчезает ни при каком упрощении.
 *
 * Цвет иконки берётся отдельным токеном, а не выводится из заливки, и это не перестраховка:
 * `successPrimary` переворачивает светлоту между темами — `#008224` в светлой и `#CCEBD5` в
 * тёмной, — поэтому одним контрастным цветом обойтись нельзя (§3.2 п. 3).
 *
 * Пульсации у кольца ожидания здесь нет, как нет и удара по кадру 5 церемонии: и то, и другое —
 * части церемонии слияния, а она пока не реализована. Компонент рисует её кадры 3 и 5 статично.
 *
 * @param contentDescription связная подпись для скринридера (§14). Тапа у узла нет, а подпись есть:
 *   рёбра скринридер не читает, и о возврате ветки в магистраль сказать больше некому
 * @param modifier модификатор узла
 * @param state кольцо ожидания или залитая точка
 */
@Composable
internal fun MergeNode(
  contentDescription: String,
  modifier: Modifier = Modifier,
  state: MergeNodeState = MergeNodeState.Done
) {
  val done = state == MergeNodeState.Done
  Box(
    modifier = modifier
      .clearAndSetSemantics { this.contentDescription = contentDescription }
      .size(24.dp)
      .background(
        // Заливка непрозрачная в обоих состояниях: узел стоит **на** линии магистрали, и
        // просвечивающая сквозь кольцо линия превратила бы его в перечёркнутый кружок.
        color = if (done) AppTheme.colors.successPrimary else AppTheme.colors.backgroundPrimary,
        shape = CircleShape
      )
      .then(
        // Обводка — примета одного лишь ожидания: у залитой точки она легла бы вторым контуром
        // поверх заливки и читалась бы как ещё одно состояние.
        if (done) {
          Modifier
        } else {
          Modifier.border(2.dp, AppTheme.colors.contentGoldPrimary, CircleShape)
        }
      ),
    contentAlignment = Alignment.Center
  ) {
    if (done) {
      Icon(
        modifier = Modifier.size(15.dp),
        painter = painterResource(R.drawable.ic_git_merged_24),
        tint = AppTheme.colors.graphMergeContent,
        contentDescription = null
      )
    }
  }
}

@Preview
@Composable
private fun MergeNodePreviewLight(
  @PreviewParameter(MergeNodePreviewProvider::class)
  merge: MergeNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    MergeNode(state = merge.state, contentDescription = "Слияние")
  }
}

@Preview
@Composable
private fun MergeNodePreviewDark(
  @PreviewParameter(MergeNodePreviewProvider::class)
  merge: MergeNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    MergeNode(state = merge.state, contentDescription = "Слияние")
  }
}

@Immutable
private data class MergeNodePreview(val state: MergeNodeState)

/**
 * Кадры превью [MergeNode] — по одному на состояние.
 *
 * Оба обязаны быть в кадре обеих тем: заливка и иконка меняют светлоту между темами навстречу друг
 * другу, и проверить это можно только глядя на них рядом.
 */
@Immutable
private class MergeNodePreviewProvider : PreviewParameterProvider<MergeNodePreview> {
  override val values = sequenceOf(
    MergeNodePreview(state = MergeNodeState.Pending),
    MergeNodePreview(state = MergeNodeState.Done)
  )
}
