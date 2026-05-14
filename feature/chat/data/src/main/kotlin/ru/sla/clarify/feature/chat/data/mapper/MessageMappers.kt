package ru.sla.clarify.feature.chat.data.mapper

import com.tencent.imsdk.v2.V2TIMMessage
import org.json.JSONObject
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random
import ru.sla.clarify.database.chat.ChatMessage as ChatMessageDB
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage as ChatMessageDomain

object MessageMappers {
  fun mapToChatMessage(
    id: ChatMessage.Id,
    parentId: ChatMessage.Id?,
    peerId: String,
    senderId: String,
    text: String,
    colorHex: String,
    timestamp: Long,
    isSelf: Long,
    status: String
  ): ChatMessageDomain {
    return ChatMessageDB(
      id = id,
      parentId = parentId,
      peerId = peerId,
      senderId = senderId,
      text = text,
      colorHex = colorHex,
      timestamp = timestamp,
      isSelf = isSelf,
      status = status
    ).toDomain()
  }
}

internal fun ChatMessageDB.toDomain(): ChatMessageDomain {
  val timestamp = Instant.ofEpochSecond(timestamp)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()
  return ChatMessageDomain(
    id = id,
    parentId = parentId,
    peerId = peerId,
    senderId = senderId,
    text = text,
    colorHex = colorHex,
    timestamp = timestamp,
    isSelf = isSelf != 0L,
    status = status.toMessageStatus()
  )
}

fun mapStatus(rawStatus: Int): ChatMessageDomain.Status {
  return when (rawStatus) {
    V2TIMMessage.V2TIM_MSG_STATUS_SENDING -> ChatMessageDomain.Status.Sending
    V2TIMMessage.V2TIM_MSG_STATUS_SEND_SUCC -> ChatMessageDomain.Status.Sent
    V2TIMMessage.V2TIM_MSG_STATUS_SEND_FAIL -> ChatMessageDomain.Status.Failed
    else -> ChatMessageDomain.Status.Sent
  }
}

private fun String.toMessageStatus(): ChatMessageDomain.Status {
  return ChatMessageDomain.Status.entries.firstOrNull { it.name == this } ?: ChatMessageDomain.Status.Sent
}

internal data class CustomMessagePayload(
  val text: String,
  val colorHex: String,
  val parentId: String?
)

internal fun createCustomMessagePayload(
  text: String,
  parentMessage: ChatMessage?
): ByteArray {
  return JSONObject()
    .put(CUSTOM_MESSAGE_TEXT_KEY, text)
    .put(CUSTOM_MESSAGE_COLOR_HEX_KEY, parentMessage?.colorHex ?: generateColorHex())
    .tryPut(CUSTOM_MESSAGE_PARENT_ID_KEY, parentMessage?.id?.value)
    .toString()
    .toByteArray(Charsets.UTF_8)
}

internal fun String.toCustomMessagePayload(): CustomMessagePayload? {
  return runCatching {
    val json = JSONObject(this)
    CustomMessagePayload(
      text = json.getOptString(CUSTOM_MESSAGE_TEXT_KEY) ?: return null,
      parentId = json.getOptString(CUSTOM_MESSAGE_PARENT_ID_KEY),
      colorHex = json.getOptString(CUSTOM_MESSAGE_COLOR_HEX_KEY) ?: return null
    )
  }.getOrNull()
}

private fun JSONObject.getOptString(key: String): String? {
  return optString(key).takeIf { it.isNotBlank() }
}

private fun JSONObject.tryPut(key: String, value: Any?): JSONObject {
  if (value != null) {
    put(key, value)
  }
  return this
}

internal fun generateColorHex(): String {
  val rgb = Random.nextInt(0x1000000)
  return "#$OPAQUE_ALPHA_HEX${rgb.toString(radix = 16).padStart(6, '0').uppercase()}"
}

const val MILLIS_PER_SECOND = 1000L
private const val OPAQUE_ALPHA_HEX = "FF"

private const val CUSTOM_MESSAGE_TEXT_KEY = "custom_message_text_key"
private const val CUSTOM_MESSAGE_PARENT_ID_KEY = "custom_message_parent_id_key"
private const val CUSTOM_MESSAGE_COLOR_HEX_KEY = "custom_message_color_hex_key"
