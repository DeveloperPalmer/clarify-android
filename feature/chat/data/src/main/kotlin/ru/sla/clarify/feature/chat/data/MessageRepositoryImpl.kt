package ru.sla.clarify.feature.chat.data

import com.squareup.anvil.annotations.ContributesBinding
import com.tencent.imsdk.v2.V2TIMAdvancedMsgListener
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMMessage
import com.tencent.imsdk.v2.V2TIMSendCallback
import com.tencent.imsdk.v2.V2TIMValueCallback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.feature.chat.data.mapper.MILLIS_PER_SECOND
import ru.sla.clarify.feature.chat.data.mapper.MessageMappers
import ru.sla.clarify.feature.chat.data.mapper.content
import ru.sla.clarify.feature.chat.data.mapper.createColoredTextPayload
import ru.sla.clarify.feature.chat.data.mapper.mapStatus
import ru.sla.clarify.feature.chat.data.mapper.randomMessageColorHex
import ru.sla.clarify.feature.chat.domain.MessageRepository
import ru.sla.clarify.feature.chat.domain.di.ChatScope
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage
import ru.sla.clarify.feature.chat.domain.entity.ChatSdkException
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.resume as resumeContinuation

@SingleIn(ChatScope::class)
@ContributesBinding(ChatScope::class)
class MessageRepositoryImpl @Inject constructor(
  private val inMemoryDB: InMemoryDB
) : MessageRepository {

  private val messageManager = V2TIMManager.getMessageManager()

  override fun peerMessages(peerId: String): Flow<ChatMessage> {
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

  override suspend fun history(
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

  override suspend fun send(
    text: String,
    peerId: String,
    parentId: ChatMessage.Id?
  ): ChatMessage {
    val colorHex = randomMessageColorHex()
    val outgoing = messageManager.createCustomMessage(
      createColoredTextPayload(
        text = text,
        colorHex = colorHex
      )
    )
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
              colorHex = colorHex,
              peerId = peerId,
              message = value
            )
            val message = inMemoryDB.messageQueries
              .selectById(ChatMessage.Id(value.msgID), MessageMappers::mapToChatMessage)
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

  override suspend fun markAsRead(peerId: String) {
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

  private fun selectMessageById(id: ChatMessage.Id): ChatMessage {
    return inMemoryDB.messageQueries
      .selectById(id, MessageMappers::mapToChatMessage)
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
        mapper = MessageMappers::mapToChatMessage
      )
    } else {
      inMemoryDB.messageQueries.selectByPeer(
        peerId = peerId,
        messageLimit = count.toLong(),
        mapper = MessageMappers::mapToChatMessage
      )
    }
    return query.executeAsList()
  }

  private fun insertOrReplaceMessage(
    message: V2TIMMessage,
    peerId: String,
    text: String? = null,
    colorHex: String? = null
  ) {
    if (message.msgID.isBlank()) return
    val messageContent = message.content(
      fallbackText = text,
      fallbackColorHex = colorHex
    ) ?: return
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
        text = messageContent.text,
        colorHex = messageContent.colorHex,
        timestamp = message.timestamp * MILLIS_PER_SECOND,
        isSelf = if (message.isSelf) 1L else 0L,
        status = mapStatus(message.status).name
      )
    }
  }
}
