package ru.sla.clarify.feature.chat.group.thread.domain.entity

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Conversation

@Immutable
data class Group(
  val id: Conversation.Id,
  val name: String,
  val ownerId: UserId,
  val memberCount: Int,
  val memberIds: List<UserId>
)
