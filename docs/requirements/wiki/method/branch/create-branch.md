---
tags:
  - method
---
# createBranch

**Summary**: Создаёт новую ветку внутри conversation, ответвляясь от конкретного commit'а.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-19

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Генерирует UUID для новой ветки, записывает документ ветки в подколлекцию `conversations/{conversationId}/branches` и возвращает готовый объект `BranchNM` без дополнительного чтения из Firestore. Документ ветки содержит ссылку на родительскую ветку и commit, от которого произошло ответвление. |


### Signature

```kotlin
suspend fun createBranch(
  conversationId: String,
  parentBranchId: String,
  branchedFromCommitId: String,
  name: String
): BranchNM
```

### Parameters

| Parameter            | Req | Type   | Description                                                                                      |
|----------------------|-----|--------|--------------------------------------------------------------------------------------------------|
| conversationId       | Y   | String | ID conversation, в которой создаётся ветка.                                                      |
| parentBranchId       | Y   | String | ID родительской ветки. Для ветки от корня — это `conversationId`.                                |
| branchedFromCommitId | Y   | String | ID commit'а, от которого отответвляется новая ветка. Зафиксирован в документе для истории.      |
| name                 | Y   | String | Название новой ветки (видно участникам).                                                         |

### Response parameters

| Parameter            | Req | Type       | Description                                              |
|----------------------|-----|------------|----------------------------------------------------------|
| id                   | Y   | String     | UUID новой ветки.                                        |
| parentBranchId       | Y   | String     | ID родительской ветки (из параметра).                    |
| branchedFromCommitId | Y   | String     | ID commit'а ответвления (из параметра).                  |
| name                 | Y   | String     | Название ветки (из параметра).                           |
| createdAt            | Y   | Timestamp  | Серверный штамп создания.                                |
| lastCommitAt         | N   | Timestamp? | Всегда `null` сразу после создания.                      |
| createdByUid         | Y   | String     | UID создателя ветки.                                     |
| mergeRequest         | N   | Object?    | Всегда `null` сразу после создания.                      |

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "parentBranchId": "conv-xyz789",
  "branchedFromCommitId": "commit-def456",
  "name": "Feature discussion"
}
```

### Response:

```json
{
  "id": "branch-ghi012",
  "parentBranchId": "conv-xyz789",
  "branchedFromCommitId": "commit-def456",
  "name": "Feature discussion",
  "createdAt": "2026-06-13T15:00:00Z",
  "lastCommitAt": null,
  "createdByUid": "uid-alice",
  "mergeRequest": null
}
```

### Errors List:

| Exception                  | Condition                                             |
|----------------------------|-------------------------------------------------------|
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.             |
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.   |
