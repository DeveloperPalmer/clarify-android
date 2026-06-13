---
tags:
  - method
---
# postInviteParticipant

**Summary**: Приглашает пользователя в группу одной атомарной операцией.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Одним batch'ом выполняет три записи: (1) добавляет UID приглашённого в массив `participantUids` через `arrayUnion`; (2) создаёт документ участника в подколлекции `participants`; (3) записывает системный commit типа `inviteParticipant`. Системный commit НЕ обновляет `lastCommitText/lastCommitAt` и НЕ инкрементит unread — он только фиксирует факт приглашения в истории. |


### Signature

```kotlin
suspend fun postInviteParticipant(
  conversationId: String,
  invitedUserId: UserId
)
```

### Parameters

| Parameter      | Req | Type   | Description                                                           |
|----------------|-----|--------|-----------------------------------------------------------------------|
| conversationId | Y   | String | ID группового conversation, в который добавляется участник.           |
| invitedUserId  | Y   | UserId | UID пользователя, которого нужно добавить в группу.                   |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что все три записи применены атомарно.

### Request:

```json
{
  "conversationId": "conv-group001",
  "invitedUserId": "uid-carol"
}
```

### Response:

Три документа, созданных/обновлённых в Firestore:

**1. Обновлённое поле в `conversations/conv-group001`:**
```json
{
  "participantUids": ["uid-alice", "uid-bob", "uid-carol"]
}
```

**2. Новый документ `conversations/conv-group001/participants/uid-carol`:**
```json
{
  "id": "uid-carol",
  "lastReadAt": null
}
```

**3. Системный commit `conversations/conv-group001/commits/commit-xxx`:**
```json
{
  "clientCommitId": "commit-xxx",
  "senderUid": "uid-alice",
  "type": "inviteParticipant",
  "invitedUid": "uid-carol",
  "branchId": "conv-group001",
  "createdAt": "2026-06-13T11:00:00Z"
}
```

### Errors List:

| Exception                  | Condition                                             |
|----------------------------|-------------------------------------------------------|
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.             |
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.   |
