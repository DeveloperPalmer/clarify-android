---
tags:
  - method
---
# observe-unread-count

**Summary**: Живая подписка на счётчик непрочитанных сообщений conversation для текущего пользователя.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Открывает snapshot listener на документ `conversations/{conversationId}/unreadCommits/{userId}`. Эмитит числовое значение поля `count`. Если документ не существует (ни одного непрочитанного ещё не было) — эмитит `0`. UID текущего пользователя берётся из `AuthSessionPersistence`. |


### Signature

```kotlin
fun observeUnreadCount(conversationId: String): Flow<Long>
```

### Parameters

| Parameter      | Req | Type   | Description                                         |
|----------------|-----|--------|-----------------------------------------------------|
| conversationId | Y   | String | ID conversation, за счётчиком которой наблюдаем. |

### Response parameters

| Тип    | Description                                                                          |
|--------|--------------------------------------------------------------------------------------|
| `Long` | Текущее количество непрочитанных сообщений. `0` если документ отсутствует в Firestore. |

### Request:

```json
{
  "conversationId": "conv-xyz789"
}
```

### Response:

Каждая эмиссия Flow:

```json
5
```

### Errors List:

| Exception                  | Condition                                                        |
|----------------------------|------------------------------------------------------------------|
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.                        |
| FirebaseFirestoreException | Ошибка сети — Flow завершается с ошибкой через `close(error)`.  |
