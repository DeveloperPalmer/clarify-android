package ru.sla.clarify.uikit.component.bubble

import androidx.compose.runtime.Immutable
import arrow.optics.optics
import ru.sla.clarify.core.domain.entity.UserId

@Immutable
@optics
data class BubbleMessage(
  val id: Id,
  val type: Type,
  val side: Side,
  val text: String,
  val time: String,
  val sender: Sender?,
  val isSelected: Boolean
) {

  @JvmInline
  value class Id(val value: String)

  @Immutable
  data class Sender(
    val id: UserId,
    val name: String
  )

  @Immutable
  sealed interface Side {
    @Immutable
    data object Left : Side

    @Immutable
    data class Right(val status: ReadStatus? = null) : Side
  }

  @Immutable
  enum class ReadStatus {
    Sending,
    Sent,
    Read
  }

  @Immutable
  enum class Type {
    Top,
    Middle,
    Bottom
  }

  companion object
}
