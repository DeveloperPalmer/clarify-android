package ru.sla.clarify.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import ru.sla.clarify.database.dao.SettingsDao
import ru.sla.clarify.database.entity.SettingsEntity

@Database(
  version = 1,
  entities = [SettingsEntity::class],
  exportSchema = false
)
abstract class SettingsDatabase : RoomDatabase() {
  abstract fun settingsDao(): SettingsDao
}
