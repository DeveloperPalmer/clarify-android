---
tags:
  - method
---
# conversationsLive

**Summary**: Живая подписка на все conversations текущего пользователя.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-19

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Открывает snapshot listener на коллекцию `conversations` с фильтром `memberUids array-contains currentUserId`. Возвращает изменения (ADDED / MODIFIED / REMOVED) в виде `FirestoreChange`. Охватывает одновременно direct и group conversations — тип различается полем `type` внутри `ConversationNM`. UID текущего пользователя берётся из `AuthSessionPersistence` внутри Flow. |


### Signature

```kotlin
fun conversationsLive(): Flow<List<FirestoreChange<ConversationNM>>>
```

### Parameters

Параметров нет. UID текущего пользователя берётся из `AuthSessionPersistence` при подписке.

### Response parameters

| Parameter                    | Req | Type                  | Description                                                        |
|------------------------------|-----|-----------------------|--------------------------------------------------------------------|
| [].changeType | Y   | String     | Тип изменения документа в Firestore.<br>\* added<br>\* modified<br>\* removed |
| [].hasPendingWrites          | N   | Boolean               | Всегда `false` для этого метода (MetadataChanges не включён).      |
| [].data.id                   | Y   | String                | ID документа conversation.                                         |
| [].data.type                 | Y   | String                | Тип беседы.<br>\* direct<br>\* group |
| [].data.memberUids      | Y   | List\<String\>        | Список UID участников.                                             |
| [].data.name                 | N   | String?               | Название группы (`null` для direct).                               |
| [].data.ownerUid             | N   | String?               | UID создателя группы (`null` для direct).                          |
| [].data.lastCommitText       | N   | String?               | Текст последнего сообщения в root-ветке.                           |
| [].data.lastCommitSenderUid  | N   | String?               | UID отправителя последнего сообщения.                              |
| [].data.lastCommitAt         | N   | Timestamp?            | Время последнего сообщения.                                        |

### Response:

Каждая эмиссия Flow — список изменений с момента предыдущего снапшота:

```json
[
  {
    "changeType": "added",
    "hasPendingWrites": false,
    "data": {
      "id": "conv-xyz789",
      "type": "direct",
      "memberUids": ["uid-alice", "uid-bob"],
      "lastCommitText": "Hey there!",
      "lastCommitSenderUid": "uid-alice",
      "lastCommitAt": "2026-06-13T14:30:00Z",
      "name": null,
      "ownerUid": null
    }
  },
  {
    "changeType": "modified",
    "hasPendingWrites": false,
    "data": {
      "id": "conv-group001",
      "type": "group",
      "memberUids": ["uid-alice", "uid-bob", "uid-carol"],
      "lastCommitText": "Welcome everyone!",
      "lastCommitSenderUid": "uid-alice",
      "lastCommitAt": "2026-06-13T14:35:00Z",
      "name": "Project Team",
      "ownerUid": "uid-alice"
    }
  }
]
```

### Errors List:

| Exception                  | Condition                                                        |
|----------------------------|------------------------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети — Flow завершается с ошибкой через `close(error)`.  |
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.                        |
