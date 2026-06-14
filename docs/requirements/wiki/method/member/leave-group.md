---
tags:
  - method
---
# leaveGroup

**Summary**: Текущий пользователь покидает групповую беседу.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Делегирует выполнение в `deleteMember`, передавая UID текущего пользователя из `AuthSessionPersistence`. Выполняет те же три атомарные операции: убирает из `memberUids`, удаляет документ участника и unreadCommits. |


### Signature

```kotlin
suspend fun leaveGroup(conversationId: String)
```

### Parameters

| Parameter      | Req | Type   | Description                             |
|----------------|-----|--------|-----------------------------------------|
| conversationId | Y   | String | ID группового conversation для выхода.  |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что текущий пользователь удалён из группы.

### Request:

```json
{
  "conversationId": "conv-group001"
}
```

### Response:

После выполнения (аналогично `deleteMember` для текущего пользователя):

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
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.             |
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.   |
