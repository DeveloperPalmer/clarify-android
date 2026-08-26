package ru.sla.clarify.feature.chronology.ui.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranchStatus
import ru.sla.clarify.uikit.theme.AppColors

/**
 * Иконка, которой статус ведёт свою строку.
 *
 * Иконка есть у каждого статуса, поэтому `null` здесь не возвращается: строка статуса на узле ветки
 * рисуется всегда.
 *
 * @return идентификатор рисунка
 */
internal fun GraphBranchStatus.toIconResId(): Int {
  return when (this) {
    GraphBranchStatus.Alive -> R.drawable.ic_git_branch_24
    GraphBranchStatus.Waiting -> R.drawable.ic_git_pull_request_24
    GraphBranchStatus.Ready -> R.drawable.ic_git_merge_24
    GraphBranchStatus.Merged -> R.drawable.ic_git_merged_24
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
internal fun GraphBranchStatus.toIconTint(colors: AppColors): Color {
  return when (this) {
    GraphBranchStatus.Alive -> colors.contentTertiary
    GraphBranchStatus.Waiting,
    GraphBranchStatus.Ready -> colors.contentGoldPrimary
    GraphBranchStatus.Merged -> colors.successPrimary
  }
}

/**
 * Строка статуса словами.
 *
 * @return готовая к показу строка на языке устройства
 */
@Composable
internal fun GraphBranchStatus.toLabel(): String {
  return when (this) {
    GraphBranchStatus.Alive -> stringResource(R.string.chronology_branch_status_alive)
    GraphBranchStatus.Waiting -> stringResource(R.string.chronology_branch_status_waiting)
    GraphBranchStatus.Ready -> stringResource(R.string.chronology_branch_status_ready)
    GraphBranchStatus.Merged -> stringResource(R.string.chronology_branch_status_merged)
  }
}
