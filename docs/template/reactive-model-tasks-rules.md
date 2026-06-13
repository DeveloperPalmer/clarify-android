# ReactiveModel.task — правила использования

`task<I, O>` из `ru.kode.remo` — основной способ запускать операции с
side-эффектом из ViewModel (через `Model`). У библиотеки есть жёсткое
ограничение: **результат `O` обязан быть non-null**. «Не найдено» / «нет
данных» сценарии передаются исключениями, не `null`.

## Правила

| # | Правило | Уточнение |
|---|---|---|
| 1 | `task<I, O>` — `O` всегда non-null | даже если Repository возвращает `T?` под капотом |
| 2 | «Не нашли» — domain-exception, не `null` | заводится в том же модуле, что и Repository (`PeerNotFoundException(email)`) |
| 3 | Repository `?: throw` на границе | `firestore.getX(...) ?: throw NotFoundException(...)` |
| 4 | UI разводит ошибки через `jobFlow.errors()` | `when (error) is XException -> snackbarA else -> snackbarB` |
| 5 | `successResults()` — только для «всё хорошо, есть значение» | если стрим может прислать пустой ответ, конвертируй его в throw в Repository |

## Примеры

Domain-exception рядом с Repository:

```kotlin
// feature/chat-conversation/domain/PeerNotFoundException.kt
class PeerNotFoundException(val email: String) : RuntimeException(
  "No user found with email: $email"
)
```

Repository поднимает её на границе:

```kotlin
override suspend fun findPeerByEmail(email: String): Peer.Id = withContext(Dispatchers.IO) {
  firestore.getPeerIdByEmail(email) ?: throw PeerNotFoundException(email)
}
```

Task в Model — non-null `O`:

```kotlin
val findPeerByEmail = task<String, Peer.Id>(name = "findPeerByEmail") { email ->
  conversationRepository.findPeerByEmail(email)
}
```

ViewModel: успех → действие, ошибка → разводка по типу:

```kotlin
onEach(chatModel.findPeerByEmail.jobFlow.successResults()) {
  action { _, _, peerId ->
    eventSink.sendEvent(FlowEvent.ThreadRequested(peerId))
  }
}

onEach(chatModel.findPeerByEmail.jobFlow.errors()) {
  action { _, _, error ->
    val messageId = when (error) {
      is PeerNotFoundException -> R.string.conversation_new_chat_error_user_not_found
      else -> R.string.conversation_new_chat_error_lookup_failed
    }
    sendViewEvent(Snackbar(isError = true, message = resRef(messageId)))
  }
}
```

## Антипаттерны

| Плохо | Хорошо | Почему |
|---|---|---|
| `task<String, Peer.Id?> { repo.findPeerByEmail(email) }` (репо возвращает `Peer.Id?`) | `task<String, Peer.Id>`; репо кидает `PeerNotFoundException` | `successResults()` падает с `IllegalStateException: internal error: null result` на `DefaultDispatcher-worker` — fatal crash, **не ловится** через `errors()`, улетает в reducer и убивает процесс |
| Возвращать sentinel-значение (`Peer.Id("")`, `Peer.Id.EMPTY`) | бросать domain-exception | теряется информация (какой email искали), UI начинает париться с проверками «а это правда peer или магия» |
| Catch'нуть исключение в Repository и вернуть `null` | пробрасывать наружу, ловить в ViewModel | то же самое — `null` в task = крах |
| Различать «не найден» и «сеть упала» через `Result<T, E>` или sealed-return | exception для «не найден», exception для «сети нет», разводка в `errors()` | библиотека построена вокруг `JobFlow` с success/error каналами — не воюй с ней |

## Где живёт обработка

- **Domain** — определение exception (`PeerNotFoundException`, `ConversationLockedException` и т.п.), рядом с интерфейсом Repository.
- **Data (Repository)** — `?: throw` на границе с Firestore/SQLite. Repository не должен возвращать nullable там, где это «не нашли».
- **UI (ViewModel)** — `successResults()` для happy-path, `errors()` с `when` для разводки на конкретные snackbars / диалоги.

## Связанные документы

- [`firestore-naming-rules.md`](firestore-naming-rules.md) — конвенция имён в Firestore-слое, который порождает данные для tasks.
- [`architecture-layers-rules.md`](architecture-layers-rules.md) — слои Repository → Model → ViewModel и поток данных.
