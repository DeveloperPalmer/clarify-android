package ru.sla.clarify.database.converter

import androidx.room3.ColumnTypeConverter
import ru.sla.clarify.entity.chat.Branch

object BranchIdConverter {
  @ColumnTypeConverter
  fun decode(databaseValue: String): Branch.Id {
    return Branch.Id(databaseValue)
  }

  @ColumnTypeConverter
  fun encode(value: Branch.Id): String {
    return value.value
  }
}
