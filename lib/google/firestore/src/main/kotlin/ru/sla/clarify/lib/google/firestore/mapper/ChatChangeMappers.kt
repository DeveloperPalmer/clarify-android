package ru.sla.clarify.lib.google.firestore.mapper

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import ru.sla.clarify.entity.chat.ChatChange
import ru.sla.clarify.lib.google.firestore.codec.codec
import ru.sla.clarify.lib.google.firestore.codec.decodeFromSnapshot

internal inline fun <reified T> QuerySnapshot?.mapDocumentChanges(
  metadataChanges: MetadataChanges = MetadataChanges.EXCLUDE,
  trackPendingWrites: Boolean = false
): List<ChatChange<T>> {
  return this
    ?.getDocumentChanges(metadataChanges)
    ?.map { it.toDomainModel<T>(trackPendingWrites) }
    .orEmpty()
}

private inline fun <reified T> DocumentChange.toDomainModel(
  trackPendingWrites: Boolean
): ChatChange<T> {
  return ChatChange(
    data = codec.decodeFromSnapshot<T>(document),
    changeType = type.toDomainModel(),
    isPending = trackPendingWrites && document.metadata.hasPendingWrites()
  )
}

internal fun DocumentChange.Type.toDomainModel(): ChatChange.Type {
  return when (this) {
    DocumentChange.Type.ADDED -> ChatChange.Type.Added
    DocumentChange.Type.MODIFIED -> ChatChange.Type.Modified
    DocumentChange.Type.REMOVED -> ChatChange.Type.Removed
  }
}
