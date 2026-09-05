package ru.sla.clarify.app.data.channel

/**
 * Открывает соединение канала. Адрес знает реализация: подписчику он не нужен, а каналу — тем
 * более, иначе он начал бы разбираться в стендах.
 */
fun interface ChannelTransport {
  suspend fun open(): ChannelConnection
}
