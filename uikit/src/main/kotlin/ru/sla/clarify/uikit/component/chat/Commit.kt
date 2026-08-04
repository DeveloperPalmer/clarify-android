package ru.sla.clarify.uikit.component.chat

import androidx.compose.runtime.Immutable
import arrow.optics.optics
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.resourcerefs.TextRef
import ru.sla.clarify.entity.chat.Commit as DomainCommit

@optics
@Immutable
sealed interface Commit {
  val key: String
  val source: DomainCommit

  @optics
  @Immutable
  data class Message(
    override val key: String,
    override val source: DomainCommit.Message,
    override val text: String,
    val side: Side,
    val shape: Shape,
    val time: String,
    val sender: Sender?,
    val replyCommit: Reply?,
    val edited: Boolean,
    val selected: Boolean
  ) : Commit, Textual {

    @optics
    @Immutable
    data class Reply(
      val targetId: DomainCommit.Id,
      val author: TextRef?,
      val text: String
    ) {
      companion object
    }

    @optics
    @Immutable
    data class Sender(
      val id: UserId,
      val name: String
    ) {
      companion object
    }

    @optics
    @Immutable
    sealed interface Side {
      companion object

      @Immutable
      data object Left : Side

      @optics
      @Immutable
      data class Right(val status: ReadStatus? = null) : Side {
        companion object
      }
    }

    @Immutable
    enum class ReadStatus {
      Sending,
      Sent,
      Read
    }

    @Immutable
    enum class Shape {
      Top,
      Middle,
      Bottom
    }

    companion object
  }

  @optics
  @Immutable
  data class InviteMember(
    override val key: String,
    override val source: DomainCommit.InviteMember,
    val text: TextRef
  ) : Commit {
    companion object
  }

  companion object
}
