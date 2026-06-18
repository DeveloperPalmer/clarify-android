package ru.sla.clarify.lib.google.firestore.codec

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.lib.google.firestore.codec.decoder.FirestoreMapDecoder
import ru.sla.clarify.lib.google.firestore.codec.encoder.FirestoreMapEncoder
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ArrayRemove
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ArrayUnion
import ru.sla.clarify.lib.google.firestore.codec.sentinel.Delete
import ru.sla.clarify.lib.google.firestore.codec.sentinel.Increment
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ServerTimestamp
import ru.sla.clarify.lib.google.firestore.codec.serializer.ArrayRemoveSerializer
import ru.sla.clarify.lib.google.firestore.codec.serializer.ArrayUnionSerializer
import ru.sla.clarify.lib.google.firestore.codec.serializer.DeleteSerializer
import ru.sla.clarify.lib.google.firestore.codec.serializer.IncrementSerializer
import ru.sla.clarify.lib.google.firestore.codec.serializer.ServerTimestampSerializer
import ru.sla.clarify.lib.google.firestore.codec.serializer.TimestampSerializer
import ru.sla.clarify.lib.google.firestore.codec.serializer.UserIdSerializer

class FirestoreFormat(override val serializersModule: SerializersModule) : SerialFormat {

  fun <T> encodeToMap(
    serializer: SerializationStrategy<T>,
    value: T
  ): Map<String, Any?> {
    val target = mutableMapOf<String, Any?>()
    val encoder = FirestoreMapEncoder(serializersModule, target)
    encoder.encodeSerializableValue(serializer, value)
    if (DOCUMENT_ID_FIELD in target) {
      throw DataMappingException.reservedDocumentIdField(DOCUMENT_ID_FIELD)
    }
    return target
  }

  fun <T> decodeFromMap(
    deserializer: DeserializationStrategy<T>,
    map: Map<String, Any?>
  ): T {
    val decoder = FirestoreMapDecoder(serializersModule, map)
    return decoder.decodeSerializableValue(deserializer)
  }

  fun <T> decodeFromSnapshot(
    deserializer: DeserializationStrategy<T>,
    snapshot: DocumentSnapshot
  ): T {
    val data = snapshot
      .getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      ?: throw DataMappingException.missingField("[document '${snapshot.id}'].data")
    return decodeFromMap(deserializer, data + (DOCUMENT_ID_FIELD to snapshot.id))
  }

  companion object {
    private const val DOCUMENT_ID_FIELD = "id"

    val Default: FirestoreFormat = FirestoreFormat(
      serializersModule = SerializersModule {
        contextual(Timestamp::class, TimestampSerializer)
        contextual(UserId::class, UserIdSerializer)
        contextual(ServerTimestamp::class, ServerTimestampSerializer)
        contextual(Delete::class, DeleteSerializer)
        contextual(Increment::class, IncrementSerializer)
        contextual(ArrayUnion::class, ArrayUnionSerializer)
        contextual(ArrayRemove::class, ArrayRemoveSerializer)
      }
    )
  }
}

inline fun <reified T> FirestoreFormat.encodeToMap(value: T): Map<String, Any?> {
  return encodeToMap(serializersModule.serializer<T>(), value)
}

inline fun <reified T> FirestoreFormat.decodeFromMap(map: Map<String, Any?>): T {
  return decodeFromMap(serializersModule.serializer<T>(), map)
}

inline fun <reified T> FirestoreFormat.decodeFromSnapshot(snapshot: DocumentSnapshot): T {
  return decodeFromSnapshot(serializersModule.serializer<T>(), snapshot)
}

internal val codec: FirestoreFormat = FirestoreFormat.Default
