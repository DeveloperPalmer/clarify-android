package ru.sla.clarify.lib.google.firestore.entity

import com.google.firebase.Timestamp

/**
 * Precise pagination cursor for the commits collection. Pairs the exact server [createdAt]
 * (full sub-millisecond precision, unlike the millisecond value cached in SQLite) with the
 * document [id] used as an ordering tie-break, so `startAfter(createdAt, id)` never skips nor
 * duplicates commits that share the same timestamp.
 */
data class CommitCursor(
  val id: String,
  val createdAt: Timestamp
)
