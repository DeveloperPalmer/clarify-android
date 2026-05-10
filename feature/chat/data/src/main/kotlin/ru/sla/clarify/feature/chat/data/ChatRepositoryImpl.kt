@file:Suppress("IgnoredReturnValue")

package ru.sla.clarify.feature.chat.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.feature.chat.data.mapper.MILLIS_PER_SECOND
import ru.sla.clarify.feature.chat.data.mapper.Mappers
import ru.sla.clarify.feature.chat.data.mapper.mapStatus
import ru.sla.clarify.feature.chat.data.mapper.previewText
import ru.sla.clarify.feature.chat.domain.ChatRepository
import ru.sla.clarify.feature.chat.domain.di.ChatScope
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage
import ru.sla.clarify.feature.chat.domain.entity.ChatSdkException
import ru.sla.clarify.feature.chat.domain.entity.Conversation
import ru.sla.log.log
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.resume as resumeContinuation

@SingleIn(ChatScope::class)
@ContributesBinding(ChatScope::class)
class ChatRepositoryImpl @Inject constructor(
  private val inMemoryDB: InMemoryDB
) : ChatRepository {

  private val messageManager = V2TIMManager.getMessageManager()
  private val conversationManager = V2TIMManager.getConversationManager()

  override val conversations: Flow<List<Conversation>> = inMemoryDB.conversationQueries
    .selectAll(Mappers::mapToConversation)
    .asFlow()
    .mapToList(Dispatchers.IO)

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
        val initial = getAllConversations()
        initial.forEach(::saveConversation)
      } catch (sdkError: ChatSdkException) {
        log { "Chat: failed to load initial conversations: $sdkError" }
      }

      conversationManager.addConversationListener(listener)
      awaitClose { conversationManager.removeConversationListener(listener) }
    }
  }

  override fun treadMessages(peerId: String): Flow<ChatMessage> {
    return callbackFlow {
      val listener = object : V2TIMAdvancedMsgListener() {
        override fun onRecvNewMessage(msg: V2TIMMessage) {
          insertOrReplaceMessage(
            message = msg,
            peerId = peerId
          )
          val message = selectMessageById(
            id = ChatMessage.Id(msg.msgID)
          )
          if (!message.isSelf && message.peerId == peerId) {
            trySend(message)
          }
        }
      }
      messageManager.addAdvancedMsgListener(listener)
      awaitClose { messageManager.removeAdvancedMsgListener(listener) }
    }
  }

  override suspend fun loadHistory(
    count: Int,
    peerId: String,
    before: ChatMessage?
  ): List<ChatMessage> {
    val anchor: V2TIMMessage? = before?.id
      ?.takeIf { it.value.isNotEmpty() }
      ?.let { findRawMessage(it) }
    val historyMessages: List<V2TIMMessage> = loadHistory(
      count = count,
      peerId = peerId,
      anchorMessage = anchor
    )
    historyMessages.forEach {
      insertOrReplaceMessage(
        message = it,
        peerId = peerId
      )
    }
    val selectMessages = selectMessagesByPeer(
      count = count,
      peerId = peerId,
      before = before
    )
    return selectMessages
  }

  private suspend fun loadHistory(
    count: Int,
    peerId: String,
    anchorMessage: V2TIMMessage?
  ): List<V2TIMMessage> {
    return suspendCancellableCoroutine { cont ->
      messageManager.getC2CHistoryMessageList(
        peerId,
        count,
        anchorMessage,
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
  }

  override suspend fun sendText(
    text: String,
    peerId: String,
    parentId: ChatMessage.Id?
  ): ChatMessage {
    val outgoing = messageManager.createTextMessage(text)
    if (!parentId?.value.isNullOrBlank()) {
      outgoing.cloudCustomData = parentId.value
    }
    return suspendCancellableCoroutine { cont ->
      messageManager.sendMessage(
        outgoing,
        peerId,
        null,
        V2TIMMessage.V2TIM_PRIORITY_NORMAL,
        false,
        null,
        object : V2TIMSendCallback<V2TIMMessage> {
          override fun onSuccess(value: V2TIMMessage) {
            insertOrReplaceMessage(
              text = text,
              peerId = peerId,
              message = value
            )
            val message = inMemoryDB.messageQueries
              .selectById(ChatMessage.Id(value.msgID), Mappers::mapToChatMessage)
              .executeAsOne()
            cont.resumeContinuation(message)
          }
          override fun onError(code: Int, desc: String?) {
            cont.resumeWithException(ChatSdkException(code, desc))
          }
          override fun onProgress(progress: Int) {
            // nothing to do
          }
        }
      )
    }
  }

  override fun getCurrentUserId(): String? {
    return V2TIMManager.getInstance().loginUser?.takeIf { it.isNotEmpty() }
  }

  override suspend fun markConversationRead(peerId: String) {
    return suspendCancellableCoroutine { cont ->
      messageManager.markC2CMessageAsRead(
        /* peerId */ peerId,
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

  private suspend fun getAllConversations(): List<V2TIMConversation> {
    val collected = mutableListOf<V2TIMConversation>()
    var nextSeq = 0L
    while (true) {
      val conversationResult = getConversationResultByPage(nextSeq)
      val result = conversationResult.conversationList.orEmpty()
      collected.addAll(result)
      if (conversationResult.isFinished) break
      nextSeq = conversationResult.nextSeq
    }
    return collected
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

  private suspend fun findRawMessage(msgId: ChatMessage.Id): V2TIMMessage? {
    return suspendCancellableCoroutine { cont ->
      V2TIMManager.getMessageManager().findMessages(
        /* msgIds */ listOf(msgId.value),
        /* listener */ object : V2TIMValueCallback<List<V2TIMMessage>> {
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

  private fun deleteConversationById(id: String) {
    inMemoryDB.conversationQueries.delete(id)
  }

  private fun selectMessageById(id: ChatMessage.Id): ChatMessage {
    return inMemoryDB.messageQueries
      .selectById(id, Mappers::mapToChatMessage)
      .executeAsOne()
  }

  private fun selectMessagesByPeer(
    peerId: String,
    before: ChatMessage?,
    count: Int
  ): List<ChatMessage> {
    val query = if (before != null) {
      inMemoryDB.messageQueries.selectByPeerBefore(
        peerId = peerId,
        timestamp = before.timestamp.toEpochSecond(ZoneOffset.UTC),
        messageLimit = count.toLong(),
        mapper = Mappers::mapToChatMessage
      )
    } else {
      inMemoryDB.messageQueries.selectByPeer(
        peerId = peerId,
        messageLimit = count.toLong(),
        mapper = Mappers::mapToChatMessage
      )
    }
    return query.executeAsList()
  }

  private fun insertOrReplaceMessage(
    message: V2TIMMessage,
    peerId: String,
    text: String? = null
  ) {
    if (message.msgID.isBlank()) return
    val messageText = message.textElem?.text ?: text ?: return
    val senderId = message.sender ?: return
    val peerId = if (message.isSelf) peerId else senderId
    val parentMsgId = message.cloudCustomData?.takeIf { it.isNotBlank() }
    inMemoryDB.transaction {
      inMemoryDB.peerQueries.insertIfAbsent(
        id = peerId,
        name = null,
        faceUrl = null
      )
      inMemoryDB.messageQueries.insertOrReplace(
        id = ChatMessage.Id(message.msgID),
        parentId = parentMsgId?.let(ChatMessage::Id),
        peerId = if (message.isSelf) peerId else senderId,
        senderId = senderId,
        text = messageText,
        timestamp = message.timestamp * MILLIS_PER_SECOND,
        isSelf = if (message.isSelf) 1L else 0L,
        status = mapStatus(message.status).name
      )
    }
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
