package ru.sla.clarify.chat

interface ChatManager {
  fun initialize()

  suspend fun signIn(chatSignature: String)
  suspend fun signOut()
}
