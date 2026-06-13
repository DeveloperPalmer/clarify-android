---
tags:
  - method
---
# patchMergeFinalize

**Summary**: Финализирует merge — переводит статус в Merged и записывает метаданные слияния.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Выполняет Firestore-транзакцию: читает документ ветки, проверяет что статус MR равен `ReadyToMerge`, затем через dot-path обновление устанавливает: `status = Merged`, `mergedAt = now`, `mergedIntoBranchId = parentBranchId`. Доступно любому участнику. После вызова ветка считается слитой — дальнейшие изменения MR не допускаются. |


### Signature

```kotlin
suspend fun patchMergeFinalize(
  conversationId: String,
  branchId: String
)
```

### Parameters

| Parameter      | Req | Type   | Description                                              |
|----------------|-----|--------|----------------------------------------------------------|
| conversationId | Y   | String | ID conversation, в которой находится ветка.              |
| branchId       | Y   | String | ID ветки с MR в статусе `ReadyToMerge`.                  |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что ветка переведена в статус `Merged`.

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "branchId": "branch-ghi012"
}
```

### Response:

Поле `mergeRequest` в документе ветки после финализации:

```json
{
  "mergeRequest": {
    "status": "merged",
    "initiatorUid": "uid-alice",
    "requestedAt": "2026-06-13T16:00:00Z",
    "approvedByUids": ["uid-alice", "uid-bob"],
    "mergedAt": "2026-06-13T17:00:00Z",
    "mergedIntoBranchId": "conv-xyz789"
  }
}
```

### Errors List:

| Exception                  | Condition                                                                         |
|----------------------------|-----------------------------------------------------------------------------------|
| IllegalStateException      | Нет активного merge request (`mergeRequest == null`).                             |
| IllegalStateException      | Статус MR не `ReadyToMerge` (например, всё ещё `Open` или уже `Merged`).         |
| FirebaseFirestoreException | Ошибка сети, конфликт транзакции или нарушение Firestore Security Rules.          |
