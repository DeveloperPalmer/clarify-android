package ru.sla.clarify.lib.google.firestore.codec.encoder

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import ru.sla.clarify.lib.google.firestore.codec.DataMappingException
import ru.sla.clarify.lib.google.firestore.codec.FirestoreOpaqueEncoder

/**
 * Encoder, который складывает значения в плоскую `MutableMap<String, Any?>` — формат
 * представления Firestore-документа. Для каждого вложенного structure/list создаёт
 * sub-encoder, дописывающий в собственный sub-map / sub-list. Сам по себе является
 * также CompositeEncoder (через [AbstractEncoder]) — kotlinx.serialization рассматривает
 * top-level encoder и его composite-форму как один объект.
 *
 * Контракт: top-level value обязан быть структурой (class/object). Примитивы и списки
 * на корне не поддерживаются — это нарушение Firestore-модели, документ всегда map.
 */
internal class FirestoreMapEncoder(
  override val serializersModule: SerializersModule,
  private val target: MutableMap<String, Any?>
) : AbstractEncoder(), FirestoreOpaqueEncoder {

  // Имя элемента (поля) для следующего encodeValue/encodeNull/beginStructure.
  // null означает что encoder ещё на корне, и beginStructure должен писать прямо в
  // target (не создавать sub-map).
  private var pendingKey: String? = null

  override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
    pendingKey = descriptor.getElementName(index)
    return true
  }

  override fun encodeValue(value: Any) {
    target[requirePending()] = value
  }

  override fun encodeNull() {
    target[requirePending()] = null
  }

  override fun encodeOpaque(value: Any) {
    target[requirePending()] = value
  }

  override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
    // Имя варианта enum'а после применения @SerialName.
    target[requirePending()] = enumDescriptor.getElementName(index)
  }

  override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
    return when (descriptor.kind) {
      StructureKind.CLASS, StructureKind.OBJECT -> {
        FirestoreMapEncoder(
          serializersModule = serializersModule,
          target = pendingKey
            ?.let { key -> mutableMapOf<String, Any?>().also { target[key] = it } }
            ?: target // на корне пишем напрямую в target
        )
      }
      StructureKind.LIST -> {
        FirestoreListEncoder(
          serializersModule = serializersModule,
          target = mutableListOf<Any?>().also { target[requirePending()] = it }
        )
      }
      else -> error("Unsupported structure kind for Firestore encoding: ${descriptor.kind}")
    }
  }

  private fun requirePending(): String {
    return pendingKey ?: throw DataMappingException.rootMustBeStructure()
  }
}
