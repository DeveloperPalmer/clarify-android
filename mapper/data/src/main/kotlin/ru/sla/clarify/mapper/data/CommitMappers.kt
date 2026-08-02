package ru.sla.clarify.mapper.data

import ru.sla.clarify.core.domain.date.TIME_FORMATTER_HOUR_MINUTE
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.database.chat.ChatCommit
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.write.LastCommitParams
import ru.sla.clarify.lib.google.firestore.toEpochNanos
import ru.sla.clarify.lib.google.firestore.toTimestamp
import ru.sla.resourcerefs.TextRef
import ru.sla.resourcerefs.resRef
import ru.sla.resourcerefs.strRef
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Suppress("LongParameterList") // сигнатура строки ChatCommit
fun mapToCommit(
  id: Commit.Id,
  senderId: UserId,
  type: String,
  text: String,
  invitedId: UserId?,
  createdAtNanos: Long,
  isSelf: Boolean,
  status: String,
  editedAtNanos: Long?
): Commit {
  val localTimestamp = (createdAtNanos / NANOS_PER_MILLI).toLocalDateTime()

  return when (Commit.Type.fromValue(type)) {
    Commit.Type.Text -> {
      Commit.Message(
        id = id,
        senderId = senderId,
        text = text,
        timestamp = localTimestamp,
        isSelf = isSelf,
        status = Commit.Status.fromValue(status),
        editedAt = editedAtNanos?.let { (it / NANOS_PER_MILLI).toLocalDateTime() }
      )
    }
    Commit.Type.InviteMember -> {
      Commit.InviteMember(
        id = id,
        senderId = senderId,
        timestamp = localTimestamp,
        isSelf = isSelf,
        status = Commit.Status.fromValue(status),
        invitedId = invitedId ?: UserId("")
      )
    }
  }
}

/**
 * Обратна [mapToCommit]: Firestore-[CommitNM] в строку кэша. Единый источник истины для
 * отображения NM -> ChatCommit, общего для всех репозиториев тредов (direct/group/branch), так что
 * поля вроде [ChatCommit.createdAtNanos] задаются ровно в одном месте.
 */
fun CommitNM.toDomainModel(
  conversationId: Conversation.Id,
  selfUserId: UserId,
  hasPendingWrites: Boolean
): ChatCommit {
  return ChatCommit(
    id = Commit.Id(id),
    conversationId = conversationId,
    branchId = Branch.Id(branchId),
    senderId = UserId(senderUid),
    type = type.value,
    text = text.orEmpty(),
    invitedId = invitedUid?.let(::UserId),
    createdAtNanos = createdAt?.toEpochNanos() ?: 0L,
    isSelf = senderUid == selfUserId.value,
    status = if (hasPendingWrites) Commit.Status.Sending.value else Commit.Status.Sent.value,
    editedAtNanos = editedAt?.toEpochNanos()
  )
}

fun formatLastCommitTimestamp(epochSeconds: Long?): TextRef? {
  if (epochSeconds == null || epochSeconds <= 0L) return null
  val zone = ZoneId.systemDefault()
  val dateTime = Instant.ofEpochSecond(epochSeconds).atZone(zone).toLocalDateTime()
  val date = dateTime.toLocalDate()
  val today = LocalDate.now(zone)
  return when (date) {
    today -> strRef(dateTime.format(TIME_FORMATTER_HOUR_MINUTE))
    today.minusDays(1) -> resRef(R.string.yesterday)
    else -> strRef(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
  }
}

fun Commit.withReadStatus(peerLastReadAt: LocalDateTime?): Commit {
  if (this !is Commit.Message || !isSelf) {
    return this
  }
  if (status != Commit.Status.Sent || peerLastReadAt == null) {
    return this
  }
  return if (timestamp.isAfter(peerLastReadAt)) this else copy(status = Commit.Status.Read)
}

/**
 * Что сделать с денормализованным `lastCommit*` после удаления [deletedIds]: если удалили текущее последнее сообщение
 * — Переставить на новое последнее оставшееся ([LastCommitParams.Replace]);
 * — Очистить, если сообщений не осталось ([LastCommitParams.Clear]);
 * — Не трогать ([LastCommitParams.Keep]).
 */
fun List<Commit>.lastCommitWriteAfterDeleting(deletedIds: Set<Commit.Id>): LastCommitParams {
  val messages = filterIsInstance<Commit.Message>()
  val currentNewest = messages
    .maxWithOrNull(newestCommitOrderComparator)
  val remainingNewest = messages
    .filterNot { it.id in deletedIds }
    .maxWithOrNull(newestCommitOrderComparator)
  return when {
    currentNewest == null || currentNewest.id !in deletedIds -> {
      LastCommitParams.Keep
    }
    remainingNewest == null -> {
      LastCommitParams.Clear
    }
    else -> {
      LastCommitParams.Replace(
        text = remainingNewest.text,
        senderUid = remainingNewest.senderId.value,
        at = remainingNewest.timestamp.toTimestamp()
      )
    }
  }
}

/**
 * Сколько из удалённых сообщений всё ещё «висит» в счётчике непрочитанного собеседника — это
 * наши сообщения ([Commit.Message.isSelf]), отправленные позже отметки прочтения собеседника
 * [peerLastReadAt] (`null` — собеседник ещё ничего не читал, значит все наши непрочитаны).
 */
fun List<Commit>.unreadDelta(
  deletedIds: Set<Commit.Id>,
  peerLastReadAt: LocalDateTime?
): Int = count { commit ->
  listOf(
    commit.id in deletedIds,
    commit is Commit.Message && commit.isSelf,
    (peerLastReadAt == null || commit.timestamp.isAfter(peerLastReadAt))
  ).all { it }
}

private val newestCommitOrderComparator = compareBy<Commit.Message>(
  { it.timestamp },
  { it.id.value }
)

private const val NANOS_PER_MILLI = 1_000_000L
