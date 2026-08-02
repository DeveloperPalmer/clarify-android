package ru.sla.clarify.database.adapter

import app.cash.sqldelight.ColumnAdapter
import ru.sla.clarify.entity.chat.Member

object MemberIdAdapter : ColumnAdapter<Member.Id, String> {
  override fun decode(databaseValue: String): Member.Id {
    return Member.Id(databaseValue)
  }

  override fun encode(value: Member.Id): String {
    return value.value
  }
}
