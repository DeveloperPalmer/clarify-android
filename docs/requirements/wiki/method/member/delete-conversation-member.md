---
tags:
  - method
---
# deleteConversationMember

**Summary**: Удаляет участника из группы одной атомарной операцией.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-19

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Одним batch'ом: (1) убирает UID из массива `memberUids` через `arrayRemove`; (2) удаляет документ участника из подколлекции `members`; (3) удаляет документ `unreadCommits` данного пользователя. Остальные подколлекции (commits, branches) остаются нетронутыми. |


### Signature

```kotlin
suspend fun deleteConversationMember(
  conversationId: String,
  userId: UserId
)
```

### Parameters

| Parameter      | Req | Type   | Description                                                    |
|----------------|-----|--------|----------------------------------------------------------------|
| conversationId | Y   | String | ID группового conversation.                                    |
| userId         | Y   | UserId | UID пользователя, которого нужно удалить из группы.            |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что все три операции применены атомарно.

### Request:

```json
{
  "conversationId": "conv-group001",
  "userId": "uid-carol"
}
```

### Response:

После выполнения в Firestore:

**1. Обновлённое поле в `conversations/conv-group001`:**
```json
{
  "memberUids": ["uid-alice", "uid-bob"]
}
```

**2. Удалён документ** `conversations/conv-group001/members/uid-carol`

**3. Удалён документ** `conversations/conv-group001/unreadCommits/uid-carol`

### Errors List:

| Exception                  | Condition                                             |
|----------------------------|-------------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.   |
