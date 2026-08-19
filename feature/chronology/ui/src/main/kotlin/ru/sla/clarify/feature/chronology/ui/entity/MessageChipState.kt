package ru.sla.clarify.feature.chronology.ui.entity

/** Состояние сообщения-узла: определяет иконку справа от текста и прозрачность чипа. */
enum class MessageChipState {
  Normal,
  Edited,
  Quoted,
  Sending
}
