package ru.sla.clarify.feature.chronology.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.entity.chat.Branch

interface ChronologyRepository {

  /**
   * Ветки беседы — из них строятся точки ветвления и слияния графа.
   *
   * Коммиты сюда ещё не заведены: сейчас репозитории читают ленту по одной ветке
   * ([ru.sla.clarify.entity.chat.Branch.id]), а графу нужны все ветки разом.
   */
  val branches: Flow<List<Branch>>
}
