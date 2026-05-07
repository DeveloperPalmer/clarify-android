package ru.sla.clarify.feature.chat.data.convirsation

import ru.sla.clarify.feature.chat.domain.entity.Conversation

class ConversationState {
  // Keyed by conversation id (e.g. "c2c_<peerId>") so removals from the SDK can be applied directly.
  private val state = LinkedHashMap<String, Conversation>()

  fun upsertAll(list: List<Conversation>) {
    list.forEach { state[it.id] = it }
  }

  fun removeByIds(ids: Collection<String>) {
    ids.forEach { state.remove(it) }
  }

  fun snapshot(): List<Conversation> {
    return state.values.sortedByDescending { it.lastMessageTimestamp }
  }
}
