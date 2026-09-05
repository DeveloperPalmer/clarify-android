package ru.sla.clarify.chat.api

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.entity.chat.BranchRecord
import ru.sla.clarify.entity.chat.ChatChange

/**
 * Ветки беседы: живой список, отдельная ветка и создание.
 *
 * Тема из `wiki/method/`: интерфейс отделён от остальных, потому что им не нужно знать
 * друг о друге, и это же деление даёт имя операции в спеке.
 */
interface BranchApi {

  fun branchesLive(
    conversationId: String
  ): Flow<List<ChatChange<BranchRecord>>>

  /**
   * Live-подписка на один документ ветки (name/mergeRequest/lastCommit). `null` —
   * документ удалён или ещё не создан. Для экрана ветки достаточно её самой, поэтому
   * слушаем один документ, а не всю коллекцию [branchesLive].
   */
  fun branchLive(
    conversationId: String,
    branchId: String
  ): Flow<BranchRecord?>

  suspend fun createBranch(
    conversationId: String,
    parentBranchId: String,
    branchedFromCommitId: String,
    name: String
  ): BranchRecord
}
