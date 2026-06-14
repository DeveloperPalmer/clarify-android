package ru.sla.clarify.feature.chat.group.thread.ui.entity

import androidx.compose.runtime.Immutable

@Immutable
data class Group(
  val id: String,
  val name: String,
  val memberCount: Int
)
