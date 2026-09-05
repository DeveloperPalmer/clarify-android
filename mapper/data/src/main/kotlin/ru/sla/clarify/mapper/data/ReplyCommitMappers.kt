package ru.sla.clarify.mapper.data

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.CommitRecord

/**
 * Цитата, пришедшая снаружи, в экранную: «своё/чужое» знает только вызывающий,
 * поэтому [selfUserId] приходит параметром.
 */
fun CommitRecord.Reply.toDomainModel(selfUserId: UserId): Commit.Reply {
  return Commit.Reply(
    id = id,
    senderId = senderId,
    isSelf = senderId == selfUserId,
    text = text
  )
}

/** Снапшот цитируемого сообщения для отправки: сохраняем то, что видел отправитель. */
fun Commit.Message.toReplyRecord(): CommitRecord.Reply {
  return CommitRecord.Reply(
    id = id,
    senderId = senderId,
    text = text
  )
}
