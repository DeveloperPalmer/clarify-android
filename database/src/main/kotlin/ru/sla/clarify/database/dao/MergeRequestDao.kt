package ru.sla.clarify.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import ru.sla.clarify.database.entity.MergeRequestEntity

/**
 * Своих чтений у запроса слияния нет: он читается вместе с веткой, через `ChatBranchDao`.
 */
@Dao
interface MergeRequestDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrReplace(entity: MergeRequestEntity)
}
