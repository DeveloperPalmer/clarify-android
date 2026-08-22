package ru.sla.clarify.feature.chronology.ui.entity

/** Состояние узла-сообщения: определяет иконку справа от текста и прозрачность плашки. */
enum class MessageNodeState {
  Normal,
  Edited,
  Quoted,
  Sending
}
