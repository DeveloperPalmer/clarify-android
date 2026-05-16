package ru.sla.clarify.lib.google.firestore.mapper

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QuerySnapshot

internal fun MutableMap<String, Any>.putIfNotNull(
  key: String,
  value: Any?
): MutableMap<String, Any> {
  if (value != null) {
    put(key, value)
  }
  return this
}

internal fun <T> QuerySnapshot?.mapChanges(transform: (DocumentChange) -> T): List<T> {
  return this
    ?.documentChanges
    .orEmpty()
    .map(transform)
}
