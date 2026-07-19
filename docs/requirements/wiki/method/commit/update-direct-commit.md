---
tags:
  - method
---
# updateDirectCommit

**Summary**: Редактирует текст сообщения direct-чата, помечая его как изменённое; при правке последнего сообщения беседы обновляет её превью.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-07-19

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-07-19 |
| Description      | Заменяет `text` существующего commit-документа и проставляет `editedAt` — момент правки. `createdAt` не меняется: позиция сообщения в истории, курсоры пагинации и отметки прочтения остаются прежними. Всё выполняется одной транзакцией: сначала проверяется существование коммита (отсутствует — типизированная ошибка, документ не создаётся заново), затем пишется новый текст. Если правится текущее последнее сообщение беседы (`lastCommitAt` беседы совпадает с `createdAt` коммита) — превью `lastCommitText` беседы обновляется тем же текстом; `lastCommitSenderUid`/`lastCommitAt` не трогаются. Сравнение выполняется по серверным данным внутри транзакции, что исключает гонку с параллельной отправкой более нового сообщения. Транзакция требует соединения с сервером — офлайн-редактирование невозможно. |

### Signature

```kotlin
suspend fun updateDirectCommit(
  conversationId: String,
  commitId: String,
  text: String
)
```

### Parameters

| Parameter      | Req | Type   | Description                                          |
|----------------|-----|--------|------------------------------------------------------|
| conversationId | Y   | String | ID беседы, которой принадлежит сообщение.            |
| commitId       | Y   | String | ID редактируемого сообщения.                         |
| text           | Y   | String | Новый текст. Не может быть пустым.                   |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что все записи применены атомарно.

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "commitId": "commit-abc123",
  "text": "Hello, edited!"
}
```

### Response:

**Обновлённый commit** `conversations/conv-xyz789/commits/commit-abc123`:
```json
{
  "text": "Hello, edited!",
  "editedAt": "2026-07-19T14:30:00Z"
}
```

**Обновлённое превью беседы** (только если правилось последнее сообщение):
```json
{
  "lastCommitText": "Hello, edited!"
}
```

### Errors List:

| Exception                  | Condition                                                             |
|----------------------------|-----------------------------------------------------------------------|
| IllegalArgumentException   | `text` пустой после trim — невалидный вызов.                          |
| CommitNotFoundException    | Сообщение не существует (удалено «у всех» параллельно).               |
| FirebaseFirestoreException | Ошибка сети, офлайн или нарушение Firestore Security Rules.           |
