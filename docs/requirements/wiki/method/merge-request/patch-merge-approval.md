---
tags:
  - method
---
# patchMergeApproval

**Summary**: Добавляет текущего пользователя в список approvers merge request'а. При полном покрытии участников переводит статус в ReadyToMerge.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Выполняет Firestore-транзакцию: читает текущий документ ветки, добавляет UID текущего пользователя в `approvedByUids` (через `distinct`, чтобы избежать дублей), и проверяет покрытие: если все `participantUids` присутствуют в `approvedByUids` — статус переключается на `ReadyToMerge`. Использует dot-path обновление, чтобы не перезаписывать остальные поля `mergeRequest`. |


### Signature

```kotlin
suspend fun patchMergeApproval(
  conversationId: String,
  branchId: String,
  participantUids: List<String>
)
```

### Parameters

| Parameter       | Req | Type          | Description                                                                                         |
|-----------------|-----|---------------|-----------------------------------------------------------------------------------------------------|
| conversationId  | Y   | String        | ID conversation, в которой находится ветка.                                                         |
| branchId        | Y   | String        | ID ветки с активным merge request'ом.                                                               |
| participantUids | Y   | List\<String\> | Полный список UID участников conversation. Используется для проверки полноты аппрувов. |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что одобрение применено.

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "branchId": "branch-ghi012",
  "participantUids": ["uid-alice", "uid-bob"]
}
```

### Response:

Частичное одобрение (не все approve):
```json
{
  "mergeRequest": {
    "status": "open",
    "initiatorUid": "uid-alice",
    "requestedAt": "2026-06-13T16:00:00Z",
    "approvedByUids": ["uid-alice"],
    "mergedAt": null,
    "mergedIntoBranchId": null
  }
}
```

Все участники одобрили (`ReadyToMerge`):
```json
{
  "mergeRequest": {
    "status": "readyToMerge",
    "initiatorUid": "uid-alice",
    "requestedAt": "2026-06-13T16:00:00Z",
    "approvedByUids": ["uid-alice", "uid-bob"],
    "mergedAt": null,
    "mergedIntoBranchId": null
  }
}
```

### Errors List:

| Exception                  | Condition                                                                           |
|----------------------------|-------------------------------------------------------------------------------------|
| IllegalStateException      | Нет активного merge request (`mergeRequest == null`).                               |
| IllegalStateException      | Статус MR не `Open` и не `ReadyToMerge` (например, уже `Merged`).                  |
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.                                           |
| FirebaseFirestoreException | Ошибка сети, конфликт транзакции или нарушение Firestore Security Rules.            |
