package ru.sla.clarify.feature.chat.direct.thread.data.entity

import ru.sla.clarify.entity.chat.LastCommitUpdate

/** Денормализованные поля для удаления «у всех», посчитанные по полному кэшу до удаления. */
internal data class DeleteForEveryoneWrite(
  val lastCommit: LastCommitUpdate,
  val peerUnreadDelta: Int
)
