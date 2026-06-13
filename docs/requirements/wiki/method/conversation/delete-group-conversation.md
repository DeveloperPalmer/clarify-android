---
tags:
  - method
---
# deleteGroupConversation

**Summary**: Удаляет документ групповой беседы.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Удаляет только корневой документ `conversations/{conversationId}`. Подколлекции commits/branches/participants/unreadCommits остаются сиротами — осознанный технический долг MVP. Полная чистка отложена на server-side trigger / recursive delete в будущем. |


### Signature

```kotlin
suspend fun deleteGroupConversation(conversationId: String)
```

### Parameters

| Parameter      | Req | Type   | Description                            |
|----------------|-----|--------|----------------------------------------|
| conversationId | Y   | String | ID группового conversation для удаления. |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что корневой документ удалён.

### Request:

```json
{
  "conversationId": "conv-group001"
}
```

### Response:

Документ `conversations/conv-group001` удалён. Подколлекции остаются:

```
conversations/conv-group001            ← УДАЛЁН
conversations/conv-group001/commits/   ← остаётся (сирота)
conversations/conv-group001/branches/  ← остаётся (сирота)
conversations/conv-group001/participants/ ← остаётся (сирота)
conversations/conv-group001/unreadCommits/ ← остаётся (сирота)
```

### Errors List:

| Exception                  | Condition                                             |
|----------------------------|-------------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.   |
