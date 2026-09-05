---
tags:
  - method
---
# readCommits

**Summary**: Загружает порцию сообщений ветки, опционально раньше указанного курсора.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-09-05

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Выполняет разовый запрос к подколлекции `conversations/{conversationId}/commits` с фильтром по `branchId`, `whereArrayContains(visibleFor, uid)` текущего пользователя, сортировкой по `createdAt DESC` и лимитом `limit`. Фильтр видимости применяется всегда: скрытые текущим пользователем сообщения не попадают в ответ, страница всегда содержит до `limit` видимых сообщений. Если передан `before` — страница начинается сразу после этого курсора (`startAfter`), реализуя пагинацию «вниз по ленте». Курсор — пара «время создания + ID сообщения»: ID служит вторым ключом сортировки, поэтому сообщения с одинаковым `createdAt` не пропускаются и не задваиваются между страницами. Параметр `source` выбирает, откуда брать данные: первая страница читается по умолчанию (можно из кэша), догрузка истории — только с сервера, иначе неполный кэш ответил бы короткой страницей и пагинация остановилась бы раньше времени. |

### Signature

```kotlin
suspend fun readCommits(
  conversationId: String,
  branchId: String,
  limit: Long,
  before: CommitCursor?,
  source: Source = Source.DEFAULT
): List<CommitNM>
```

### Parameters

| Parameter        | Req | Type          | Description                                                                                    |
|------------------|-----|---------------|------------------------------------------------------------------------------------------------|
| conversationId   | Y   | String        | ID conversation, из которой загружаем сообщения.                                               |
| branchId         | Y   | String        | ID ветки. Фильтрует только сообщения этой ветки.                                               |
| limit            | Y   | Long          | Максимальное количество сообщений в ответе.                                                    |
| before           | N   | CommitCursor? | Верхняя граница страницы, исключительно: возвращаются сообщения строго раньше курсора. `null` — с самого свежего. |
| before.createdAt | Y   | Timestamp     | Время создания граничного сообщения.                                                           |
| before.id        | Y   | String        | ID граничного сообщения — тай-брейк для сообщений с одинаковым временем.                       |
| source           | N   | String        | Откуда читать.<br>\* default — можно из кэша<br>\* cache — только из кэша<br>\* server — только с сервера |

### Response parameters

| Parameter      | Req | Type           | Description                                                              |
|----------------|-----|----------------|--------------------------------------------------------------------------|
| [].id          | Y   | String         | ID документа commit.                                                     |
| [].senderUid   | Y   | String         | UID отправителя.                                                         |
| [].text        | Y   | String         | Текст сообщения.                                                         |
| [].type        | Y   | String         | Тип сообщения.<br>\* text<br>\* inviteMember                             |
| [].createdAt   | Y   | Timestamp      | Время создания (DESC-порядок в ответе).                                  |
| [].editedAt    | N   | Timestamp?     | Время правки. `null` — сообщение не редактировалось.                     |
| [].branchId    | Y   | String         | ID ветки.                                                                |
| [].visibleFor  | Y   | List\<String\> | UID участников, которым видно сообщение.                                 |

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "branchId": "branch-ghi012",
  "limit": 50,
  "before": {
    "createdAt": "2026-06-13T14:40:00Z",
    "id": "commit-aaa111"
  },
  "source": "server"
}
```

### Response:

```json
[
  {
    "id": "commit-zzz",
    "senderUid": "uid-bob",
    "text": "Sounds great!",
    "type": "text",
    "createdAt": "2026-06-13T14:35:00Z",
    "editedAt": null,
    "branchId": "conv-xyz789",
    "visibleFor": ["uid-alice", "uid-bob"]
  },
  {
    "id": "commit-def456",
    "senderUid": "uid-alice",
    "text": "Hello!",
    "type": "text",
    "createdAt": "2026-06-13T14:30:00Z",
    "editedAt": "2026-06-13T14:31:00Z",
    "branchId": "conv-xyz789",
    "visibleFor": ["uid-alice", "uid-bob"]
  }
]
```

### Errors List:

| Exception                  | Condition                                           |
|----------------------------|-----------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules. |
