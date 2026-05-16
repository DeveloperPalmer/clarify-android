package ru.sla.clarify.feature.chat.thread.data.mapper

import ru.sla.clarify.feature.entity.chat.Commit
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

object ThreadMappers {
  @Suppress("LongParameterList")
  fun mapToCommit(
    id: Commit.Id,
    parentId: Commit.Id?,
    peerId: String,
    senderId: String,
    text: String,
    colorHex: String,
    timestamp: Long,
    isSelf: Boolean,
    status: String
  ): Commit {
    val timestamp = Instant.ofEpochSecond(timestamp)
      .atZone(ZoneId.systemDefault())
      .toLocalDateTime()
    return Commit.Message(
      id = id,
      parentId = parentId,
      peerId = peerId,
      senderId = senderId,
      text = text,
      colorHex = colorHex,
      timestamp = timestamp,
      isSelf = isSelf,
      status = Commit.Status.entries.firstOrNull { it.name == status } ?: Commit.Status.Sent
    )
  }
}

internal fun generateColorHex(): String {
  val rgb = Random.nextInt(0x1000000)
  return "#$OPAQUE_ALPHA_HEX${rgb.toString(radix = 16).padStart(6, '0').uppercase()}"
}

const val MILLIS_PER_SECOND = 1000L
private const val OPAQUE_ALPHA_HEX = "FF"
