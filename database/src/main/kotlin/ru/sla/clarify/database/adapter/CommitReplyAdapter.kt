package ru.sla.clarify.database.adapter

import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Commit

/**
 * Снапшот цитаты — неделимое значение: либо он есть целиком, либо его нет. Поэтому колонка одна,
 * а не набор nullable-полей, каждое из которых пришлось бы проверять на согласованность.
 */
object CommitReplyAdapter : ColumnAdapter<Commit.Reply, String> {
  override fun decode(databaseValue: String): Commit.Reply {
    val reply = Json.decodeFromString<ReplyJson>(databaseValue)
    return Commit.Reply(
      id = Commit.Id(reply.id),
      senderId = UserId(reply.senderId),
      isSelf = reply.isSelf,
      text = reply.text
    )
  }

  override fun encode(value: Commit.Reply): String {
    val reply = ReplyJson(
      id = value.id.value,
      senderId = value.senderId.value,
      isSelf = value.isSelf,
      text = value.text
    )
    return Json.encodeToString(reply)
  }

  @Serializable
  private data class ReplyJson(
    val id: String,
    val senderId: String,
    val isSelf: Boolean,
    val text: String
  )
}
