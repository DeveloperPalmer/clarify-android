package ru.sla.clarify.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.database.entity.SettingsEntity

@Dao
interface SettingsDao {

  @Query("SELECT value FROM Settings WHERE key = :key LIMIT 1")
  suspend fun select(key: SettingsEntity.Key): String?

  @Query("SELECT value FROM Settings WHERE key = :key LIMIT 1")
  fun observe(key: SettingsEntity.Key): Flow<String?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrReplace(entity: SettingsEntity)

  @Delete(SettingsEntity::class)
  suspend fun delete(key: SettingsEntity.Key)
}
