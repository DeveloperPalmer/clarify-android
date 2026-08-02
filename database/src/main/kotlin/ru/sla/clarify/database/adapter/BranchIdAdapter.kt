package ru.sla.clarify.database.adapter

import app.cash.sqldelight.ColumnAdapter
import ru.sla.clarify.entity.chat.Branch

object BranchIdAdapter : ColumnAdapter<Branch.Id, String> {
  override fun decode(databaseValue: String): Branch.Id {
    return Branch.Id(databaseValue)
  }

  override fun encode(value: Branch.Id): String {
    return value.value
  }
}
