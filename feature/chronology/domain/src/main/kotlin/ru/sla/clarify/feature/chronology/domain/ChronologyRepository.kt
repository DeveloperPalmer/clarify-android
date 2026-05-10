package ru.sla.clarify.feature.chronology.domain

import kotlinx.coroutines.flow.Flow

interface ChronologyRepository {
  fun graphForPeer(peerId: String): Flow<ChronologyGraph>
}
