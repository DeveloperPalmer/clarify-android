package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable

/**
 * Содержимое плашки эпизода, собранное из ленты.
 *
 * Всё уже приведено к тому виду, в котором рисуется: время — строкой, доля — числом от нуля до
 * единицы. Форматировать по дороге к плашке нечего, и это ровно то, что обещает [EpisodeContent].
 *
 * @param time время начала эпизода — время его первого сообщения
 * @param count число сообщений в кластере
 * @param snippet текст последнего сообщения эпизода
 * @param myShare доля своих реплик в кластере
 * @param unreadCount сколько сообщений эпизода осталось непрочитанными
 * @param dim эпизод внутри слитой ветки: рисуется приглушённым
 */
@Immutable
internal data class GraphEpisode(
  override val time: String,
  override val count: Int,
  override val snippet: String,
  override val myShare: Float,
  override val unreadCount: Long,
  override val dim: Boolean
) : EpisodeContent
