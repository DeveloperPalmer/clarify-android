package ru.sla.clarify.feature.chronology.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.feature.chat.direct.thread.domain.DirectThreadRepository
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Граф беседы для экрана хронологии.
 *
 * Подписку на изменения веток модель не поднимает: хронология открывается только из личного треда,
 * а `subscribeOnBranchesChanges` уже запущена моделью этого треда. Второй слушатель дал бы лишний
 * трафик и расхождение состояний между чатом и графом.
 *
 * Коммиты сюда ещё не заведены: репозиторий читает ленту по одной ветке, а графу нужны все ветки
 * разом.
 */
@SingleIn(ChronologyScope::class)
class ChronologyModel @Inject constructor(
  directThreadRepository: DirectThreadRepository,
  @ForScope(ChronologyScope::class) parentScope: CoroutineScope
) : ReactiveModel(parentScope) {

  val branches: Flow<List<Branch>> = directThreadRepository.branches
}
