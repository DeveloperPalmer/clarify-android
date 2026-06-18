---
tags:
  - method
---
# deleteMergeRequestApproval

**Summary**: Отзывает одобрение текущего пользователя и откатывает статус MR обратно в Open.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-19

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Выполняет Firestore-транзакцию: читает текущий документ ветки, убирает UID текущего пользователя из `approvedByUids` и принудительно устанавливает статус обратно в `Open` — даже если до этого был `ReadyToMerge`. Использует dot-path обновление. |


### Signature

```kotlin
suspend fun deleteMergeRequestApproval(
  conversationId: String,
  branchId: String
)
```

### Parameters

| Parameter      | Req | Type   | Description                                          |
|----------------|-----|--------|------------------------------------------------------|
| conversationId | Y   | String | ID conversation, в которой находится ветка.          |
| branchId       | Y   | String | ID ветки с активным merge request'ом.                |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что одобрение отозвано.

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "branchId": "branch-ghi012"
}
```

### Response:

Документ ветки после отзыва (alice убрала свой approve):

```json
{
  "mergeRequest": {
    "status": "open",
    "initiatorUid": "uid-alice",
    "requestedAt": "2026-06-13T16:00:00Z",
    "approvedByUids": ["uid-bob"],
    "mergedAt": null,
    "mergedIntoBranchId": null
  }
}
```

### Errors List:

| Exception                  | Condition                                                                   |
|----------------------------|-----------------------------------------------------------------------------|
| IllegalStateException      | Нет активного merge request (`mergeRequest == null`).                       |
| IllegalStateException      | Статус MR не `Open` и не `ReadyToMerge` (например, уже `Merged`).          |
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.                                   |
| FirebaseFirestoreException | Ошибка сети, конфликт транзакции или нарушение Firestore Security Rules.    |
