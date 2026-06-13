---
tags:
  - method
---
# observe-branches

**Summary**: Живая подписка на список веток conversation.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Открывает snapshot listener на подколлекцию `conversations/{conversationId}/branches`. Возвращает изменения при создании новых веток или обновлении существующих (например, при появлении `mergeRequest`). Открытие трекается через `FirestoreListenerGuard`. |

### Signature

```kotlin
fun observeBranches(conversationId: String): Flow<List<FirestoreChange<BranchNM>>>
```

### Parameters

| Parameter      | Req | Type   | Description                                 |
|----------------|-----|--------|---------------------------------------------|
| conversationId | Y   | String | ID conversation, ветки которой отслеживаем. |

### Response parameters

| Parameter                               | Req | Type       | Description                                                             |
| --------------------------------------- | --- | ---------- | ----------------------------------------------------------------------- |
| [].changeType                           | Y   | String     | Тип изменения документа ветки.<br>\* added<br>\* modified<br>\* removed |
| [].hasPendingWrites                     | N   | Boolean    | Всегда `false` для этого метода.                                        |
| [].data.id                              | Y   | String     | ID ветки.                                                               |
| [].data.parentBranchId                  | Y   | String     | ID родительской ветки.                                                  |
| [].data.branchedFromCommitId            | Y   | String     | ID commit'а, от которого отответвились.                                 |
| [].data.name                            | Y   | String     | Название ветки.                                                         |
| [].data.createdAt                       | Y   | Timestamp  | Время создания.                                                         |
| [].data.lastCommitAt                    | N   | Timestamp? | Время последнего сообщения в ветке.                                     |
| [].data.createdByUid                    | Y   | String     | UID создателя ветки.                                                    |
| [].data.mergeRequest                    | N   | Object?    | Merge request, если открыт. `null` — нет активного MR.                  |
| [].data.mergeRequest.status             | Y   | String     | Статус MR.<br>\* open<br>\* readyToMerge<br>\* merged                   |
| [].data.mergeRequest.initiatorUid       | Y   | String     | UID пользователя, открывшего MR.                                        |
| [].data.mergeRequest.requestedAt        | Y   | Timestamp  | Время открытия MR.                                                      |
| [].data.mergeRequest.approvedByUids     | Y   | String[]   | Список UID участников, одобривших MR.                                   |
| [].data.mergeRequest.mergedAt           | N   | Timestamp? | Время финализации merge. `null` — merge ещё не завершён.                |
| [].data.mergeRequest.mergedIntoBranchId | N   | String?    | ID ветки, в которую смержили. `null` — merge ещё не завершён.           |

### Request:

```json
{
  "conversationId": "conv-xyz789"
}
```

### Response:

```json
[
  {
    "changeType": "added",
    "hasPendingWrites": false,
    "data": {
      "id": "branch-ghi012",
      "parentBranchId": "conv-xyz789",
      "branchedFromCommitId": "commit-def456",
      "name": "Feature discussion",
      "createdAt": "2026-06-13T15:00:00Z",
      "lastCommitAt": "2026-06-13T16:00:00Z",
      "createdByUid": "uid-alice",
      "mergeRequest": {
        "status": "open",
        "initiatorUid": "uid-alice",
        "requestedAt": "2026-06-13T15:30:00Z",
        "approvedByUids": ["uid-alice"],
        "mergedAt": null,
        "mergedIntoBranchId": null
      }
    }
  }
]
```

### Errors List:

| Exception                  | Condition                                                       |
|----------------------------|-----------------------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети — Flow завершается с ошибкой через `close(error)`. |
