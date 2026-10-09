package ru.sla.clarify.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.DistinctFlow
import ru.sla.clarify.database.entity.UserEntity

@Dao
interface UserDao {

  @Query("SELECT * FROM User WHERE id = :id LIMIT 1")
  fun observe(id: UserId): DistinctFlow<UserEntity?>
}
