package ru.sla.clarify.feature.chronology.domain.entity

import androidx.compose.runtime.Immutable
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit

/**
 * Ветка вместе со своей лентой.
 *
 * Лента бывает пустой, и это не ошибка: коммиты веток попадают в кэш, только когда ветку открывали,
 * — а сама ветка известна с первой же подписки на беседу. Ветка без единого коммита показывается
 * как ветка: раз она создана, она существует (решение владельца, журнал, итерация 39, п. 2).
 *
 * @param branch ветка со статусом merge request и счётчиком непрочитанного
 * @param commits лента ветки по возрастанию времени
 */
@Immutable
data class BranchHistory(
  val branch: Branch,
  val commits: List<Commit>
)
