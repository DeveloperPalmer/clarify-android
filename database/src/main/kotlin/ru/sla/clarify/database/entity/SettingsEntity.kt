package ru.sla.clarify.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "Settings")
data class SettingsEntity(
  @PrimaryKey
  val key: Key,
  val value: String
) {
  @JvmInline
  value class Key(val key: String)
}
