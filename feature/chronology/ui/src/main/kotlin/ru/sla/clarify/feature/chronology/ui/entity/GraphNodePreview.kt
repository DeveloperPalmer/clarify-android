package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable

/**
 * Содержимое превью-карточки: последнее сообщение эпизода целиком.
 *
 * Время здесь — **не** время начала эпизода: карточка разворачивает последнее сообщение кластера, и
 * совпадение этих двух величин было бы случайным.
 *
 * @param authorName имя автора сообщения
 * @param authorPhotoUrl фото автора; `null` — аватар рисует инициалы
 * @param text полный текст сообщения, без эллипсиса
 * @param time время сообщения, готовое к показу
 */
@Immutable
internal data class GraphNodePreview(
  override val authorName: String,
  override val authorPhotoUrl: String?,
  override val text: String,
  override val time: String
) : NodePreview
