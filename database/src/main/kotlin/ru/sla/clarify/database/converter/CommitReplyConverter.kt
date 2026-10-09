package ru.sla.clarify.database.converter

import androidx.room3.ColumnTypeConverter
import kotlinx.serialization.json.Json
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.entity.CommitReplyJson
import ru.sla.clarify.entity.chat.Commit

/**
 * Снапшот цитаты — неделимое значение: либо он есть целиком, либо его нет. Поэтому колонка одна,
 * а не набор nullable-полей, каждое из которых пришлось бы проверять на согласованность.
 */
object CommitReplyConverter {
  @ColumnTypeConverter
  fun decode(databaseValue: String): Commit.Reply {
    val reply = Json.decodeFromString<CommitReplyJson>(databaseValue)
    return Commit.Reply(
      id = Commit.Id(reply.id),
      senderId = UserId(reply.senderId),
      isSelf = reply.isSelf,
      text = reply.text
    )
  }

  @ColumnTypeConverter
  fun encode(value: Commit.Reply): String {
    val reply = CommitReplyJson(
      id = value.id.value,
      senderId = value.senderId.value,
      isSelf = value.isSelf,
      text = value.text
    )
    return Json.encodeToString(reply)
  }
}
