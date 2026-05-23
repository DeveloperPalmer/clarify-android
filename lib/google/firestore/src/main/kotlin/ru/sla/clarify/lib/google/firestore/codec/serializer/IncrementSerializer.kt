package ru.sla.clarify.lib.google.firestore.codec.serializer

import com.google.firebase.firestore.FieldValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.sla.clarify.lib.google.firestore.codec.sentinel.Increment

object IncrementSerializer : KSerializer<Increment> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
    Increment::class.java.name,
    PrimitiveKind.STRING
  )

  override fun serialize(encoder: Encoder, value: Increment) {
    val firestore = encoder.firestoreOpaqueEncoder()
    firestore.encodeOpaque(FieldValue.increment(value.value))
  }

  override fun deserialize(decoder: Decoder): Increment {
    throw SerializationException(
      "Increment is a write-only sentinel — it cannot be decoded from Firestore snapshot"
    )
  }
}
