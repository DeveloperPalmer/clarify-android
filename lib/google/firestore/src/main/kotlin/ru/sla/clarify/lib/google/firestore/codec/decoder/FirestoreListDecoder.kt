package ru.sla.clarify.lib.google.firestore.codec.decoder

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import ru.sla.clarify.lib.google.firestore.codec.DataMappingException
import ru.sla.clarify.lib.google.firestore.codec.FirestoreOpaqueDecoder

/**
 * Decoder для List-структур. Идём по [source] последовательно; каждый
 * `decodeElementIndex` отдаёт следующий индекс пока он в границах, иначе DECODE_DONE.
 * decodeValue / decodeOpaque / beginStructure берут текущий элемент и инкрементят
 * курсор.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class FirestoreListDecoder(
  override val serializersModule: SerializersModule,
  private val source: List<*>
) : AbstractDecoder(), FirestoreOpaqueDecoder {

  private var index = 0

  override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = source.size

  override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
    return if (index < source.size) index else CompositeDecoder.DECODE_DONE
  }

  override fun decodeValue(): Any {
    val raw = source[index++]
    return raw ?: throw DataMappingException.missingField("[${index - 1}]")
  }

  override fun decodeNotNullMark(): Boolean {
    return source[index] != null
  }

  override fun decodeNull(): Nothing? {
    // Курсор всё равно надо подвинуть — null-элемент мы пропускаем.
    index++
    return null
  }

  override fun decodeOpaque(): Any? {
    return source[index++]
  }

  override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
    val raw = source[index++] as? String ?: throw DataMappingException.wrongType(
      key = "[${index - 1}]",
      expected = "String",
      actual = source.getOrNull(index - 1)
    )
    return (0 until enumDescriptor.elementsCount)
      .firstOrNull { enumDescriptor.getElementName(it) == raw }
      ?: throw DataMappingException.unknownEnumValue(enumDescriptor.serialName, raw)
  }

  override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
    val raw = source[index++]
    return when (descriptor.kind) {
      StructureKind.CLASS, StructureKind.OBJECT -> {
        @Suppress("UNCHECKED_CAST")
        FirestoreMapDecoder(
          serializersModule = serializersModule,
          source = raw as? Map<String, Any?> ?: throw DataMappingException.wrongType(
            key = "[${index - 1}]",
            expected = "Map",
            actual = raw
          )
        )
      }
      StructureKind.LIST -> {
        FirestoreListDecoder(
          serializersModule = serializersModule,
          source = raw as? List<*> ?: throw DataMappingException.wrongType(
            key = "[${index - 1}]",
            expected = "List",
            actual = raw
          )
        )
      }
      else -> error("Unsupported nested kind in Firestore list: ${descriptor.kind}")
    }
  }
}
