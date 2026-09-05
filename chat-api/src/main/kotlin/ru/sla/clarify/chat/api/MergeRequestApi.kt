package ru.sla.clarify.chat.api

/**
 * Merge request ветки: открытие, одобрение, отмена и финализация.
 *
 * Тема из `wiki/method/`: интерфейс отделён от остальных, потому что им не нужно знать
 * друг о друге, и это же деление даёт имя операции в спеке.
 */
interface MergeRequestApi {

  suspend fun createOpenMergeRequest(conversationId: String, branchId: String)

  suspend fun updateMergeApproval(
    conversationId: String,
    branchId: String,
    memberUids: List<String>
  )

  suspend fun updateMergeFinalize(conversationId: String, branchId: String)

  suspend fun deleteMergeRequest(conversationId: String, branchId: String)

  suspend fun deleteMergeRequestApproval(conversationId: String, branchId: String)
}
