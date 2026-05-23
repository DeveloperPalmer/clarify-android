package ru.sla.clarify.lib.google.firestore.codec.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.sla.clarify.core.domain.entity.UserId

object UserIdSerializer : KSerializer<UserId> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
    UserId::class.java.name,
    PrimitiveKind.STRING
  )

  override fun serialize(encoder: Encoder, value: UserId) {
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): UserId {
    return UserId(decoder.decodeString())
  }
}
