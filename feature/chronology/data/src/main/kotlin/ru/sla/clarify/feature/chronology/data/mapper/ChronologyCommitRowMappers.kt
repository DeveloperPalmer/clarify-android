package ru.sla.clarify.feature.chronology.data.mapper

import ru.sla.clarify.database.entity.ChronologyCommitRow
import ru.sla.clarify.database.entity.CommitRow
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.mapper.data.toDomainModel

/**
 * Строка ленты вместе с веткой, которой она принадлежит.
 *
 * Существует потому, что графу нужны ленты **всех** веток разом, а [CommitRow] ветку не носит:
 * ленту читают по одной ветке, и там она известна вызывающему. Здесь известна не она, а беседа, —
 * поэтому ветка приходит из строки и остаётся при коммите до группировки.
 *
 * @return пара «ветка — коммит», готовая к группировке по ветке
 */
internal fun ChronologyCommitRow.toBranchCommit(): Pair<Branch.Id, Commit> {
  val commit = CommitRow(
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
  return branchId to commit.toDomainModel()
}
