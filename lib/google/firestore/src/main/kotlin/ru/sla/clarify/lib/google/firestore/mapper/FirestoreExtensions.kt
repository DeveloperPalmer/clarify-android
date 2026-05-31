package ru.sla.clarify.lib.google.firestore.mapper

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult

internal fun <T> QuerySnapshot?.mapDocumentChanges(
  metadataChanges: MetadataChanges = MetadataChanges.EXCLUDE,
  transform: (DocumentChange) -> T
): List<T> {
  return this
    ?.getDocumentChanges(metadataChanges)
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
