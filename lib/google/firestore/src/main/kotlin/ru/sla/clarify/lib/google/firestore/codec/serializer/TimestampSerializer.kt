package ru.sla.clarify.lib.google.firestore.codec.serializer

import com.google.firebase.Timestamp
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object TimestampSerializer : KSerializer<Timestamp> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
    Timestamp::class.java.name,
    PrimitiveKind.STRING
  )

  override fun serialize(encoder: Encoder, value: Timestamp) {
    val firestore = encoder.firestoreOpaqueEncoder()
    firestore.encodeOpaque(value)
  }

  override fun deserialize(decoder: Decoder): Timestamp {
    val firestoreDecoder = decoder.firestoreOpaqueDecoder()
    val value = firestoreDecoder.decodeOpaque()
    return value as? Timestamp ?: throw SerializationException(
      "expected Timestamp, got ${value?.let { it::class.simpleName } ?: "null"}"
    )
  }
}
