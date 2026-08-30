package ru.sla.clarify.feature.chronology.ui.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import ru.sla.atlas.entity.Branch
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.theme.AppColors

/**
 * Иконка, которой статус ведёт свою строку.
 *
 * Иконка есть у каждого статуса, поэтому `null` здесь не возвращается: строка статуса на узле ветки
 * рисуется всегда.
 *
 * @return идентификатор рисунка
 */
internal fun Branch.Status.toIconResId(): Int {
  return when (this) {
    Branch.Status.Alive -> R.drawable.ic_git_branch_24
    Branch.Status.Waiting -> R.drawable.ic_git_pull_request_24
    Branch.Status.Ready -> R.drawable.ic_git_merge_24
    Branch.Status.Merged -> R.drawable.ic_git_merged_24
  }
}

/**
 * Цвет иконки статуса — единственное место, где статус кодируется цветом.
 *
 * Строка статуса цвет не берёт, и это решение с ценой: `contentGoldPrimary` на карточке в светлой
 * теме даёт 2.19 : 1 при пороге 4.5 : 1, то есть §3.2 п. 4 и §14 брифа запрещают золото как цвет
 * мелкого текста. У иконки та же цифра допустима: рядом с ней стоит слово, и цвет здесь не
 * единственный носитель смысла, а его усиление.
 *
 * @param colors палитра активной темы
 * @return золото у ожидания, зелёный у закрытой темы, нейтральный серый у живой
 */
internal fun Branch.Status.toIconTint(colors: AppColors): Color {
  return when (this) {
    Branch.Status.Alive -> colors.contentTertiary
    Branch.Status.Waiting,
    Branch.Status.Ready -> colors.contentGoldPrimary
    Branch.Status.Merged -> colors.successPrimary
  }
}

/**
 * Подпись чипа «Закрыта» для скринридера.
 *
 * Чип — отдельный элемент рядом с точкой слияния, а не её часть, поэтому и подпись у него своя: он
 * говорит не «здесь ветка вернулась», а «эта тема закрыта». Собирается тем же порядком, что подписи
 * узлов, — см. [nodeDescriptionOf].
 *
 * @param branchName имя закрытой ветки; `null`, если имени нет
 * @return готовая к озвучиванию строка
 */
@Composable
internal fun Branch.Status.toNodeDescription(branchName: String?): String {
  return nodeDescriptionOf(
    listOf(
      toLabel(),
      branchName?.let { stringResource(R.string.chronology_node_branch, it) }
    )
  )
}

/**
 * Строка статуса словами.
 *
 * @return готовая к показу строка на языке устройства
 */
@Composable
internal fun Branch.Status.toLabel(): String {
  return when (this) {
    Branch.Status.Alive -> stringResource(R.string.chronology_branch_status_alive)
    Branch.Status.Waiting -> stringResource(R.string.chronology_branch_status_waiting)
    Branch.Status.Ready -> stringResource(R.string.chronology_branch_status_ready)
    Branch.Status.Merged -> stringResource(R.string.chronology_branch_status_merged)
  }
}
