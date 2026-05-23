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
 * Decoder, читающий поля по именам из `Map<String, Any?>` — формат представления
 * Firestore-документа. Для каждого вложенного structure/list создаёт sub-decoder.
 *
 * Контракт: top-level value обязан быть структурой. Optional-поля (`val x: T = ...`)
 * пропускаются если ключа нет в source. Required-поля при отсутствии →
 * [ru.sla.clarify.lib.google.firestore.codec.DataMappingException].
 */
@OptIn(ExperimentalSerializationApi::class)
internal class FirestoreMapDecoder(
  override val serializersModule: SerializersModule,
  private val source: Map<String, Any?>
) : AbstractDecoder(), FirestoreOpaqueDecoder {

  private var elementIndex = 0
  private var pendingKey: String? = null

  override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
    while (elementIndex < descriptor.elementsCount) {
      val name = descriptor.getElementName(elementIndex)
      val present = source.containsKey(name)
      val isOptional = descriptor.isElementOptional(elementIndex)
      if (present || !isOptional) {
        pendingKey = name
        return elementIndex++
      }
      // Optional + отсутствует → пропускаем, kotlinx.serialization подставит дефолт.
      elementIndex++
    }
    return CompositeDecoder.DECODE_DONE
  }

  override fun decodeValue(): Any {
    val key = requirePending()
    return source[key] ?: throw DataMappingException.missingField(key)
  }

  override fun decodeNotNullMark(): Boolean {
    val key = requirePending()
    return source[key] != null
  }

  override fun decodeNull(): Nothing? {
    return null
  }

  override fun decodeOpaque(): Any? {
    return source[requirePending()]
  }

  override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
    val key = requirePending()
    val raw = source[key] as? String ?: throw DataMappingException.wrongType(
      key = key,
      expected = "String",
      actual = source[key]
    )

    val index = (0 until enumDescriptor.elementsCount).firstOrNull {
      enumDescriptor.getElementName(it) == raw
    }

    return index ?: throw DataMappingException.unknownEnumValue(enumDescriptor.serialName, raw)
  }

  override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
    return when (descriptor.kind) {
      StructureKind.CLASS, StructureKind.OBJECT -> {
        FirestoreMapDecoder(
          serializersModule = serializersModule,
          source = pendingKey?.let { key -> readNestedMap(key) } ?: source
        )
      }
      StructureKind.LIST -> {
        val key = requirePending()
        val raw = source[key]
        FirestoreListDecoder(
          serializersModule = serializersModule,
          source = raw as? List<*> ?: throw DataMappingException.wrongType(
            key = key,
            expected = "List",
            actual = raw
          )
        )
      }
      else -> error("Unsupported structure kind for Firestore decoding: ${descriptor.kind}")
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun readNestedMap(key: String): Map<String, Any?> {
    val raw = source[key]
    return raw as? Map<String, Any?> ?: throw DataMappingException.wrongType(
      key = key,
      expected = "Map",
      actual = raw
    )
  }

  private fun requirePending(): String {
    return pendingKey ?: throw DataMappingException.rootMustBeStructure()
  }
}
