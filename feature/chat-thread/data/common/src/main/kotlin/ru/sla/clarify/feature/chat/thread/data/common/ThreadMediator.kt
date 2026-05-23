package ru.sla.clarify.feature.chat.thread.data.common

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.feature.chat.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.ConversationType
import ru.sla.clarify.lib.google.firestore.entity.FirestoreConversation
import javax.inject.Inject

@SingleIn(ThreadScope::class)
class ThreadMediator @Inject constructor(
  private val peerId: Peer.Id,
  private val inMemoryDB: InMemoryDB,
  private val authSessionPersistence: AuthSessionPersistence
) {

  suspend fun awaitConversationId(): FirestoreConversation.Id {
    conversationId()?.let { return it }

    val firstAppearance = inMemoryDB.chatConversationQueries
      .selectIdByParticipants(
        type = ConversationType.Direct.value,
        participantUids = directParticipantIds()
      )
      .observeOneOrNull()
      .filterNotNull()
      .first()
    return FirestoreConversation.Id(firstAppearance)
  }

  suspend fun conversationId(): FirestoreConversation.Id? {
    val participants = directParticipantIds()
    return inMemoryDB.chatConversationQueries
      .selectIdByParticipants(
        type = ConversationType.Direct.value,
        participantUids = participants
      )
      .executeAsOneOrNull()
      ?.let(FirestoreConversation::Id)
  }

  suspend fun requireConversationId(): FirestoreConversation.Id {
    return requireNotNull(conversationId()) {
      "conversationId is null. A branch can only be created for an existing conversation."
    }
  }

  suspend fun directParticipantIds(): List<String> {
    return setOf(requireUserId().value, peerId.value).sorted()
  }

  suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) })
  }
}
