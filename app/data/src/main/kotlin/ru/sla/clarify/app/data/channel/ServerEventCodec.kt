package ru.sla.clarify.app.data.channel

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.sla.clarify.app.data.entity.DecodedFrame
import ru.sla.clarify.app.data.entity.SequencedEvent
import ru.sla.clarify.app.data.entity.ServerException
import ru.sla.clarify.app.data.entity.ServerFrame
import ru.sla.log.LogPriority
import ru.sla.log.log

/**
 * Разбирает кадр канала в два шага: сначала кадр и номера, потом сами события — по одному.
 *
 * Незнакомый тип события пропускается, знакомый, но не разобравшийся, — роняет кадр. Разница
 * намеренная: первое означает, что сервер обновился раньше клиента, и это норма; второе — что
 * копия спеки разошлась с сервером по форме, и молчать об этом нельзя.
 *
 * Класс обобщён по типу события, потому что тип событий генерируется по спеке: знать его
 * устройство кодеку незачем, ему хватает сериализатора.
 */
class ServerEventCodec<E>(
  private val json: Json,
  private val serializer: DeserializationStrategy<E>
) {

  private val knownTypes: Set<String> = serializer.descriptor.readDiscriminatorValues()
  private val discriminator: String = serializer.descriptor.readDiscriminatorName(json)

  fun decode(text: String): DecodedFrame<E> {
    val frame = decodeFrame(text)
    val events = mutableListOf<SequencedEvent<E>>()
    val skipped = mutableListOf<String>()
    frame.events.forEach { raw ->
      val type = raw.event.readDiscriminator(discriminator)
      if (type != null && type in knownTypes) {
        events += SequencedEvent(raw.seq, decodeEvent(raw.event))
      } else {
        val named = type ?: "<none>"
        skipped += named
        log(LogPriority.Info) { "skipping event of unknown type $named at seq ${raw.seq}" }
      }
    }
    return DecodedFrame(
      resyncRequired = frame.resyncRequired,
      resyncFromSeq = frame.resyncFromSeq,
      events = events,
      skippedTypes = skipped,
      firstSeq = frame.events.firstOrNull()?.seq,
      lastSeq = frame.events.lastOrNull()?.seq
    )
  }

  private fun decodeFrame(text: String): ServerFrame {
    return try {
      json.decodeFromString(ServerFrame.serializer(), text)
    } catch (e: SerializationException) {
      throw ServerException.Malformed(e)
    }
  }

  private fun decodeEvent(element: JsonElement): E {
    return try {
      json.decodeFromJsonElement(serializer, element)
    } catch (e: SerializationException) {
      throw ServerException.Malformed(e)
    }
  }
}

/**
 * Значения дискриминатора, известные этому сериализатору.
 *
 * Берутся у самого сериализатора, а не перечисляются рядом руками: список, который пишут руками,
 * расходится с генерацией на первом же новом событии, и расходится молча.
 *
 * Форма дескриптора проверена тестом: у sealed-иерархии второй элемент — контекстный, и его
 * `elementNames` и есть значения `@SerialName` подклассов.
 */
private fun SerialDescriptor.readDiscriminatorValues(): Set<String> {
  if (elementsCount < 2) return emptySet()
  return getElementDescriptor(1).elementNames.toSet()
}

private fun SerialDescriptor.readDiscriminatorName(json: Json): String {
  val declared = annotations.filterIsInstance<JsonClassDiscriminator>().firstOrNull()
  return declared?.discriminator ?: json.configuration.classDiscriminator
}

private fun JsonElement.readDiscriminator(name: String): String? {
  val obj = this as? JsonObject ?: return null
  return obj[name]?.jsonPrimitive?.content
}
