package ru.sla.clarify.feature.chronology.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.feature.chronology.domain.ChronologyRepository
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Заглушка: реальные источники подключаются следующим шагом.
 *
 * Модуль и его DI-привязка заведены заранее, чтобы gradle- и DI-обвязка фичи прошла ревью
 * один раз, а не дважды. Пока экран рисует захардкоженный граф.
 */
@SingleIn(ChronologyScope::class)
@ContributesBinding(ChronologyScope::class)
class ChronologyRepositoryImpl @Inject constructor() : ChronologyRepository {

  override val branches: Flow<List<Branch>> = flowOf(emptyList())
}
