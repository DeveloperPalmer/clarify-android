package ru.sla.clarify.feature.chronology.domain.entity

import androidx.compose.runtime.Immutable
import ru.sla.clarify.entity.chat.Member

/**
 * История беседы целиком: всё, из чего собирается граф хронологии.
 *
 * Одной сущностью, а не тремя потоками, ровно по той причине, по которой полотну нельзя подменять
 * узлы отдельно от веток: дорожки, цвета и точки слияния выводятся из веток **и** коммитов сразу, и
 * снимок, собранный из веток одного кадра и коммитов другого, дал бы раскладку по чужим индексам —
 * молча.
 *
 * @param trunk магистраль: у неё нет ни имени, ни merge request, поэтому она и не в [branches]
 * @param branches ветки беседы в порядке ветвления — в этом же порядке идёт раскраска дорожек
 * @param members участники беседы, включая себя: из них берутся имя и фото автора сообщения
 */
@Immutable
data class ChronologyHistory(
  val trunk: TrunkHistory,
  val branches: List<BranchHistory>,
  val members: List<Member>
)
