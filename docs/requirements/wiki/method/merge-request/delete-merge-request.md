---
tags:
  - method
---
# deleteMergeRequest

**Summary**: Отменяет merge request — поле `mergeRequest` удаляется из документа ветки.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Выполняет Firestore-транзакцию: читает документ ветки, проверяет наличие и статус (`Open` или `ReadyToMerge`) MR, затем удаляет поле `mergeRequest` через `FieldValue.delete()`. После удаления ветка снова принимает commit'ы. Доступно любому участнику, не только инициатору. |


### Signature

```kotlin
suspend fun deleteMergeRequest(
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

Метод возвращает `Unit`. Успешное завершение означает, что поле `mergeRequest` удалено из документа ветки.

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "branchId": "branch-ghi012"
}
```

### Response:

Документ ветки после отмены MR — поле `mergeRequest` отсутствует:

```json
{
  "id": "branch-ghi012",
  "parentBranchId": "conv-xyz789",
  "branchedFromCommitId": "commit-def456",
  "name": "Feature discussion",
  "createdAt": "2026-06-13T15:00:00Z",
  "lastCommitAt": "2026-06-13T16:30:00Z",
  "createdByUid": "uid-alice",
  "mergeRequest": null
}
```

### Errors List:

| Exception                  | Condition                                                                       |
|----------------------------|---------------------------------------------------------------------------------|
| IllegalStateException      | Нет активного merge request (`mergeRequest == null`).                           |
| IllegalStateException      | Статус MR не `Open` и не `ReadyToMerge` (например, уже `Merged`).              |
| FirebaseFirestoreException | Ошибка сети, конфликт транзакции или нарушение Firestore Security Rules.        |
