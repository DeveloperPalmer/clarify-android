package ru.sla.clarify.lib.google.firestore

import com.google.firebase.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * Имена коллекций и тех полей документа, к которым нужно обращаться **снаружи**
 * NM — query whereEqualTo/orderBy, dot-path update'ы и денормализационные чтения
 * через `snapshot.getString/getLong(...)`. Поля, имена которых фигурируют только
 * внутри NM (`UserNM.email`, `MergeRequestNM.status`, ...), сюда не входят —
 * single source of truth там @Serializable property name (или @SerialName).
 *
 * Sentinel-поля (`updatedAt`, `serverCreatedAt`, ...) тоже не нужны здесь как
 * константы — они живут как обычные типизированные поля внутри `*Params`
 * (`ServerTimestamp`, `Delete`, `Increment`); подменяются на `FieldValue.*`
 * через свои атомарные @Contextual-сериализаторы в [FirestoreFormat].
 */
object FirestoreSchema {
  const val USERS_COLLECTION = "users"
  const val MEMBERS_COLLECTION = "members"
  const val CONVERSATIONS_COLLECTION = "conversations"
  const val COMMITS_COLLECTION = "commits"
  const val UNREAD_COMMITS_COLLECTION = "unreadCommits"
  const val BRANCHES_COLLECTION = "branches"

  // Поле документа unreadCommits/{uid} — нужно для getLong-чтения в listener'е.
  // NM для документа смысла не имеет — в нём ровно одно поле, обращаемся точечно.
  const val UNREAD_COMMITS_COUNT = "count"

  // usersQuery whereEqualTo(USER_EMAIL, ...) — точечный query.
  const val USER_EMAIL = "email"

  // conversationsQuery whereEqualTo/whereArrayContains.
  const val CONVERSATION_TYPE = "type"
  const val CONVERSATION_MEMBER_UIDS = "memberUids"

  // commitsCollectionRef-query: orderBy + whereLessThan + whereEqualTo по branchId,
  // whereArrayContains по visibleFor.
  const val COMMIT_CREATED_AT = "createdAt"
  const val COMMIT_BRANCH_ID = "branchId"
  const val COMMIT_VISIBLE_FOR = "visibleFor"

  const val BRANCH_MERGE_REQUEST = "mergeRequest"
}

fun Timestamp.toEpochSeconds(): Long {
  return seconds
}

fun Timestamp.toEpochMillis(): Long {
  return toDate().time
}

/** Collapse the (seconds, nanoseconds) pair into a single nanoseconds-since-epoch value. */
fun Timestamp.toEpochNanos(): Long {
  return seconds * NANOS_PER_SECOND + nanoseconds
}

/** Inverse of [toEpochNanos]: split nanoseconds-since-epoch back into a [Timestamp]. */
fun Long.epochNanosToTimestamp(): Timestamp {
  return Timestamp(this / NANOS_PER_SECOND, (this % NANOS_PER_SECOND).toInt())
}

fun LocalDateTime.toTimestamp(): Timestamp {
  val instant = atZone(ZoneId.systemDefault())
    .toInstant()
  return Timestamp(Date.from(instant))
}

private const val NANOS_PER_SECOND = 1_000_000_000L
