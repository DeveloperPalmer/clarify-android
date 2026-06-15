---
tags:
  - method
---
# postCreateGroupConversation

**Summary**: Создаёт групповую беседу и записывает создателя как первого участника.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Атомарно создаёт документ `conversations/{conversationId}` (тип `group`) и документ участника `conversations/{conversationId}/members/{ownerId}` в одном batch. `conversationId` генерируется как UUID. В отличие от direct-чата (ленивая инициация при первом сообщении), группа материализуется немедленно. Подколлекции commits/branches/unreadCommits появляются лениво при первом сообщении. |


### Signature

```kotlin
suspend fun postCreateGroupConversation(name: String): String
```

### Parameters

| Parameter | Req | Type   | Description                     |
|-----------|-----|--------|---------------------------------|
| name      | Y   | String | Название группы (видно всем участникам). |

### Response parameters

| Тип    | Description                                               |
|--------|-----------------------------------------------------------|
| String | Сгенерированный UUID новой группы — `conversationId`.    |

### Request:

```json
{
  "name": "Project Team"
}
```

### Response:

```json
"conv-group001"
```

Документ `conversations/conv-group001` после записи:

```json
{
  "id": "conv-group001",
  "type": "group",
  "name": "Project Team",
  "ownerUid": "uid-alice",
  "memberUids": ["uid-alice"],
  "lastCommitText": null,
  "lastCommitSenderUid": null,
  "lastCommitAt": null,
  "createdAt": "2026-06-13T10:00:00Z"
}
```

### Errors List:

| Exception                  | Condition                                             |
|----------------------------|-------------------------------------------------------|
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.             |
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.   |
