package ru.sla.clarify.entity.chat

import ru.sla.clarify.core.domain.entity.UserId

/**
 * Беседа такой, какой её держит источник истины: состав участников и денормализованное превью
 * последнего сообщения. В отличие от [Conversation], который собирается для экрана и знает про
 * непрочитанное и разрешённого собеседника, здесь только то, что приходит снаружи.
 */
data class ConversationRecord(
  val id: Conversation.Id,
  val type: Type,
  val memberIds: List<Member.Id>,
  val name: String?,
  val ownerId: UserId?,
  val lastCommitText: String?,
  val lastCommitSenderId: UserId?,
  val lastCommitAtSeconds: Long
) {

  /**
   * Вид беседы отдельным полем: запись плоская, а [Conversation] различает вид подтипами
   * и в дискриминаторе не нуждается.
   */
  enum class Type(val value: String) {
    Direct("direct"),
    Group("group");

    companion object {
      fun fromValue(value: String): Type {
        return entries.firstOrNull { it.value == value } ?: error("unexpected conversation type: $value")
      }
    }
  }
}
