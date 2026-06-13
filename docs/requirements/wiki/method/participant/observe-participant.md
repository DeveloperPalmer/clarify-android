---
tags:
  - method
---
# observe-participant

**Summary**: Живая подписка на документ конкретного участника conversation.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Открывает snapshot listener на документ `conversations/{conversationId}/participants/{userId}`. Эмитит данные участника или `null` если документ не существует. Используется для наблюдения за `lastReadAt` конкретного пользователя — например, для показа индикатора «прочитано». Открытие трекается через `FirestoreListenerGuard`. |


### Signature

```kotlin
fun observeParticipant(
  conversationId: String,
  userId: UserId
): Flow<ParticipantNM?>
```

### Parameters

| Parameter      | Req | Type   | Description                                                   |
|----------------|-----|--------|---------------------------------------------------------------|
| conversationId | Y   | String | ID conversation, к которой принадлежит участник.             |
| userId         | Y   | UserId | UID пользователя, за документом которого ведётся наблюдение. |

### Response parameters

| Parameter  | Req | Type       | Description                                              |
|------------|-----|------------|----------------------------------------------------------|
| id         | Y   | String     | UID пользователя (дублирует ID документа).               |
| lastReadAt | N   | Timestamp? | Время последнего прочитанного сообщения. `null` — не читал. |

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "userId": "uid-bob"
}
```

### Response:

Каждая эмиссия Flow:

```json
{
  "id": "uid-bob",
  "lastReadAt": "2026-06-13T14:35:00Z"
}
```

### Errors List:

| Exception                  | Condition                                                       |
|----------------------------|-----------------------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети — Flow завершается с ошибкой через `close(error)`. |
