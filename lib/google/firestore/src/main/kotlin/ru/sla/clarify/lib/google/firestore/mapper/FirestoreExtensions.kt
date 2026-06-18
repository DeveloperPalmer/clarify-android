package ru.sla.clarify.lib.google.firestore.mapper

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import ru.sla.clarify.lib.google.firestore.FirestoreChange
import ru.sla.clarify.lib.google.firestore.codec.codec
import ru.sla.clarify.lib.google.firestore.codec.decodeFromSnapshot
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult

internal inline fun <reified T> QuerySnapshot?.mapDocumentChanges(
  metadataChanges: MetadataChanges = MetadataChanges.EXCLUDE,
  trackPendingWrites: Boolean = false
): List<FirestoreChange<T>> {
  return this
    ?.getDocumentChanges(metadataChanges)
    ?.map { it.toFirestoreChange<T>(trackPendingWrites) }
    .orEmpty()
}

private inline fun <reified T> DocumentChange.toFirestoreChange(
  trackPendingWrites: Boolean
): FirestoreChange<T> {
  return FirestoreChange(
    data = codec.decodeFromSnapshot<T>(document),
    changeType = type.toFirestoreDocumentResult(),
    hasPendingWrites = trackPendingWrites && document.metadata.hasPendingWrites()
  )
}

internal fun DocumentChange.Type.toFirestoreDocumentResult(): FirestoreDocumentResult {
  return when (this) {
    DocumentChange.Type.ADDED -> FirestoreDocumentResult.Added
    DocumentChange.Type.MODIFIED -> FirestoreDocumentResult.Modified
    DocumentChange.Type.REMOVED -> FirestoreDocumentResult.Removed
  }
}
