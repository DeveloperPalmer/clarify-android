---
tags:
  - method
---
# groupCommitsLive

**Summary**: Живая подписка на сообщения корневой ветки групповой беседы.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-07-05

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Слушает корневую ветку (master) групповой беседы — ту, ID которой совпадает с `conversationId`. Возвращает изменения с флагом `hasPendingWrites` для оптимистичного отображения. Как и остальные подписки на сообщения, запрос фильтруется по `whereArrayContains(visibleFor, uid)` текущего пользователя — участник видит сообщения, отправленные с момента его вступления в группу. Действия «скрыть сообщение» в группах пока нет, поэтому `visibleFor` из документов не убирается — фильтр на текущем поведении ничего не отсекает, но готовит почву для будущей доработки. |


### Signature

```kotlin
fun groupCommitsLive(
  conversationId: String,
  limit: Long
): Flow<List<FirestoreChange<CommitNM>>>
```

### Parameters

| Parameter      | Req | Type   | Description                                                |
|----------------|-----|--------|------------------------------------------------------------|
| conversationId | Y   | String | ID группового conversation. Одновременно является branchId корневой ветки. |
| limit          | Y   | Long   | Максимальное количество последних сообщений в подписке.    |

### Response parameters

| Parameter               | Req | Type                   | Description                                     |
|-------------------------|-----|------------------------|-------------------------------------------------|
| [].changeType | Y   | String     | Тип изменения документа commit.<br>\* added<br>\* modified<br>\* removed |
| [].hasPendingWrites     | Y   | Boolean                | `true` если документ ещё не подтверждён сервером (optimistic update). |
| [].data.id              | Y   | String                 | ID документа commit.                            |
| [].data.clientCommitId  | Y   | String                 | Клиентский UUID сообщения.                      |
| [].data.senderUid       | Y   | String                 | UID отправителя.                                |
| [].data.text            | Y   | String                 | Текст сообщения.                                |
| [].data.type            | Y   | String                 | Тип сообщения.<br>\* text<br>\* inviteMember   |
| [].data.createdAt       | Y   | Timestamp              | Время создания.                                 |
| [].data.branchId        | Y   | String                 | Всегда равен `conversationId` (корневая ветка). |
| [].data.visibleFor      | Y   | List\<String\>         | UID участников на момент отправки. В группах для фильтрации не используется. |

### Request:

```json
{
  "conversationId": "conv-group001",
  "limit": 50
}
```

### Response:

Каждая эмиссия Flow:

```json
[
  {
    "changeType": "added",
    "hasPendingWrites": true,
    "data": {
      "id": "commit-new",
      "clientCommitId": "commit-new",
      "senderUid": "uid-alice",
      "text": "Welcome everyone!",
      "type": "text",
      "createdAt": "2026-06-13T14:35:00Z",
      "branchId": "conv-group001",
      "visibleFor": ["uid-alice", "uid-bob", "uid-carol"]
    }
  }
]
```

### Errors List:

| Exception                  | Condition                                                       |
|----------------------------|-----------------------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети — Flow завершается с ошибкой через `close(error)`. |
