---
tags:
  - method
---
# branchLive

**Summary**: Живая подписка на документ одной ветки.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-09-05

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-09-05 |
| Description      | Открывает snapshot listener на документ `conversations/{conversationId}/branches/{branchId}`. Эмитит данные ветки или `null`, если документ удалён либо ещё не создан. В отличие от `branchesLive`, слушает один документ, а не всю коллекцию: экрану ветки нужна только она сама. Через эту подписку экран узнаёт о переименовании ветки, о появлении и смене статуса merge request и об обновлении превью последнего сообщения. Открытие трекается через `FirestoreListenerGuard`. |

### Signature

```kotlin
fun branchLive(
  conversationId: String,
  branchId: String
): Flow<BranchNM?>
```

### Parameters

| Parameter      | Req | Type   | Description                                |
|----------------|-----|--------|--------------------------------------------|
| conversationId | Y   | String | ID беседы, которой принадлежит ветка.      |
| branchId       | Y   | String | ID ветки, за которой ведётся наблюдение.   |

### Response parameters

| Parameter                       | Req | Type       | Description                                                   |
|---------------------------------|-----|------------|---------------------------------------------------------------|
| id                              | Y   | String     | ID ветки.                                                     |
| parentBranchId                  | Y   | String     | ID родительской ветки.                                        |
| branchedFromCommitId            | Y   | String     | ID commit'а, от которого отответвились.                       |
| name                            | Y   | String     | Название ветки.                                               |
| lastCommitText                  | N   | String?    | Текст последнего сообщения ветки. `null` — сообщений нет.     |
| lastCommitAt                    | N   | Timestamp? | Время последнего сообщения ветки. `null` — сообщений нет.     |
| createdByUid                    | Y   | String     | UID создателя ветки.                                          |
| createdAt                       | Y   | Timestamp  | Время создания.                                               |
| mergeRequest                    | N   | Object?    | Merge request, если открыт. `null` — нет активного MR.        |
| mergeRequest.status             | Y   | String     | Статус MR.<br>\* open<br>\* readyToMerge<br>\* merged         |
| mergeRequest.initiatorUid       | Y   | String     | UID пользователя, открывшего MR.                              |
| mergeRequest.requestedAt        | Y   | Timestamp  | Время открытия MR.                                            |
| mergeRequest.approvedByUids     | Y   | String[]   | Список UID участников, одобривших MR.                         |
| mergeRequest.mergedAt           | N   | Timestamp? | Время финализации merge. `null` — merge ещё не завершён.      |
| mergeRequest.mergedIntoBranchId | N   | String?    | ID ветки, в которую смержили. `null` — merge ещё не завершён. |

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "branchId": "branch-ghi012"
}
```

### Response:

Каждая эмиссия Flow:

```json
{
  "id": "branch-ghi012",
  "parentBranchId": "conv-xyz789",
  "branchedFromCommitId": "commit-def456",
  "name": "Feature discussion",
  "lastCommitText": "Let's discuss this here",
  "lastCommitAt": "2026-06-13T16:00:00Z",
  "createdByUid": "uid-alice",
  "createdAt": "2026-06-13T15:00:00Z",
  "mergeRequest": {
    "status": "open",
    "initiatorUid": "uid-alice",
    "requestedAt": "2026-06-13T15:30:00Z",
    "approvedByUids": ["uid-alice"],
    "mergedAt": null,
    "mergedIntoBranchId": null
  }
}
```

### Errors List:

| Exception                  | Condition                                                     |
|----------------------------|---------------------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети — Flow завершается с ошибкой через `close(error)`. |
