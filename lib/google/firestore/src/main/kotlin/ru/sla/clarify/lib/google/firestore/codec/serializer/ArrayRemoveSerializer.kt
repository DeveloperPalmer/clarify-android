package ru.sla.clarify.lib.google.firestore.codec.serializer

import com.google.firebase.firestore.FieldValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ArrayRemove

object ArrayRemoveSerializer : KSerializer<ArrayRemove> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
    ArrayRemove::class.java.name,
    PrimitiveKind.STRING
  )

  // FieldValue.arrayRemove принимает только vararg — spread неизбежен;
  // массив крошечный (обычно один uid) и путь записи не горячий
  @Suppress("SpreadOperator")
  override fun serialize(encoder: Encoder, value: ArrayRemove) {
    val firestore = encoder.firestoreOpaqueEncoder()
    firestore.encodeOpaque(FieldValue.arrayRemove(*value.values.toTypedArray()))
  }

  override fun deserialize(decoder: Decoder): ArrayRemove {
    throw SerializationException(
      "ArrayRemove is a write-only sentinel — it cannot be decoded from Firestore snapshot"
    )
  }
}
