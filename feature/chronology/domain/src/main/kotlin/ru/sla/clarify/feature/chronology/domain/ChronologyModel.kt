package ru.sla.clarify.feature.chronology.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope
import ru.sla.clarify.feature.chronology.domain.entity.ChronologyHistory
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * История беседы для экрана хронологии.
 *
 * Одним потоком, а не набором: ветки, их ленты и участники описывают один граф, и собранное из
 * разных снимков состояние дало бы раскраску дорожек по чужим индексам — молча.
 */
@SingleIn(ChronologyScope::class)
class ChronologyModel @Inject constructor(
  private val chronologyRepository: ChronologyRepository,
  @ForScope(ChronologyScope::class) parentScope: CoroutineScope
) : ReactiveModel(parentScope) {

  init {
    scope.launch { chronologyRepository.subscribeOnBranchCommits() }
  }

  val history: Flow<ChronologyHistory> = chronologyRepository.history
}
