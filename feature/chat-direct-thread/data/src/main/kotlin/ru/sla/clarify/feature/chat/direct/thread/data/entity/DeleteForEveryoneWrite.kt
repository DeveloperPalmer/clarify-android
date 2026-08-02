package ru.sla.clarify.feature.chat.direct.thread.data.entity

import ru.sla.clarify.lib.google.firestore.entity.write.LastCommitParams

/** Денормализованные поля для удаления «у всех», посчитанные по полному кэшу до удаления. */
internal data class DeleteForEveryoneWrite(
  val lastCommit: LastCommitParams,
  val peerUnreadDelta: Int
)
