package ru.sla.clarify.lib.google.firestore.codec.serializer

import com.google.firebase.firestore.FieldValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.sla.clarify.lib.google.firestore.codec.sentinel.Delete

object DeleteSerializer : KSerializer<Delete> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
    Delete::class.java.name,
    PrimitiveKind.STRING
  )

  override fun serialize(encoder: Encoder, value: Delete) {
    val firestore = encoder.firestoreOpaqueEncoder()
    firestore.encodeOpaque(FieldValue.delete())
  }

  override fun deserialize(decoder: Decoder): Delete {
    throw SerializationException(
      "Delete is a write-only sentinel — it cannot be decoded from Firestore snapshot"
    )
  }
}
