package ru.sla.clarify.feature.chronology.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.feature.chat.data.mapper.Mappers
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage
import ru.sla.clarify.feature.chronology.domain.ChronologyEdge
import ru.sla.clarify.feature.chronology.domain.ChronologyGraph
import ru.sla.clarify.feature.chronology.domain.ChronologyNode
import ru.sla.clarify.feature.chronology.domain.ChronologyRepository
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope
import javax.inject.Inject

@SingleIn(ChronologyScope::class)
@ContributesBinding(ChronologyScope::class)
class ChronologyRepositoryImpl @Inject constructor(
  private val inMemoryDB: InMemoryDB
) : ChronologyRepository {

  override fun graphForPeer(peerId: String): Flow<ChronologyGraph> {
    return inMemoryDB.messageQueries
      .selectChronologyByPeer(peerId, Mappers::mapToChatMessage)
      .asFlow()
      .mapToList(Dispatchers.IO)
      .map(::buildGraph)
  }
}

internal fun buildGraph(messages: List<ChatMessage>): ChronologyGraph {
  if (messages.isEmpty()) return ChronologyGraph.Empty

  val ordered = messages.sortedBy { it.timestamp }
  val byId: Map<ChatMessage.Id, ChatMessage> = ordered.associateBy { it.id }

  // Эффективный родитель = либо явный parentId, либо предыдущее по времени сообщение
  // (так линейный диалог отрисуется как естественная цепочка, а явные ответы дадут ветки).
  val effectiveParent = HashMap<ChatMessage.Id, ChatMessage.Id?>(ordered.size)
  ordered.forEachIndexed { index, message ->
    val explicit = message.parentId
    val explicitValid = explicit != null &&
      explicit.value.isNotBlank() &&
      byId.containsKey(explicit) &&
      explicit != message.id
    effectiveParent[message.id] = when {
      explicitValid -> explicit
      index > 0 -> ordered[index - 1].id
      else -> null
    }
  }

  val depths = HashMap<ChatMessage.Id, Int>(ordered.size)
  ordered.forEach { message ->
    depths[message.id] = if (effectiveParent[message.id] == null) 0 else -1
  }
  // Несколько проходов до сходимости — на случай несортированных вставок и длинных цепей.
  var changed = true
  while (changed) {
    changed = false
    ordered.forEach { message ->
      if (depths.getValue(message.id) >= 0) return@forEach
      val parent = effectiveParent[message.id]
      val parentDepth = parent?.let { depths[it] } ?: -1
      if (parentDepth >= 0) {
        depths[message.id] = parentDepth + 1
        changed = true
      }
    }
  }
  // Зацикленные/невалидные ссылки — fallback в корни.
  ordered.forEach { message ->
    if (depths.getValue(message.id) < 0) {
      depths[message.id] = 0
    }
  }

  val nodes = ordered
    .groupBy { depths.getValue(it.id) }
    .toSortedMap()
    .flatMap { (depth, items) ->
      items
        .sortedBy { it.timestamp }
        .mapIndexed { index, message ->
          ChronologyNode(
            message = message,
            depth = depth,
            branchIndex = index
          )
        }
    }

  val edges = ordered.mapNotNull { message ->
    effectiveParent[message.id]?.let { parent ->
      ChronologyEdge(
        fromMsgId = parent.value,
        toMsgId = message.id.value
      )
    }
  }

  return ChronologyGraph(nodes = nodes, edges = edges)
}
