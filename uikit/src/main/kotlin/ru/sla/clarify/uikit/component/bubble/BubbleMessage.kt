package ru.sla.clarify.uikit.component.bubble

import androidx.compose.runtime.Immutable

@Immutable
data class BubbleMessage(
  val type: Type,
  val side: Side,
  val text: String,
  val time: String
) {

  @Immutable
  sealed interface Side {
    @Immutable
    data object Left : Side

    @Immutable
    data class Right(val status: ReadStatus) : Side
  }

  @Immutable
  enum class ReadStatus {
    Sent,
    Delivered,
    Read
  }

  @Immutable
  enum class Type {
    Top,
    Middle,
    Bottom
  }
}