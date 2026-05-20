package ru.sla.clarify.lib.google.firestore.mapper

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QuerySnapshot

internal fun <T> QuerySnapshot?.mapChanges(transform: (DocumentChange) -> T): List<T> {
  return this
    ?.documentChanges
    .orEmpty()
    .map(transform)
}
