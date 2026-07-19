package ru.sla.clarify.lib.google.firestore.entity

data class CommitNotFoundException(
  val commitId: String
) : RuntimeException("commit $commitId not found")
