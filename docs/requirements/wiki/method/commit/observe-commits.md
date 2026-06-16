---
tags:
  - method
---
# observe-commits

**Summary**: Живая подписка на сообщения конкретной ветки по известному conversationId.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Делегирует выполнение в `conversationMessagesLive`. В отличие от `observeDirectCommits`, не требует `peerId` и не резолвит conversationId — conversation уже известна. Используется branch-фичей, которая знает свой `conversationId` и `branchId`, но не работает с peer. |


### Signature

```kotlin
fun observeCommits(
  conversationId: String,
  branchId: String,
  limit: Long
): Flow<List<FirestoreChange<CommitNM>>>
```

### Parameters

| Parameter      | Req | Type   | Description                                                |
|----------------|-----|--------|------------------------------------------------------------|
| conversationId | Y   | String | ID conversation, в которой находится ветка.                |
| branchId       | Y   | String | ID ветки, сообщения которой слушаем.                       |
| limit          | Y   | Long   | Максимальное количество последних сообщений в подписке.    |

### Response parameters

| Parameter               | Req | Type                   | Description                                     |
|-------------------------|-----|------------------------|-------------------------------------------------|
| [].changeType | Y   | String     | Тип изменения документа commit.<br>\* added<br>\* modified<br>\* removed |
| [].hasPendingWrites     | Y   | Boolean                | `true` если документ ещё не подтверждён сервером. |
| [].data.id              | Y   | String                 | ID документа commit.                            |
| [].data.clientCommitId  | Y   | String                 | Клиентский UUID сообщения.                      |
| [].data.senderUid       | Y   | String                 | UID отправителя.                                |
| [].data.text            | Y   | String                 | Текст сообщения.                                |
| [].data.type            | Y   | String                 | Тип сообщения.<br>\* text |                                  |
| [].data.createdAt       | Y   | Timestamp              | Время создания.                                 |
| [].data.branchId        | Y   | String                 | ID ветки.                                       |

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "branchId": "branch-ghi012",
  "limit": 50
}
```

### Response:

```json
[
  {
    "changeType": "added",
    "hasPendingWrites": false,
    "data": {
      "id": "commit-branch01",
      "clientCommitId": "commit-branch01",
      "senderUid": "uid-alice",
      "text": "Let's discuss this here",
      "type": "text",
      "createdAt": "2026-06-13T15:00:00Z",
      "branchId": "branch-ghi012"
    }
  }
]
```

### Errors List:

| Exception                  | Condition                                                       |
|----------------------------|-----------------------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети — Flow завершается с ошибкой через `close(error)`. |
