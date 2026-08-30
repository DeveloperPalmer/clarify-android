package ru.sla.clarify.feature.chronology.data.mapper

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.mapper.data.mapToCommit

/**
 * Строка ленты вместе с веткой, которой она принадлежит.
 *
 * Существует потому, что графу нужны ленты **всех** веток разом, а `mapToCommit` ветку не носит:
 * ленту читают по одной ветке, и там она известна вызывающему. Здесь известна не она, а беседа, —
 * поэтому ветка приходит из строки и остаётся при коммите до группировки.
 *
 * @return пара «ветка — коммит», готовая к группировке по ветке
 */
@Suppress("LongParameterList") // сигнатура строки ChatCommit
internal fun mapToBranchCommit(
  id: Commit.Id,
  branchId: Branch.Id,
  senderId: UserId,
  type: String,
  text: String,
  replyCommit: Commit.Reply?,
  invitedId: UserId?,
  createdAtNanos: Long,
  isSelf: Boolean,
  status: String,
  editedAtNanos: Long?
): Pair<Branch.Id, Commit> {
  return branchId to mapToCommit(
    id = id,
    senderId = senderId,
    type = type,
    text = text,
    replyCommit = replyCommit,
    invitedId = invitedId,
    createdAtNanos = createdAtNanos,
    isSelf = isSelf,
    status = status,
    editedAtNanos = editedAtNanos
  )
}
