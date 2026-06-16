package ru.sla.clarify.lib.google.firestore.entity.write

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ServerTimestamp
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM.Type

@Serializable
data class PostConversationParams(
  val type: Type,
  val memberUids: List<String>,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  val name: String? = null,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  val ownerUid: String? = null,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  val lastCommitText: String? = null,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  val lastCommitSenderUid: String? = null,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  @Contextual
  val lastCommitAt: Timestamp? = null,
  @Contextual
  val updatedAt: ServerTimestamp = ServerTimestamp
)
