---
tags:
  - method
---
# updateUnreadCount

**Summary**: Сбрасывает счётчик непрочитанных сообщений conversation до нуля для текущего пользователя.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-19

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Записывает `count = 0` в документ `conversations/{conversationId}/unreadCommits/{userId}` через `set(merge)`. Вызывается при открытии чата или прокрутке до конца ленты, чтобы обнулить бейдж непрочитанных. |


### Signature

```kotlin
suspend fun updateUnreadCount(conversationId: String)
```

### Parameters

| Parameter      | Req | Type   | Description                                        |
|----------------|-----|--------|----------------------------------------------------|
| conversationId | Y   | String | ID conversation, счётчик которой нужно обнулить.  |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что поле `count` установлено в `0`.

### Request:

```json
{
  "conversationId": "conv-xyz789"
}
```

### Response:

Документ `conversations/conv-xyz789/unreadCommits/uid-alice` после обновления:

```json
{
  "count": 0
}
```

### Errors List:

| Exception                  | Condition                                                    |
|----------------------------|--------------------------------------------------------------|
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.                    |
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.          |
