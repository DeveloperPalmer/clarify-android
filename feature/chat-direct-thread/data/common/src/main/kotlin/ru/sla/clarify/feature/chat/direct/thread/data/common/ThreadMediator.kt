package ru.sla.clarify.feature.chat.direct.thread.data.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.TargetParams
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM.Type
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(DirectThreadScope::class)
class ThreadMediator @Inject constructor(
  params: TargetParams,
  private val inMemoryDB: InMemoryDB,
  private val authSessionPersistence: AuthSessionPersistence
) {
  private val peerId = params.peerId

  suspend fun awaitConversationId(): String {
    conversationId()?.let { return it }

    return inMemoryDB.chatConversationQueries
      .selectIdByMembers(
        type = Type.Direct.value,
        memberUids = directMemberIds()
      )
      .observeOneOrNull()
      .filterNotNull()
      .first()
  }

  suspend fun conversationId(): String? {
    val members = directMemberIds()
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatConversationQueries
        .selectIdByMembers(
          type = Type.Direct.value,
          memberUids = members
        )
        .executeAsOneOrNull()
    }
  }

  suspend fun requireConversationId(): String {
    return requireNotNull(conversationId()) {
      "conversationId is null. A branch can only be created for an existing conversation."
    }
  }

  suspend fun directMemberIds(): List<String> {
    return setOf(requireUserId().value, peerId.value).sorted()
  }

  suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) })
  }
}
