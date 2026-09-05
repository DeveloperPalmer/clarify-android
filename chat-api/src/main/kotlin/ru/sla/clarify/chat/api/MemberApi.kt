package ru.sla.clarify.chat.api

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.entity.chat.ChatChange
import ru.sla.clarify.entity.chat.Member
import java.time.LocalDateTime

/**
 * Участники беседы: состав, отметки прочтения, приглашение и выход.
 *
 * Тема из `wiki/method/`: интерфейс отделён от остальных, потому что им не нужно знать
 * друг о друге, и это же деление даёт имя операции в спеке.
 */
interface MemberApi {

  suspend fun readMember(conversationId: String, memberId: String): LocalDateTime?

  suspend fun deleteConversationMember(conversationId: String)

  suspend fun deleteConversationMember(
    conversationId: String,
    memberId: String
  )

  fun membersLive(
    conversationId: String
  ): Flow<List<ChatChange<Member.Id>>>

  fun memberLive(
    conversationId: String,
    memberId: String
  ): Flow<LocalDateTime?>

  suspend fun updateReadWatermark(
    conversationId: String,
    lastReadAt: LocalDateTime
  )

  suspend fun createCommitInviteMember(
    conversationId: String,
    memberId: String,
    memberUids: List<String>
  )
}
