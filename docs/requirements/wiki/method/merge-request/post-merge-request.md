---
tags:
  - method
---
# postMergeRequest

**Summary**: Открывает merge request для ветки. Атомарная операция с проверкой отсутствия активного MR.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Выполняет Firestore-транзакцию: читает текущий документ ветки, проверяет что `mergeRequest == null`, и записывает поле `mergeRequest` со статусом `Open`. Если MR уже существует — транзакция падает с ошибкой. Инициатор НЕ добавляется в `approvedByUids` автоматически — он должен явно нажать approve. |


### Signature

```kotlin
suspend fun postMergeRequest(
  conversationId: String,
  branchId: String
)
```

### Parameters

| Parameter      | Req | Type   | Description                                                        |
|----------------|-----|--------|--------------------------------------------------------------------|
| conversationId | Y   | String | ID conversation, в которой находится ветка.                        |
| branchId       | Y   | String | ID ветки, для которой открывается merge request.                   |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что поле `mergeRequest` записано в документ ветки.

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "branchId": "branch-ghi012"
}
```

### Response:

Обновлённый документ ветки `conversations/conv-xyz789/branches/branch-ghi012`:

```json
{
  "id": "branch-ghi012",
  "mergeRequest": {
    "status": "open",
    "initiatorUid": "uid-alice",
    "requestedAt": "2026-06-13T16:00:00Z",
    "approvedByUids": [],
    "mergedAt": null,
    "mergedIntoBranchId": null
  }
}
```

### Errors List:

| Exception                  | Condition                                                                       |
|----------------------------|---------------------------------------------------------------------------------|
| IllegalStateException      | У ветки уже есть активный merge request (`mergeRequest != null`).               |
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.                                       |
| FirebaseFirestoreException | Ошибка сети, конфликт транзакции или нарушение Firestore Security Rules.        |
