package ru.sla.clarify.lib.google.firestore.mapper

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import ru.sla.clarify.entity.chat.ChatChange
import ru.sla.clarify.lib.google.firestore.codec.codec
import ru.sla.clarify.lib.google.firestore.codec.decodeFromSnapshot

/**
 * Читает изменения снапшота как [NM] и сразу отдаёт их доменными: за границу модуля
 * транспортный тип не выходит, поэтому декодирование и отображение стоят рядом.
 */
internal inline fun <reified NM, R> QuerySnapshot?.mapDocumentChanges(
  metadataChanges: MetadataChanges = MetadataChanges.EXCLUDE,
  trackPendingWrites: Boolean = false,
  transform: (NM) -> R
): List<ChatChange<R>> {
  return this
    ?.getDocumentChanges(metadataChanges)
    ?.map { it.toDomainModel(trackPendingWrites, transform) }
    .orEmpty()
}

private inline fun <reified NM, R> DocumentChange.toDomainModel(
  trackPendingWrites: Boolean,
  transform: (NM) -> R
): ChatChange<R> {
  return ChatChange(
    data = transform(codec.decodeFromSnapshot<NM>(document)),
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
