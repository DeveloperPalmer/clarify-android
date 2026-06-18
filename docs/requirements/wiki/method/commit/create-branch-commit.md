---
tags:
  - method
---
# createBranchCommit

**Summary**: Отправляет сообщение в существующую ветку разговора.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-19

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Аналог non-root ветки в `createDirectCommit`, но без `peerId` — conversation уже существует. Одним batch'ом: (1) записывает commit с указанным `branchId`; (2) merge-обновляет `lastCommit*`-поля документа ветки; (3) инкрементит `branchUnreadCommits` всем участникам кроме отправителя. Список участников передаёт caller. |


### Signature

```kotlin
suspend fun createBranchCommit(
  conversationId: String,
  branchId: String,
  text: String,
  memberUids: List<String>
)
```

### Parameters

| Parameter       | Req | Type          | Description                                                                                 |
|-----------------|-----|---------------|---------------------------------------------------------------------------------------------|
| conversationId  | Y   | String        | ID conversation, в которой находится ветка.                                                  |
| branchId        | Y   | String        | ID ветки, в которую отправляется сообщение.                                                  |
| text            | Y   | String        | Текст сообщения.                                                                             |
| memberUids | Y   | List\<String\> | Полный список UID участников conversation. Нужен для инкремента branch-unread всем кроме отправителя. |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что commit и обновления ветки применены атомарно.

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "branchId": "branch-ghi012",
  "text": "I think we should refactor this",
  "memberUids": ["uid-alice", "uid-bob"]
}
```

### Response:

**Новый commit** `conversations/conv-xyz789/commits/commit-new`:
```json
{
  "clientCommitId": "commit-new",
  "senderUid": "uid-alice",
  "text": "I think we should refactor this",
  "type": "text",
  "createdAt": "2026-06-13T15:10:00Z",
  "branchId": "branch-ghi012"
}
```

**Обновлённая ветка** `conversations/conv-xyz789/branches/branch-ghi012`:
```json
{
  "lastCommitText": "I think we should refactor this",
  "lastCommitAt": "2026-06-13T15:10:00Z"
}
```

**Unread increment** для uid-bob `conversations/conv-xyz789/branches/branch-ghi012/unreadCommits/uid-bob`:
```json
{ "count": 1 }
```

### Errors List:

| Exception                  | Condition                                             |
|----------------------------|-------------------------------------------------------|
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.             |
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.   |
