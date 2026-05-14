package ru.sla.clarify.feature.chat.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.squareup.anvil.annotations.ContributesBinding
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.imsdk.v2.V2TIMConversationListener
import com.tencent.imsdk.v2.V2TIMConversationResult
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMValueCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.feature.chat.data.mapper.ConversationMappers
import ru.sla.clarify.feature.chat.data.mapper.MILLIS_PER_SECOND
import ru.sla.clarify.feature.chat.data.mapper.previewText
import ru.sla.clarify.feature.chat.domain.ConversationRepository
import ru.sla.clarify.feature.chat.domain.di.ChatScope
import ru.sla.clarify.feature.chat.domain.entity.ChatSdkException
import ru.sla.clarify.feature.chat.domain.entity.Conversation
import ru.sla.log.log
import javax.inject.Inject
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.resume as resumeContinuation

@SingleIn(ChatScope::class)
@ContributesBinding(ChatScope::class)
class ConversationRepositoryImpl @Inject constructor(
  private val inMemoryDB: InMemoryDB
) : ConversationRepository {

  private val conversationManager = V2TIMManager.getConversationManager()

  override val conversations: Flow<List<Conversation>> = inMemoryDB.conversationQueries
    .selectAll(ConversationMappers::mapToConversation)
    .asFlow()
    .mapToList(Dispatchers.IO)

  override fun getCurrentUserId(): String? {
    return V2TIMManager.getInstance().loginUser?.takeIf { it.isNotEmpty() }
  }

  override fun subscribeOnConversations(): Flow<Unit> {
    return callbackFlow {
      val listener = object : V2TIMConversationListener() {
        override fun onNewConversation(conversationList: List<V2TIMConversation>) {
          conversationList.forEach(::saveConversation)
        }
        override fun onConversationChanged(conversationList: List<V2TIMConversation>) {
          conversationList.forEach(::saveConversation)
        }
        override fun onConversationDeleted(conversationIds: List<String>) {
          conversationIds.forEach(::deleteConversationById)
        }
      }
      try {
        fetchAllConversations()
      } catch (sdkError: ChatSdkException) {
        log { "Chat: failed to load initial conversations: $sdkError" }
      }

      conversationManager.addConversationListener(listener)
      awaitClose { conversationManager.removeConversationListener(listener) }
    }
  }

  override suspend fun deleteConversation(id: Conversation.Id) {
    return suspendCancellableCoroutine { cont ->
      V2TIMManager.getConversationManager().deleteConversation(
        /* id */ id.value,
        /* listener */ object : V2TIMCallback {
          override fun onSuccess() {
            cont.resumeContinuation(Unit)
          }
          override fun onError(code: Int, desc: String?) {
            cont.resumeWithException(ChatSdkException(code, desc))
          }
        }
      )
    }
  }

  private suspend fun fetchAllConversations() {
    val collected = mutableListOf<V2TIMConversation>()
    var nextSeq = 0L
    while (true) {
      val conversationResult = getConversationResultByPage(nextSeq)
      val result = conversationResult.conversationList.orEmpty()
      collected.addAll(result)
      if (conversationResult.isFinished) break
      nextSeq = conversationResult.nextSeq
    }
    collected.forEach(::saveConversation)
  }

  private suspend fun getConversationResultByPage(page: Long): V2TIMConversationResult {
    return suspendCancellableCoroutine { cont ->
      V2TIMManager.getConversationManager().getConversationList(
        /* page */ page,
        /* pageSize */ CONVERSATION_PAGE_SIZE,
        /* listener */ object : V2TIMValueCallback<V2TIMConversationResult> {
          override fun onSuccess(result: V2TIMConversationResult) {
            cont.resumeContinuation(result)
          }
          override fun onError(code: Int, description: String?) {
            cont.resumeWithException(ChatSdkException(code, description))
          }
        }
      )
    }
  }

  private fun deleteConversationById(id: String) {
    inMemoryDB.conversationQueries.delete(id)
  }

  private fun saveConversation(item: V2TIMConversation) {
    if (item.type != V2TIMConversation.V2TIM_C2C) {
      return
    }
    inMemoryDB.transaction {
      inMemoryDB.peerQueries.insertOrReplace(
        id = item.userID,
        name = item.showName,
        faceUrl = item.faceUrl
      )
      inMemoryDB.conversationQueries.insertOrReplace(
        id = item.conversationID,
        peerId = item.userID,
        unreadCount = item.unreadCount.toLong(),
        lastMessage = item.lastMessage?.previewText(),
        lastMessageTimestamp = item.lastMessage?.timestamp?.let { it * MILLIS_PER_SECOND } ?: 0L
      )
    }
  }
}

private const val CONVERSATION_PAGE_SIZE = 100
