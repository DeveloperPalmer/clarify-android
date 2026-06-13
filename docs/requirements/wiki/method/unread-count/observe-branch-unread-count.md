---
tags:
  - method
---
# observe-branch-unread-count

**Summary**: Живая подписка на счётчик непрочитанных сообщений ветки для текущего пользователя.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Открывает snapshot listener на документ `conversations/{conversationId}/branches/{branchId}/unreadCommits/{userId}`. Эмитит числовое значение поля `count`. Если документ не существует — эмитит `0`. UID текущего пользователя берётся из `AuthSessionPersistence`. |


### Signature

```kotlin
fun observeBranchUnreadCount(
  conversationId: String,
  branchId: String
): Flow<Long>
```

### Parameters

| Parameter      | Req | Type   | Description                                                    |
|----------------|-----|--------|----------------------------------------------------------------|
| conversationId | Y   | String | ID conversation, в которой находится ветка.                    |
| branchId       | Y   | String | ID ветки, счётчик непрочитанных которой отслеживаем.           |

### Response parameters

| Тип    | Description                                                                             |
|--------|-----------------------------------------------------------------------------------------|
| `Long` | Текущее количество непрочитанных сообщений в ветке. `0` если документ отсутствует.     |

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
3
```

### Errors List:

| Exception                  | Condition                                                        |
|----------------------------|------------------------------------------------------------------|
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.                        |
| FirebaseFirestoreException | Ошибка сети — Flow завершается с ошибкой через `close(error)`.  |
