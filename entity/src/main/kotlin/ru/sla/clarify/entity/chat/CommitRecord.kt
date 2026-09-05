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
  val type: Commit.Type,
  val text: String?,
  val invitedId: UserId? = null,
  val replyCommit: Reply? = null,
  val createdAtNanos: Long,
  val editedAtNanos: Long? = null,
  val isPending: Boolean = false
) {

  data class Reply(
    val id: Commit.Id,
    val senderId: UserId,
    val text: String
  )
}
