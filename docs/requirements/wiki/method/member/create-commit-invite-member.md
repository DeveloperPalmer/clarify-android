---
tags:
  - method
---
# createCommitInviteMember

**Summary**: Приглашает пользователя в группу одной атомарной операцией.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-07-05

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Одним batch'ом выполняет три записи: (1) добавляет UID приглашённого в массив `memberUids` через `arrayUnion`; (2) создаёт документ участника в подколлекции `members`; (3) записывает системный commit типа `inviteMember` с полем `visibleFor` (кто видит сообщение), куда передаётся состав группы уже с учётом приглашённых — иначе системный коммит не прошёл бы фильтр видимости запросов и не показался бы. Системный commit НЕ обновляет `lastCommitText/lastCommitAt` и НЕ инкрементит unread — он только фиксирует факт приглашения в истории. |


### Signature

```kotlin
suspend fun createCommitInviteMember(
  conversationId: String,
  invitedUserId: UserId,
  memberUids: List<String>
)
```

### Parameters

| Parameter      | Req | Type          | Description                                                           |
|----------------|-----|---------------|-----------------------------------------------------------------------|
| conversationId | Y   | String        | ID группового conversation, в который добавляется участник.           |
| invitedUserId  | Y   | UserId        | UID пользователя, которого нужно добавить в группу.                   |
| memberUids     | Y   | List\<String\> | Состав группы с учётом приглашённых — попадает в `visibleFor` системного коммита. |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что все три записи применены атомарно.

### Request:

```json
{
  "conversationId": "conv-group001",
  "invitedUserId": "uid-carol",
  "memberUids": ["uid-alice", "uid-bob", "uid-carol"]
}
```

### Response:

Три документа, созданных/обновлённых в Firestore:

**1. Обновлённое поле в `conversations/conv-group001`:**
```json
{
  "memberUids": ["uid-alice", "uid-bob", "uid-carol"]
}
```

**2. Новый документ `conversations/conv-group001/members/uid-carol`:**
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
  "type": "inviteMember",
  "invitedUid": "uid-carol",
  "branchId": "conv-group001",
  "visibleFor": ["uid-alice", "uid-bob", "uid-carol"],
  "createdAt": "2026-06-13T11:00:00Z"
}
```

### Errors List:

| Exception                  | Condition                                             |
|----------------------------|-------------------------------------------------------|
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.             |
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.   |
