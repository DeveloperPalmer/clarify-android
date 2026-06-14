---
tags:
  - method
---
# observe-members

**Summary**: Живая подписка на список участников групповой беседы.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Открывает snapshot listener на подколлекцию `conversations/{conversationId}/members`. Возвращает изменения (ADDED / MODIFIED / REMOVED) при каждом вступлении или выходе участника. Открытие трекается через `FirestoreListenerGuard`. |


### Signature

```kotlin
fun observeMembers(conversationId: String): Flow<List<FirestoreChange<MemberNM>>>
```

### Parameters

| Parameter      | Req | Type   | Description                                              |
|----------------|-----|--------|----------------------------------------------------------|
| conversationId | Y   | String | ID conversation, список участников которой отслеживаем. |

### Response parameters

| Parameter             | Req | Type                   | Description                                               |
|-----------------------|-----|------------------------|-----------------------------------------------------------|
| [].changeType | Y   | String     | Тип изменения документа участника.<br>\* added<br>\* modified<br>\* removed |
| [].hasPendingWrites   | N   | Boolean                | Всегда `false` для этого метода.                          |
| [].data.id            | Y   | String                 | UID пользователя.                                         |
| [].data.lastReadAt    | N   | Timestamp?             | Время последнего прочитанного сообщения. `null` — не читал. |

### Request:

```json
{
  "conversationId": "conv-group001"
}
```

### Response:

Каждая эмиссия Flow — список изменений:

```json
[
  {
    "changeType": "added",
    "hasPendingWrites": false,
    "data": {
      "id": "uid-carol",
      "lastReadAt": null
    }
  }
]
```

### Errors List:

| Exception                  | Condition                                                       |
|----------------------------|-----------------------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети — Flow завершается с ошибкой через `close(error)`. |
