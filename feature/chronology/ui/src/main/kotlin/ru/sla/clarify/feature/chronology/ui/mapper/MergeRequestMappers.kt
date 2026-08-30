package ru.sla.clarify.feature.chronology.ui.mapper

import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranchStatus

/**
 * Merge request ветки — в статус, которым она рисуется.
 *
 * Расширение над **nullable** типом, и в этом вся суть маппинга: «Обсуждается» — это отсутствие
 * merge request, а не одно из его состояний. Домен даёт три значения и `null`, экран — четыре
 * равноправных состояния линии; поэтому статус и есть сущность экрана, а не доменный `enum`,
 * вынесенный в `ui`.
 *
 * @return состояние, из которого выводятся штрих линии и строка статуса
 */
internal fun Branch.MergeRequest?.toGraphBranchStatus(): GraphBranchStatus {
  return when (this?.status) {
    null -> GraphBranchStatus.Alive
    Branch.MergeRequest.Status.Open -> GraphBranchStatus.Waiting
    Branch.MergeRequest.Status.ReadyToMerge -> GraphBranchStatus.Ready
    Branch.MergeRequest.Status.Merged -> GraphBranchStatus.Merged
  }
}
