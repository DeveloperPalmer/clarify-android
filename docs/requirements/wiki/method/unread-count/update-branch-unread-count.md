---
tags:
  - method
---
# updateBranchUnreadCount

**Summary**: Сбрасывает счётчик непрочитанных сообщений ветки до нуля для текущего пользователя.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-19

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Записывает `count = 0` в документ `conversations/{conversationId}/branches/{branchId}/unreadCommits/{userId}` через `set(merge)`. Вызывается при открытии экрана ветки, чтобы снять бейдж непрочитанных. |


### Signature

```kotlin
suspend fun updateBranchUnreadCount(
  conversationId: String,
  branchId: String
)
```

### Parameters

| Parameter      | Req | Type   | Description                                              |
|----------------|-----|--------|----------------------------------------------------------|
| conversationId | Y   | String | ID conversation, в которой находится ветка.              |
| branchId       | Y   | String | ID ветки, счётчик которой нужно обнулить.                |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что поле `count` установлено в `0`.

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "branchId": "branch-ghi012"
}
```

### Response:

Документ `conversations/conv-xyz789/branches/branch-ghi012/unreadCommits/uid-alice` после обновления:

```json
{
  "count": 0
}
```

### Errors List:

| Exception                  | Condition                                             |
|----------------------------|-------------------------------------------------------|
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.             |
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.   |
