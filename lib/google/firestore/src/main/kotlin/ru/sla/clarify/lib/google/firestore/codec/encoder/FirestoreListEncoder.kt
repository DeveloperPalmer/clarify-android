package ru.sla.clarify.lib.google.firestore.codec.encoder

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import ru.sla.clarify.lib.google.firestore.codec.FirestoreOpaqueEncoder

/**
 * Encoder для List-структур: значения append'ятся в [target] по порядку. Имена
 * элементов из SerialDescriptor списка ("0", "1", ...) нам не нужны — порядок и так
 * сохраняется через последовательные вызовы encode*Element.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class FirestoreListEncoder(
  override val serializersModule: SerializersModule,
  private val target: MutableList<Any?>
) : AbstractEncoder(), FirestoreOpaqueEncoder {

  override fun encodeValue(value: Any) {
    target.add(value)
  }

  override fun encodeNull() {
    target.add(null)
  }

  override fun encodeOpaque(value: Any) {
    target.add(value)
  }

  override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
    target.add(enumDescriptor.getElementName(index))
  }

  override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
    return when (descriptor.kind) {
      StructureKind.CLASS, StructureKind.OBJECT -> {
        FirestoreMapEncoder(
          serializersModule = serializersModule,
          target = mutableMapOf<String, Any?>().also { target.add(it) }
        )
      }
      StructureKind.LIST -> {
        FirestoreListEncoder(
          serializersModule = serializersModule,
          target = mutableListOf<Any?>().also { target.add(it) }
        )
      }
      else -> error("Unsupported nested kind in Firestore list: ${descriptor.kind}")
    }
  }
}
