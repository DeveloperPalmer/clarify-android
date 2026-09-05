---
tags:
  - method
---
# commitsLive

**Summary**: Живая подписка на сообщения ветки от указанного курсора и дальше, без верхней границы.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-09-05

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-09-05 |
| Description      | Вторая перегрузка `commitsLive`. Вместо окна «последние `limit` сообщений» открывает восходящий хвост: подписку на все сообщения ветки начиная с курсора `from` и дальше в будущее. Курсор — пара «время создания + ID сообщения», где ID служит тай-брейком для сообщений с одинаковым временем. Граница включительная: пограничное сообщение само остаётся в окне, поэтому его последующие правки и удаления по-прежнему наблюдаются вживую, а повторная выдача такого сообщения как `added` идемпотентна. Лимита нет намеренно — верхняя граница вытесняла бы старые сообщения из окна и порождала бы фантомные изменения `removed`. Запрос всегда дополняется `whereArrayContains(visibleFor, uid)` текущего пользователя — скрытые им сообщения не приходят; скрытие сообщения приходит подписчику как изменение `removed`. Открытие трекается через `FirestoreListenerGuard`. Подписка открывается поверх уже загруженной первой страницы истории — от самого старого сообщения в кэше; более ранняя история догружается отдельно, через `readCommits`. |

### Signature

```kotlin
fun commitsLive(
  conversationId: String,
  branchId: String,
  from: CommitCursor?
): Flow<List<FirestoreChange<CommitNM>>>
```

### Parameters

| Parameter      | Req | Type          | Description                                                                            |
|----------------|-----|---------------|----------------------------------------------------------------------------------------|
| conversationId | Y   | String        | ID conversation, в которой находится ветка.                                            |
| branchId       | Y   | String        | ID ветки, сообщения которой слушаем.                                                   |
| from           | N   | CommitCursor? | Нижняя граница подписки, включительно. `null` — вся история ветки без нижней границы.  |
| from.createdAt | Y   | Timestamp     | Время создания граничного сообщения.                                                   |
| from.id        | Y   | String        | ID граничного сообщения — тай-брейк для сообщений с одинаковым временем.               |

### Response parameters

| Parameter              | Req | Type           | Description                                                              |
|------------------------|-----|----------------|--------------------------------------------------------------------------|
| [].changeType          | Y   | String         | Тип изменения документа commit.<br>\* added<br>\* modified<br>\* removed |
| [].hasPendingWrites    | Y   | Boolean        | `true` если документ ещё не подтверждён сервером.                        |
| [].data.id             | Y   | String         | ID документа commit.                                                     |
| [].data.senderUid      | Y   | String         | UID отправителя.                                                         |
| [].data.text           | Y   | String         | Текст сообщения.                                                         |
| [].data.type           | Y   | String         | Тип сообщения.<br>\* text<br>\* inviteMember                             |
| [].data.createdAt      | Y   | Timestamp      | Время создания (ASC-порядок в ответе).                                   |
| [].data.editedAt       | N   | Timestamp?     | Время правки. `null` — сообщение не редактировалось.                     |
| [].data.branchId       | Y   | String         | ID ветки.                                                                |
| [].data.visibleFor     | Y   | List\<String\> | UID участников, которым видно сообщение.                                 |

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "branchId": "branch-ghi012",
  "from": {
    "createdAt": "2026-06-13T15:00:00Z",
    "id": "commit-branch01"
  }
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
      "senderUid": "uid-alice",
      "text": "Let's discuss this here",
      "type": "text",
      "createdAt": "2026-06-13T15:00:00Z",
      "editedAt": null,
      "branchId": "branch-ghi012",
      "visibleFor": ["uid-alice", "uid-bob"]
    }
  },
  {
    "changeType": "added",
    "hasPendingWrites": false,
    "data": {
      "id": "commit-branch02",
      "senderUid": "uid-bob",
      "text": "Agreed",
      "type": "text",
      "createdAt": "2026-06-13T16:00:00Z",
      "editedAt": null,
      "branchId": "branch-ghi012",
      "visibleFor": ["uid-alice", "uid-bob"]
    }
  }
]
```

### Errors List:

| Exception                  | Condition                                                     |
|----------------------------|---------------------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети — Flow завершается с ошибкой через `close(error)`. |
