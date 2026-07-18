package ru.sla.clarify.lib.google.firestore.entity

import com.google.firebase.Timestamp

/**
 * Точный курсор пагинации для коллекции commits. Пара из точного серверного [createdAt]
 * (полная наносекундная точность) и документного [id], который служит тай-брейком сортировки,
 * так что `startAfter(createdAt, id)` никогда не пропускает и не дублирует коммиты с одинаковым
 * временем.
 */
data class CommitCursor(
  val id: String,
  val createdAt: Timestamp
)
