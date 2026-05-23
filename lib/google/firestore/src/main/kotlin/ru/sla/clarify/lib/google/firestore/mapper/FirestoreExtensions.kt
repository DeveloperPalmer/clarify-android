package ru.sla.clarify.lib.google.firestore.mapper

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QuerySnapshot
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult

internal fun <T> QuerySnapshot?.mapDocumentChanges(transform: (DocumentChange) -> T): List<T> {
  return this
    ?.documentChanges
    .orEmpty()
    .map(transform)
}

internal fun DocumentChange.Type.toFirestoreDocumentResult(): FirestoreDocumentResult {
  return when (this) {
    DocumentChange.Type.ADDED -> FirestoreDocumentResult.Added
    DocumentChange.Type.MODIFIED -> FirestoreDocumentResult.Modified
    DocumentChange.Type.REMOVED -> FirestoreDocumentResult.Removed
  }
}
