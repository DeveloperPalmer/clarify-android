package ru.sla.clarify.feature.chronology.ui.components.node

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.MergeCeremonyFrame
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
 * **Узел рисуется, а не собирается из модификаторов.** Кольцо, его пульсация, заливка ударом и рост
 * иконки — покадровые значения церемонии (§12), и `background` с `border` означали бы рекомпозицию
 * узла **внутри** `Layout` полотна на каждом кадре анимации. Поэтому [ceremony] приходит лямбдой и
 * читается в фазе рисования — тем же приёмом, которым камера меняет свой слой.
 *
 * @param contentDescription связная подпись для скринридера (§14). Тапа у узла нет, а подпись есть:
 *   рёбра скринридер не читает, и о возврате ветки в магистраль сказать больше некому
 * @param modifier модификатор узла
 * @param state кольцо ожидания или залитая точка; за пределами церемонии узел живёт только им
 * @param ceremony кадр церемонии, если она играет; `null` — узел в покое и рисуется по [state]
 */
@Composable
internal fun MergeNode(
  contentDescription: String,
  modifier: Modifier = Modifier,
  state: GraphNode.Merge.Status = GraphNode.Merge.Status.Done,
  ceremony: () -> MergeCeremonyFrame? = { null }
) {
  val colors = AppTheme.colors
  val restFill = if (state == GraphNode.Merge.Status.Done) 1f else 0f
  Box(
    modifier = modifier
      .clearAndSetSemantics { this.contentDescription = contentDescription }
      .size(24.dp)
      .drawBehind {
        val frame = ceremony()
        // До кадра 3 точки на магистрали нет вовсе (§6.6): она не «прозрачная», её ещё не случилось.
        val presence = frame?.ring ?: 1f
        val filled = frame?.impact ?: restFill
        if (presence <= 0f) {
          return@drawBehind
        }
        val radius = size.minDimension / 2f
        // Подложка непрозрачна намеренно: узел стоит **на** линии магистрали, и просвечивающая
        // сквозь кольцо линия превратила бы его в перечёркнутый кружок.
        drawCircle(color = colors.backgroundPrimary, radius = radius, alpha = presence)
        drawCircle(color = colors.successPrimary, radius = radius, alpha = presence * filled)
        val waiting = presence * (1f - filled)
        if (waiting > 0f) {
          drawCircle(
            color = colors.contentGoldPrimary,
            radius = radius - 1.dp.toPx(),
            alpha = waiting,
            style = Stroke(width = 2.dp.toPx())
          )
          val pulse = frame?.ringPulse ?: 0f
          if (pulse > 0f) {
            // Волна ожидания: кольцо расходится наружу до полутора радиусов и гаснет. Рисуется
            // обводкой, а не размытием, — при панорамировании это не стоит ничего (§16 брифа).
            drawCircle(
              color = colors.contentGoldPrimary,
              radius = radius * (1f + pulse / 2f),
              alpha = waiting * (1f - pulse) * 0.4f,
              style = Stroke(width = 1.dp.toPx())
            )
          }
        }
      },
    contentAlignment = Alignment.Center
  ) {
    Icon(
      modifier = Modifier
        .size(15.dp)
        .graphicsLayer {
          // Кадр 5: иконка приходит масштабом 0.6 → 1.0 вместе с заливкой, а не после неё.
          val filled = ceremony()?.impact ?: restFill
          alpha = filled
          scaleX = 0.6f + 0.4f * filled
          scaleY = scaleX
        },
      painter = painterResource(R.drawable.ic_git_merged_24),
      tint = AppTheme.colors.graphMergeContent,
      contentDescription = null
    )
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
private data class MergeNodePreview(val state: GraphNode.Merge.Status)

/**
 * Кадры превью [MergeNode] — по одному на состояние.
 *
 * Оба обязаны быть в кадре обеих тем: заливка и иконка меняют светлоту между темами навстречу друг
 * другу, и проверить это можно только глядя на них рядом.
 *
 * Кадров церемонии здесь нет и быть не может: превью снимает один момент, а церемония — движение.
 * Её раскадровку сторожит `MergeCeremonyTest`, и сторожит числами, а не глазом.
 */
@Immutable
private class MergeNodePreviewProvider : PreviewParameterProvider<MergeNodePreview> {
  override val values = sequenceOf(
    MergeNodePreview(state = GraphNode.Merge.Status.Pending),
    MergeNodePreview(state = GraphNode.Merge.Status.Done)
  )
}
