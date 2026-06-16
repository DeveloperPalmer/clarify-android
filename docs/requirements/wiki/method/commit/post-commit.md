---
tags:
  - method
---
# postCommit

**Summary**: Отправляет сообщение в direct-чат, при необходимости создавая conversation и документы участников.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Универсальный метод для direct-чата. Если `conversationId == null` — conversation создаётся с новым UUID, и для обоих участников создаются документы в подколлекции `members`. Если `branchId == null` — сообщение идёт в корневую ветку (master), чей ID совпадает с `conversationId`. Root-сообщения обновляют `lastCommit*`-поля conversation и инкрементят `unreadCommits` собеседника. Branch-сообщения обновляют `lastCommit*` ветки и инкрементят `branchUnreadCommits`. Всё выполняется в одном batch. |


### Signature

```kotlin
suspend fun postCommit(
  peerId: Peer.Id,
  branchId: String?,
  conversationId: String?,
  text: String
)
```

### Parameters

| Parameter      | Req | Type     | Description                                                                                                   |
|----------------|-----|----------|---------------------------------------------------------------------------------------------------------------|
| peerId         | Y   | Peer.Id  | UID собеседника. Вместе с текущим пользователем определяет пару участников.                                    |
| branchId       | N   | String?  | ID ветки. `null` — сообщение идёт в корень conversation (master).                                             |
| conversationId | N   | String?  | ID существующей conversation. `null` — conversation будет создана с новым UUID.                               |
| text           | Y   | String   | Текст сообщения.                                                                                              |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что все записи применены атомарно.

### Request:

```json
{
  "peerId": "uid-bob",
  "branchId": null,
  "conversationId": null,
  "text": "Hello!"
}
```

### Response:

**Новый commit** `conversations/conv-xyz789/commits/commit-new`:
```json
{
  "clientCommitId": "commit-new",
  "senderUid": "uid-alice",
  "text": "Hello!",
  "type": "text",
  "createdAt": "2026-06-13T14:30:00Z",
  "branchId": "conv-xyz789"
}
```

**Обновлённый корень conversation** (root-сообщение):
```json
{
  "lastCommitText": "Hello!",
  "lastCommitSenderUid": "uid-alice",
  "lastCommitAt": "2026-06-13T14:30:00Z"
}
```

**Unread increment** для собеседника `conversations/conv-xyz789/unreadCommits/uid-bob`:
```json
{
  "count": 1
}
```

### Errors List:

| Exception                  | Condition                                             |
|----------------------------|-------------------------------------------------------|
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.             |
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.   |
