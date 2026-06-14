---
tags:
  - method
---
# getCommits

**Summary**: Загружает порцию сообщений ветки, опционально раньше указанного момента.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Выполняет разовый запрос к подколлекции `conversations/{conversationId}/commits` с фильтром по `branchId`, сортировкой по `createdAt DESC` и лимитом `count`. Если передан `before` — добавляет условие `whereLessThan(createdAt, before)`, реализуя пагинацию «вниз по ленте». |


### Signature

```kotlin
suspend fun getCommits(
  conversationId: String,
  branchId: String,
  count: Int,
  before: LocalDateTime?
): List<CommitNM>
```

### Parameters

| Parameter      | Req | Type          | Description                                                                                       |
|----------------|-----|---------------|---------------------------------------------------------------------------------------------------|
| conversationId | Y   | String        | ID conversation, из которой загружаем сообщения.                                                  |
| branchId       | Y   | String        | ID ветки. Фильтрует только сообщения этой ветки.                                                  |
| count          | Y   | Int           | Максимальное количество сообщений в ответе.                                                       |
| before         | N   | LocalDateTime? | Если передан — возвращает только сообщения, созданные до этого момента. `null` — с самого начала. |

### Response parameters

| Parameter       | Req | Type      | Description                                     |
|-----------------|-----|-----------|-------------------------------------------------|
| [].id           | Y   | String    | ID документа commit.                            |
| [].clientCommitId | Y   | String  | Клиентский UUID сообщения.                      |
| [].senderUid    | Y   | String    | UID отправителя.                                |
| [].text         | Y   | String    | Текст сообщения.                                |
| [].type         | Y   | String    | Тип: `"text"` или `"inviteMember"`.        |
| [].createdAt    | Y   | Timestamp | Время создания (DESC-порядок в ответе).         |
| [].colorHex     | Y   | String    | HEX-цвет сообщения.                            |
| [].branchId     | Y   | String    | ID ветки.                                       |

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "branchId": "branch-ghi012",
  "count": 50,
  "before": null
}
```

### Response:

```json
[
  {
    "id": "commit-zzz",
    "clientCommitId": "commit-zzz",
    "senderUid": "uid-bob",
    "text": "Sounds great!",
    "type": "text",
    "createdAt": "2026-06-13T14:35:00Z",
    "colorHex": "#3399FF",
    "branchId": "conv-xyz789"
  },
  {
    "id": "commit-def456",
    "clientCommitId": "commit-def456",
    "senderUid": "uid-alice",
    "text": "Hello!",
    "type": "text",
    "createdAt": "2026-06-13T14:30:00Z",
    "colorHex": "#FF5733",
    "branchId": "conv-xyz789"
  }
]
```

### Errors List:

| Exception                  | Condition                                             |
|----------------------------|-------------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.   |
