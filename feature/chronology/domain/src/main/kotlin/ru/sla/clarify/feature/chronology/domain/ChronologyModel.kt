package ru.sla.clarify.feature.chronology.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope
import javax.inject.Inject

@SingleIn(ChronologyScope::class)
class ChronologyModel @Inject constructor(
  private val chronologyRepository: ChronologyRepository
) : ReactiveModel() {

  fun graph(peerId: String): Flow<ChronologyGraph> {
    return chronologyRepository.graphForPeer(peerId)
  }
}
