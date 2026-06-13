package ru.sla.clarify.feature.chat.direct.thread.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Participant
import ru.sla.clarify.feature.entity.chat.Peer
import java.time.LocalDateTime

interface ThreadRepository {
  suspend fun subscribeOnPeerChanges()
  suspend fun subscribeOnCommitChanges()

  suspend fun fetchHistoryCommits(count: Int, before: Commit? = null)
  suspend fun sendCommit(colorHex: String?, text: String)

  suspend fun markAsRead()
  suspend fun markReadUpTo(lastReadAt: LocalDateTime)

  val peer: Flow<Peer?>
  fun commits(): Flow<List<Commit>>

  val participants: Flow<List<Participant>>
  fun participant(initiator: UserId): Flow<Participant?>

  val unreadCount: Flow<Long>
}
