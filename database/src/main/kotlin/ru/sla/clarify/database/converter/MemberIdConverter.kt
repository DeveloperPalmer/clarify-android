package ru.sla.clarify.database.converter

import androidx.room3.ColumnTypeConverter
import ru.sla.clarify.entity.chat.Member

object MemberIdConverter {
  @ColumnTypeConverter
  fun decode(databaseValue: String): Member.Id {
    return Member.Id(databaseValue)
  }

  @ColumnTypeConverter
  fun encode(value: Member.Id): String {
    return value.value
  }
}
