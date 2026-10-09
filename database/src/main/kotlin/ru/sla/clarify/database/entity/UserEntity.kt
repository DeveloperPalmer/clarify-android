package ru.sla.clarify.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import ru.sla.clarify.core.domain.entity.UserId

@Entity(tableName = "User")
data class UserEntity(
  @PrimaryKey
  val id: UserId,
  val email: String,
  val displayName: String,
  val photoUrl: String?
)
