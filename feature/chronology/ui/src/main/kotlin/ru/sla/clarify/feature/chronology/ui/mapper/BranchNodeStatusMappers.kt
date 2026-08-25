package ru.sla.clarify.feature.chronology.ui.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chronology.ui.entity.BranchNodeStatus
import ru.sla.clarify.uikit.theme.AppColors

/**
 * Иконка, которой статус ведёт свою строку.
 *
 * Иконка есть у каждого статуса, поэтому `null` здесь не возвращается: строка статуса на узле ветки
 * рисуется всегда, а `ic_clock_24` заведена ровно затем, чтобы [BranchNodeStatus.Abandoned] не
 * оказался единственным состоянием без значка.
 *
 * @return идентификатор рисунка
 */
internal fun BranchNodeStatus.toIconResId(): Int {
  return when (this) {
    BranchNodeStatus.Active -> R.drawable.ic_git_branch_24
    BranchNodeStatus.Waiting -> R.drawable.ic_git_pull_request_24
    BranchNodeStatus.Ready -> R.drawable.ic_git_merge_24
    BranchNodeStatus.Merged -> R.drawable.ic_git_merged_24
    is BranchNodeStatus.Abandoned -> R.drawable.ic_clock_24
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
 * @return золото у ожидания, зелёный у закрытой темы, приглушённый серый у брошенной
 */
internal fun BranchNodeStatus.toIconTint(colors: AppColors): Color {
  return when (this) {
    BranchNodeStatus.Active -> colors.contentTertiary
    BranchNodeStatus.Waiting,
    BranchNodeStatus.Ready -> colors.contentGoldPrimary
    BranchNodeStatus.Merged -> colors.successPrimary
    is BranchNodeStatus.Abandoned -> colors.contentQuaternary
  }
}

/**
 * Строка статуса словами.
 *
 * У брошенной темы это plural, а не строка с подстановкой: по-русски у «дня» четыре формы, и
 * «Нет сообщений 34 дней» получилось бы ровно из попытки обойтись одним `%d`.
 *
 * @return готовая к показу строка на языке устройства
 */
@Composable
internal fun BranchNodeStatus.toLabel(): String {
  return when (this) {
    BranchNodeStatus.Active -> stringResource(R.string.chronology_branch_status_active)
    BranchNodeStatus.Waiting -> stringResource(R.string.chronology_branch_status_waiting)
    BranchNodeStatus.Ready -> stringResource(R.string.chronology_branch_status_ready)
    BranchNodeStatus.Merged -> stringResource(R.string.chronology_branch_status_merged)
    is BranchNodeStatus.Abandoned -> pluralStringResource(
      R.plurals.chronology_branch_status_abandoned,
      silentDays,
      silentDays
    )
  }
}
