package ru.sla.clarify.lib.google.firestore.codec.serializer

import com.google.firebase.firestore.FieldValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ArrayUnion

object ArrayUnionSerializer : KSerializer<ArrayUnion> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
    ArrayUnion::class.java.name,
    PrimitiveKind.STRING
  )

  // FieldValue.arrayUnion принимает только vararg — spread неизбежен;
  // массив крошечный (обычно один uid) и путь записи не горячий
  @Suppress("SpreadOperator")
  override fun serialize(encoder: Encoder, value: ArrayUnion) {
    val firestore = encoder.firestoreOpaqueEncoder()
    firestore.encodeOpaque(FieldValue.arrayUnion(*value.values.toTypedArray()))
  }

  override fun deserialize(decoder: Decoder): ArrayUnion {
    throw SerializationException(
      "ArrayUnion is a write-only sentinel — it cannot be decoded from Firestore snapshot"
    )
  }
}
