---
tags:
  - method
---
# postGroupCommit

**Summary**: Отправляет сообщение в корневую ветку групповой беседы.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Отличается от `postCommit` тем, что conversation уже существует — не нужно ни создавать её, ни создавать member-документы. Одним batch'ом: (1) записывает commit; (2) merge-обновляет `lastCommit*`-поля conversation; (3) инкрементит `unreadCommits` всем участникам кроме отправителя. Список участников (`memberUids`) передаёт caller из локального кэша. |


### Signature

```kotlin
suspend fun postGroupCommit(
  conversationId: String,
  text: String,
  memberUids: List<String>
)
```

### Parameters

| Parameter       | Req | Type          | Description                                                                               |
|-----------------|-----|---------------|-------------------------------------------------------------------------------------------|
| conversationId  | Y   | String        | ID групповой беседы.                                                                      |
| text            | Y   | String        | Текст сообщения.                                                                          |
| memberUids | Y   | List\<String\> | Полный список UID участников группы из локального кэша. Нужен для инкремента unread всем кроме отправителя. |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что commit записан и unread обновлены атомарно.

### Request:

```json
{
  "conversationId": "conv-group001",
  "text": "Good morning!",
  "memberUids": ["uid-alice", "uid-bob", "uid-carol"]
}
```

### Response:

**Новый commit** `conversations/conv-group001/commits/commit-new`:
```json
{
  "clientCommitId": "commit-new",
  "senderUid": "uid-alice",
  "text": "Good morning!",
  "type": "text",
  "createdAt": "2026-06-13T09:00:00Z",
  "branchId": "conv-group001"
}
```

**Обновлённая conversation** `conversations/conv-group001`:
```json
{
  "lastCommitText": "Good morning!",
  "lastCommitSenderUid": "uid-alice",
  "lastCommitAt": "2026-06-13T09:00:00Z"
}
```

**Unread increment** для uid-bob и uid-carol (не для uid-alice):
```json
{ "count": 1 }
```

### Errors List:

| Exception                  | Condition                                             |
|----------------------------|-------------------------------------------------------|
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.             |
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.   |
