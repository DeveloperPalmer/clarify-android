---
tags:
  - method
---
# hideCommits

**Summary**: Скрывает сообщения «только у себя», физически удаляя документ, когда его не видит больше никто.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-07-05

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-07-05 |
| Description      | Одной транзакцией для каждого указанного commit: читает документ и удаляет UID текущего пользователя из `visibleFor` (`FieldValue.arrayRemove`). Если после удаления `visibleFor` пустеет — документ удаляется физически (сообщение скрыли все участники, хранить его больше незачем). Несуществующие документы пропускаются — сообщение могли параллельно удалить «у всех». Скрытое сообщение перестаёт приходить в запросы с фильтром видимости у скрывшего, у остальных участников остаётся видимым. `lastCommit*`-поля conversation и счётчики unread не пересчитываются. |


### Signature

```kotlin
suspend fun hideCommits(
  conversationId: String,
  commitIds: List<String>
)
```

### Parameters

| Parameter      | Req | Type           | Description                                                |
|----------------|-----|----------------|------------------------------------------------------------|
| conversationId | Y   | String         | ID conversation, в которой скрываются сообщения.           |
| commitIds      | Y   | List\<String\> | ID скрываемых commit'ов. Пустой список запрещён.           |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что все изменения применены атомарно.

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "commitIds": ["commit-def456", "commit-zzz"]
}
```

### Response:

**Commit, который ещё видит собеседник** `conversations/conv-xyz789/commits/commit-def456`:
```json
{
  "visibleFor": ["uid-bob"]
}
```

**Commit, который скрыли оба участника** `conversations/conv-xyz789/commits/commit-zzz` — документ удалён.

### Errors List:

| Exception                  | Condition                                             |
|----------------------------|-------------------------------------------------------|
| IllegalArgumentException   | `commitIds` пуст — вызов без элементов считается багом на коллсайте. |
| IllegalStateException      | UID не найден в `AuthSessionPersistence`.             |
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.   |
