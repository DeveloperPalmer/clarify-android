package ru.sla.clarify.feature.chat.group.thread.domain.entity

import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation

// Non-inline обёртка над Conversation.Id: инлайн value class нельзя биндить через Dagger
// (@Inject-конструктор / @BindsInstance) — KSP падает на name mangling.
data class GroupThreadTarget(val conversationId: Conversation.Id)
