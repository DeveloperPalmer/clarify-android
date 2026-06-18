---
tags:
  - method
---
# updateReadWatermark

**Summary**: Записывает отметку о последнем прочитанном сообщении для текущего пользователя.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-19

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Обновляет поле `lastReadAt` в документе участника `conversations/{conversationId}/members/{userId}` через `set(merge)`. Отметка используется для определения, какие сообщения пользователь уже видел. `lastReadAt` конвертируется из `LocalDateTime` в Firestore `Timestamp` перед записью. |


### Signature

```kotlin
suspend fun updateReadWatermark(
  conversationId: String,
  lastReadAt: LocalDateTime
)
```

### Parameters

| Parameter      | Req | Type          | Description                                                                          |
|----------------|-----|---------------|--------------------------------------------------------------------------------------|
| conversationId | Y   | String        | ID conversation, для которой обновляется отметка.                                    |
| lastReadAt     | Y   | LocalDateTime | Время последнего прочитанного сообщения. Конвертируется в Firestore Timestamp.       |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что `lastReadAt` записан в документ участника.

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "lastReadAt": "LocalDateTime.of(2026, 6, 13, 14, 30, 0)"
}
```

### Response:

Документ `conversations/conv-xyz789/members/uid-alice` после обновления:

```json
{
  "id": "uid-alice",
  "lastReadAt": "2026-06-13T14:30:00Z"
}
```

### Errors List:

| Exception                  | Condition                                             |
|----------------------------|-------------------------------------------------------|
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.             |
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.   |
