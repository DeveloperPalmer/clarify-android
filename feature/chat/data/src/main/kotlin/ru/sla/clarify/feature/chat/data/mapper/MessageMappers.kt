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
    colorHex: String?,
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

internal fun V2TIMMessage.previewText(): String? {
  return when (elemType) {
    V2TIMMessage.V2TIM_ELEM_TYPE_TEXT -> textElem?.text
    V2TIMMessage.V2TIM_ELEM_TYPE_CUSTOM -> customElem?.data?.decodeToString()?.toCustomMessageText()
    else -> null
  }
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

internal fun String.toCustomMessageText(): String? {
  return runCatching {
    org.json.JSONObject(this).optString("text").takeIf { it.isNotBlank() }
  }.getOrNull()
}

internal data class MessageContent(
  val text: String,
  val colorHex: String?
)

internal fun createColoredTextPayload(
  text: String,
  colorHex: String
): ByteArray {
  return JSONObject()
    .put(CUSTOM_MESSAGE_TYPE_KEY, CUSTOM_MESSAGE_TYPE)
    .put(CUSTOM_MESSAGE_TEXT_KEY, text)
    .put(CUSTOM_MESSAGE_COLOR_HEX_KEY, colorHex)
    .toString()
    .toByteArray(Charsets.UTF_8)
}

internal fun V2TIMMessage.content(
  fallbackText: String?,
  fallbackColorHex: String?
): MessageContent? {
  val customContent = customElem
    ?.data
    ?.decodeToString()
    ?.toCustomMessageContent()
  if (customContent != null) {
    return customContent
  }
  val text = textElem?.text ?: fallbackText ?: return null
  return MessageContent(
    text = text,
    colorHex = fallbackColorHex
  )
}

internal fun String.toCustomMessageContent(): MessageContent? {
  return runCatching {
    val json = JSONObject(this)
    val type = json.optString(CUSTOM_MESSAGE_TYPE_KEY).takeIf { it.isNotBlank() }
    if (type != null && type != CUSTOM_MESSAGE_TYPE) {
      return@runCatching null
    }
    val text = json.optString(CUSTOM_MESSAGE_TEXT_KEY).takeIf { it.isNotBlank() }
      ?: return@runCatching null
    val colorHex = json.optString(CUSTOM_MESSAGE_COLOR_HEX_KEY).takeIf { it.isNotBlank() }
      ?: json.optString(CUSTOM_MESSAGE_COLOR_KEY).takeIf { it.isNotBlank() }
    MessageContent(
      text = text,
      colorHex = colorHex
    )
  }.getOrNull()
}

internal fun randomMessageColorHex(): String {
  val rgb = Random.nextInt(0x1000000)
  return "#$OPAQUE_ALPHA_HEX${rgb.toString(radix = 16).padStart(6, '0').uppercase()}"
}

const val MILLIS_PER_SECOND = 1000L
private const val CUSTOM_MESSAGE_TYPE = "colored_text"
private const val CUSTOM_MESSAGE_TYPE_KEY = "type"
private const val CUSTOM_MESSAGE_TEXT_KEY = "text"
private const val CUSTOM_MESSAGE_COLOR_HEX_KEY = "colorHex"
private const val CUSTOM_MESSAGE_COLOR_KEY = "color"
private const val OPAQUE_ALPHA_HEX = "FF"
