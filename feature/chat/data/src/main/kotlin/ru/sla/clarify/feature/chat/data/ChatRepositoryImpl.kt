@file:Suppress("IgnoredReturnValue")

package ru.sla.clarify.feature.chat.data

import com.squareup.anvil.annotations.ContributesBinding
import com.tencent.imsdk.v2.V2TIMAdvancedMsgListener
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.imsdk.v2.V2TIMConversationListener
import com.tencent.imsdk.v2.V2TIMConversationResult
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMMessage
import com.tencent.imsdk.v2.V2TIMSendCallback
import com.tencent.imsdk.v2.V2TIMValueCallback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.data.mapper.toDomain
import ru.sla.clarify.feature.chat.domain.ChatRepository
import ru.sla.clarify.feature.chat.domain.di.ChatScope
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage
import ru.sla.clarify.feature.chat.domain.entity.Conversation
import ru.sla.log.log
import javax.inject.Inject
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.resume as resumeContinuation

@SingleIn(ChatScope::class)
@ContributesBinding(ChatScope::class)
class ChatRepositoryImpl @Inject constructor() : ChatRepository {

  override fun observeConversations(): Flow<List<Conversation>> = callbackFlow {
    val state = ConversationState()
    val listener = object : V2TIMConversationListener() {
      override fun onNewConversation(conversationList: List<V2TIMConversation>) {
        state.upsertAll(conversationList)
        trySend(state.snapshot())
      }

      override fun onConversationChanged(conversationList: List<V2TIMConversation>) {
        state.upsertAll(conversationList)
        trySend(state.snapshot())
      }

      override fun onConversationDeleted(conversationIds: List<String>) {
        state.removeByIds(conversationIds)
        trySend(state.snapshot())
      }
    }
    V2TIMManager.getConversationManager().addConversationListener(listener)

    val initial = try {
      fetchAllRawConversations()
    } catch (sdkError: ChatSdkException) {
      log { "Chat: failed to load initial conversations: $sdkError" }
      emptyList()
    }
    state.upsertAll(initial)
    trySend(state.snapshot())

    awaitClose {
      V2TIMManager.getConversationManager().removeConversationListener(listener)
    }
  }

  override suspend fun loadConversations(): List<Conversation> {
    return fetchAllRawConversations().mapNotNull { it.toDomain() }
  }

  override fun observeMessages(peerUserId: String): Flow<ChatMessage> = callbackFlow {
    val listener = object : V2TIMAdvancedMsgListener() {
      override fun onRecvNewMessage(msg: V2TIMMessage) {
        val domain = msg.toDomain() ?: return
        if (!domain.isSelf && domain.peerUserId == peerUserId) {
          trySend(domain)
        }
      }
    }
    V2TIMManager.getMessageManager().addAdvancedMsgListener(listener)
    awaitClose {
      V2TIMManager.getMessageManager().removeAdvancedMsgListener(listener)
    }
  }

  override suspend fun loadHistory(
    peerUserId: String,
    before: ChatMessage?,
    count: Int
  ): List<ChatMessage> {
    val anchor: V2TIMMessage? = before?.msgId?.takeIf { it.isNotEmpty() }?.let { findRawMessage(it) }
    val raw = suspendCancellableCoroutine<List<V2TIMMessage>> { cont ->
      V2TIMManager.getMessageManager().getC2CHistoryMessageList(
        peerUserId,
        count,
        anchor,
        object : V2TIMValueCallback<List<V2TIMMessage>> {
          override fun onSuccess(value: List<V2TIMMessage>) {
            cont.resumeContinuation(value)
          }
          override fun onError(code: Int, desc: String?) {
            cont.resumeWithException(ChatSdkException(code, desc))
          }
        }
      )
    }
    return raw.mapNotNull { it.toDomain() }
  }

  override suspend fun sendText(peerUserId: String, text: String): ChatMessage {
    val outgoing = V2TIMManager.getMessageManager().createTextMessage(text)
    return suspendCancellableCoroutine { cont ->
      V2TIMManager.getMessageManager().sendMessage(
        outgoing,
        peerUserId,
        null,
        V2TIMMessage.V2TIM_PRIORITY_NORMAL,
        false,
        null,
        object : V2TIMSendCallback<V2TIMMessage> {
          override fun onSuccess(value: V2TIMMessage) {
            cont.resumeContinuation(value.toDomain() ?: fallbackOutgoing(value, peerUserId, text))
          }

          override fun onError(code: Int, desc: String?) {
            cont.resumeWithException(ChatSdkException(code, desc))
          }

          override fun onProgress(progress: Int) = Unit
        }
      )
    }
  }

  override fun getCurrentUserId(): String? {
    return V2TIMManager.getInstance().loginUser?.takeIf { it.isNotEmpty() }
  }

  override suspend fun markConversationRead(peerUserId: String) {
    suspendCancellableCoroutine { cont ->
      V2TIMManager.getMessageManager().markC2CMessageAsRead(
        peerUserId,
        object : V2TIMCallback {
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

  private suspend fun fetchAllRawConversations(): List<V2TIMConversation> {
    val collected = mutableListOf<V2TIMConversation>()
    var nextSeq = 0L
    while (true) {
      val page = suspendCancellableCoroutine<V2TIMConversationResult> { cont ->
        V2TIMManager.getConversationManager().getConversationList(
          nextSeq,
          CONVERSATION_PAGE_SIZE,
          object : V2TIMValueCallback<V2TIMConversationResult> {
            override fun onSuccess(value: V2TIMConversationResult) {
              cont.resumeContinuation(value)
            }
            override fun onError(code: Int, desc: String?) {
              cont.resumeWithException(ChatSdkException(code, desc))
            }
          }
        )
      }
      page.conversationList.orEmpty().forEach { collected.add(it) }
      if (page.isFinished) break
      nextSeq = page.nextSeq
    }
    return collected
  }

  private suspend fun findRawMessage(msgId: String): V2TIMMessage? {
    return suspendCancellableCoroutine { cont ->
      V2TIMManager.getMessageManager().findMessages(
        listOf(msgId),
        object : V2TIMValueCallback<List<V2TIMMessage>> {
          override fun onSuccess(value: List<V2TIMMessage>) {
            cont.resumeContinuation(value.firstOrNull())
          }
          override fun onError(code: Int, desc: String?) {
            cont.resumeContinuation(null)
          }
        }
      )
    }
  }
}

private const val CONVERSATION_PAGE_SIZE = 100

private fun fallbackOutgoing(raw: V2TIMMessage, peerUserId: String, text: String): ChatMessage {
  return ChatMessage(
    msgId = raw.msgID.orEmpty(),
    peerUserId = peerUserId,
    senderId = raw.sender.orEmpty(),
    text = text,
    timestamp = System.currentTimeMillis(),
    isSelf = true,
    status = ChatMessage.Status.Sent
  )
}

private class ConversationState {
  // Keyed by conversation id (e.g. "c2c_<peerId>") so removals from the SDK can be applied directly.
  private val byConversationId = LinkedHashMap<String, Conversation>()

  fun upsertAll(list: List<V2TIMConversation>) {
    list.forEach { raw ->
      val id = raw.conversationID ?: return@forEach
      val domain = raw.toDomain()
      if (domain != null) {
        byConversationId[id] = domain
      } else {
        byConversationId.remove(id)
      }
    }
  }

  fun removeByIds(ids: Collection<String>) {
    ids.forEach { byConversationId.remove(it) }
  }

  fun snapshot(): List<Conversation> {
    return byConversationId.values.sortedByDescending { it.lastMessageTimestamp }
  }
}
