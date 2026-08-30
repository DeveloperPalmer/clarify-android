package ru.sla.clarify.feature.chronology.domain.entity

import androidx.compose.runtime.Immutable
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit

/**
 * Магистраль беседы: её лента и то, сколько в ней осталось непрочитанного.
 *
 * Отдельным типом от [BranchHistory], потому что магистраль — не ветка с пустыми полями:
 * [ru.sla.clarify.entity.chat.Branch] у неё нет вовсе, в таблице веток её нет, а идентификатор
 * совпадает с идентификатором беседы. Сложенная в общий список с `null`-полями, она заставила бы
 * каждого читателя проверять, не магистраль ли это, — и первый забывший проверить получил бы ветку
 * без имени и без статуса.
 *
 * @param id идентификатор магистрали, он же идентификатор беседы
 * @param commits лента магистрали по возрастанию времени
 * @param unreadCount непрочитанное магистрали — счётчик беседы, тот же, что показывает чат
 */
@Immutable
data class BaselineHistory(
  val id: Branch.Id,
  val commits: List<Commit>,
  val unreadCount: Long
)
