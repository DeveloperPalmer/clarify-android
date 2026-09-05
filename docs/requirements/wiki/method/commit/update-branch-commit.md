---
tags:
  - method
---
# updateBranchCommit

**Summary**: Редактирует текст сообщения ветки, помечая его как изменённое; при правке последнего сообщения ветки обновляет её превью.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-09-05

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-09-05 |
| Description      | Заменяет `text` существующего commit-документа и проставляет `editedAt` — момент правки. `createdAt` не меняется: позиция сообщения в истории, курсоры пагинации и отметки прочтения остаются прежними. Всё выполняется одной транзакцией: сначала проверяется существование коммита (отсутствует — типизированная ошибка, документ не создаётся заново), затем пишется новый текст. Если правится текущее последнее сообщение ветки (`lastCommitAt` ветки совпадает с `createdAt` коммита) — превью обновляется в документе ветки парой `lastCommitText` + `lastCommitAt`; в этом отличие от `updateDirectCommit`, где превью живёт в документе беседы и время не переписывается. Сравнение выполняется по серверным данным внутри транзакции, что исключает гонку с параллельной отправкой более нового сообщения. Транзакция требует соединения с сервером — офлайн-редактирование невозможно. |

### Signature

```kotlin
suspend fun updateBranchCommit(
  conversationId: String,
  branchId: String,
  commitId: String,
  text: String
)
```

### Parameters

| Parameter      | Req | Type   | Description                               |
|----------------|-----|--------|-------------------------------------------|
| conversationId | Y   | String | ID беседы, которой принадлежит ветка.     |
| branchId       | Y   | String | ID ветки, превью которой может обновиться. |
| commitId       | Y   | String | ID редактируемого сообщения.              |
| text           | Y   | String | Новый текст. Не может быть пустым.        |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что все записи применены атомарно.

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "branchId": "branch-ghi012",
  "commitId": "commit-branch01",
  "text": "Let's discuss this here, edited!"
}
```

### Response:

**Обновлённый commit** `conversations/conv-xyz789/commits/commit-branch01`:
```json
{
  "text": "Let's discuss this here, edited!",
  "editedAt": "2026-09-05T14:30:00Z"
}
```

**Обновлённое превью ветки** (только если правилось последнее сообщение ветки):
```json
{
  "lastCommitText": "Let's discuss this here, edited!",
  "lastCommitAt": "2026-06-13T16:00:00Z"
}
```

### Errors List:

| Exception                  | Condition                                                   |
|----------------------------|-------------------------------------------------------------|
| IllegalArgumentException   | `text` пустой после trim — невалидный вызов.                |
| CommitNotFoundException    | Сообщение не существует (удалено «у всех» параллельно).     |
| FirebaseFirestoreException | Ошибка сети, офлайн или нарушение Firestore Security Rules. |
