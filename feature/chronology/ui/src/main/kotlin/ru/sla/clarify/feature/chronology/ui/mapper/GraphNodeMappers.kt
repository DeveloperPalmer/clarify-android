package ru.sla.clarify.feature.chronology.ui.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode

/**
 * Связная подпись узла для скринридера — §14 брифа.
 *
 * Граф — самый враждебный скринридеру вид интерфейса: рёбра он не читает вовсе, и вся структура
 * обязана уместиться в подпись самого узла. Поэтому подпись собирается здесь, а не в компонентах:
 * формулировка у девяти узлов одна, а знает её только экран — имя ветки в узел не приходит и
 * приходить не должно, это содержимое, а не то, что узел рисует.
 *
 * Части перечисляются в порядке убывания важности: чем узел является, сколько в нём чего, когда это
 * было, где это лежит и что осталось непрочитанным.
 *
 * @param branchName имя ветки, которой принадлежит узел; `null` у магистрали
 * @return готовая к озвучиванию строка
 */
@Composable
internal fun GraphNode.toDescription(branchName: String?): String {
  val branch = branchName?.let { stringResource(R.string.chronology_node_branch, it) }
  return when (this) {
    is GraphNode.Episode -> nodeDescriptionOf(
      listOf(
        stringResource(R.string.chronology_node_episode),
        pluralStringResource(R.plurals.chronology_episode_messages_count, count, count),
        time,
        branch,
        unreadCount.takeIf { it > 0 }?.let { unread ->
          pluralStringResource(R.plurals.chronology_node_unread_count, unread.toInt(), unread.toInt())
        }
      )
    )
    is GraphNode.Fork -> nodeDescriptionOf(
      listOf(stringResource(R.string.chronology_node_fork), branch)
    )
    is GraphNode.Merge -> nodeDescriptionOf(
      listOf(stringResource(R.string.chronology_node_merge), branch)
    )
    // Фронт — единственный узел, у которого нет ни данных, ни ветки: он и есть «сейчас».
    is GraphNode.Front -> stringResource(R.string.chronology_front_caption)
  }
}

/**
 * Собирает подпись из частей, пропуская те, которых нет.
 *
 * Чистая половина маппера, и заведена она не ради красоты: `@Composable`-функция юнит-тестом не
 * покрывается — Robolectric в проекте нет, — а проверять здесь надо ровно одно: часть, которой нет,
 * в подпись не попадает. Иначе скринридер прочитает «Эпизод, , 6 мар» или «ветка null».
 *
 * @param parts части подписи в порядке озвучивания; `null` и пустые пропускаются
 * @return части через запятую
 */
internal fun nodeDescriptionOf(parts: List<String?>): String {
  return parts.filterNot { it.isNullOrBlank() }.joinToString(separator = ", ")
}
