package ru.sla.clarify.entity.chat

import ru.sla.clarify.core.domain.entity.UserId

/**
 * Сообщение таким, каким его отдаёт источник истины. В отличие от [Commit], который собирается
 * для экрана и знает про «своё/чужое» и про прочтение собеседником, здесь только пришедшее
 * снаружи: отправитель как есть, время в наносекундах эпохи и признак неподтверждённой записи.
 *
 * [isPending] — запись ещё не подтверждена сервером: локальное оптимистичное эхо, отданное
 * потоком раньше ответа.
 */
data class CommitRecord(
  val id: Commit.Id,
  val branchId: Branch.Id,
  val senderId: UserId,
  val type: Type,
  val text: String?,
  val invitedId: UserId? = null,
  val replyCommit: Reply? = null,
  val createdAtNanos: Long,
  val editedAtNanos: Long? = null,
  val isPending: Boolean = false
) {

  /**
   * Вид сообщения отдельным полем: запись плоская, а [Commit] различает вид подтипами
   * и в дискриминаторе не нуждается.
   */
  enum class Type(val value: String) {
    Text("text"),
    InviteMember("inviteMember");

    companion object {
      fun fromValue(value: String): Type {
        return entries.firstOrNull { it.value == value } ?: error("unexpected commit type: $value")
      }
    }
  }

  data class Reply(
    val id: Commit.Id,
    val senderId: UserId,
    val text: String
  )
}
