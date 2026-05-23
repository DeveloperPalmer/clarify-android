package ru.sla.clarify.lib.google.firestore.codec.serializer

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.sla.clarify.lib.google.firestore.codec.FirestoreOpaqueDecoder
import ru.sla.clarify.lib.google.firestore.codec.FirestoreOpaqueEncoder

/**
 * Хелперы для опаковых @Contextual-сериализаторов: каст encoder/decoder в
 * Firestore-aware форму. Используются из всех сериализаторов в этом пакете —
 * сообщение в исключении намеренно общее, чтобы не врать про конкретный тип.
 */
internal fun Encoder.firestoreOpaqueEncoder(): FirestoreOpaqueEncoder {
  return this as? FirestoreOpaqueEncoder ?: throw SerializationException(
    "Opaque encoding requires FirestoreFormat — got ${this::class.simpleName ?: "null"}"
  )
}

internal fun Decoder.firestoreOpaqueDecoder(): FirestoreOpaqueDecoder {
  return this as? FirestoreOpaqueDecoder ?: throw SerializationException(
    "Opaque decoding requires FirestoreFormat — got ${this::class.simpleName ?: "null"}"
  )
}
