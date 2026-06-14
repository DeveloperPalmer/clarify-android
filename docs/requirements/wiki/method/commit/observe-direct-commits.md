---
tags:
  - method
---
# observe-direct-commits

**Summary**: Живая подписка на сообщения ветки в direct-чате с конкретным собеседником.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Сначала подписывается на ID direct conversation с данным `peerId` (`directConversationIdLive`). При изменении conversationId автоматически переключает внутренний listener через `flatMapLatest`. Если conversation ещё не существует — эмитит пустой список. При появлении conversation — начинает слушать её commits по `branchId` с ограничением `limit`. Используется в экране direct-чата, где conversationId неизвестен заранее. |


### Signature

```kotlin
fun observeDirectCommits(
  peerId: Peer.Id,
  branchId: String,
  limit: Long
): Flow<List<FirestoreChange<CommitNM>>>
```

### Parameters

| Parameter | Req | Type    | Description                                                                                               |
|-----------|-----|---------|-----------------------------------------------------------------------------------------------------------|
| peerId    | Y   | Peer.Id | UID собеседника. Вместе с UID текущего пользователя определяет единственный direct conversation между ними. |
| branchId  | Y   | String  | ID ветки, сообщения которой слушаем. Для корня conversation равен `conversationId`.                        |
| limit     | Y   | Long    | Максимальное количество последних сообщений в подписке.                                                    |

### Response parameters

| Parameter               | Req | Type                   | Description                                     |
|-------------------------|-----|------------------------|-------------------------------------------------|
| [].changeType | Y   | String     | Тип изменения документа commit.<br>\* added<br>\* modified<br>\* removed |
| [].hasPendingWrites     | Y   | Boolean                | `true` если документ ещё не подтверждён сервером (optimistic update). |
| [].data.id              | Y   | String                 | ID документа commit.                            |
| [].data.clientCommitId  | Y   | String                 | Клиентский UUID сообщения.                      |
| [].data.senderUid       | Y   | String                 | UID отправителя.                                |
| [].data.text            | Y   | String                 | Текст сообщения.                                |
| [].data.type            | Y   | String                 | Тип сообщения.<br>\* text<br>\* inviteMember |        |
| [].data.createdAt       | Y   | Timestamp              | Время создания.                                 |
| [].data.colorHex        | Y   | String                 | HEX-цвет сообщения (например `"#FF5733"`).      |
| [].data.branchId        | Y   | String                 | ID ветки, к которой принадлежит сообщение.      |

### Request:

```json
{
  "peerId": "uid-bob",
  "branchId": "conv-xyz789",
  "limit": 50
}
```

### Response:

Каждая эмиссия Flow — список изменений:

```json
[
  {
    "changeType": "added",
    "hasPendingWrites": false,
    "data": {
      "id": "commit-def456",
      "clientCommitId": "commit-def456",
      "senderUid": "uid-alice",
      "text": "Hello!",
      "type": "text",
      "createdAt": "2026-06-13T14:30:00Z",
      "colorHex": "#FF5733",
      "branchId": "conv-xyz789"
    }
  }
]
```

### Errors List:

| Exception                  | Condition                                                       |
|----------------------------|-----------------------------------------------------------------|
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.                       |
| FirebaseFirestoreException | Ошибка сети — Flow завершается с ошибкой через `close(error)`. |
