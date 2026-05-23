package ru.sla.clarify.lib.google.firestore.codec.serializer

import com.google.firebase.firestore.FieldValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ServerTimestamp

/**
 * Сериализатор для [ServerTimestamp]: подменяет значение на
 * `FieldValue.serverTimestamp()` опаково. Decode запрещён — sentinel write-only,
 * из снэпшота он никогда не приходит.
 */
object ServerTimestampSerializer : KSerializer<ServerTimestamp> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
    ServerTimestamp::class.java.name,
    PrimitiveKind.STRING
  )

  override fun serialize(encoder: Encoder, value: ServerTimestamp) {
    val firestore = encoder.firestoreOpaqueEncoder()
    firestore.encodeOpaque(FieldValue.serverTimestamp())
  }

  override fun deserialize(decoder: Decoder): ServerTimestamp {
    throw SerializationException(
      "ServerTimestamp is a write-only sentinel — it cannot be decoded from Firestore snapshot"
    )
  }
}
