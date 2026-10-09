package ru.sla.clarify.database.entity

import ru.sla.clarify.entity.chat.Member

/**
 * Поля профиля nullable: участник приходит раньше своего профиля, а `LEFT JOIN` такого участника
 * не теряет.
 */
data class GroupMemberRow(
  val id: Member.Id,
  val displayName: String?,
  val email: String?,
  val photoUrl: String?
)
