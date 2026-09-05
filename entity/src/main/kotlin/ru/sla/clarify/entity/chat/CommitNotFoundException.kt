package ru.sla.clarify.entity.chat

/**
 * Сообщения, которое просили изменить или удалить, больше нет: его удалили «у всех»
 * вне живого окна этого устройства.
 */
data class CommitNotFoundException(
  val commitId: Commit.Id
) : RuntimeException("commit ${commitId.value} not found")
