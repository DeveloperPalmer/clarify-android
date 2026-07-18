---
tags:
  - method
---
# deleteBranchCommits

**Summary**: Удаляет сообщения ветки «у всех», физически стирая документы и пересчитывая `lastCommit*` ветки и счётчик непрочитанных ветки у собеседника.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-07-18

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-07-18 |
| Description      | Одной транзакцией физически удаляет все указанные commit'ы ветки (в отличие от `hideCommits`, который скрывает сообщение только у текущего пользователя). Затем по `lastCommit` пересчитывает поля `lastCommit*` ветки: `keep` — оставить как есть (удалили не последнее сообщение); `replace` — переставить на новое последнее оставшееся (`text`, `at`); `clear` — очистить (ветка опустела). В отличие от `deleteDirectCommits`, ветка не хранит `lastCommitSenderUid` — при `replace` переставляются только `text` и `at`. Если `peerUnreadDelta` > 0 — читает текущий счётчик непрочитанных ветки у собеседника и уменьшает его на `peerUnreadDelta`, но не ниже `0` (это наши сообщения, которые собеседник ещё не прочитал). При `peerUnreadDelta` = 0 счётчик не читается и не трогается. |


### Signature

```kotlin
suspend fun deleteBranchCommits(
  conversationId: String,
  branchId: String,
  peerId: String,
  commitIds: List<String>,
  lastCommit: LastCommitParams,
  peerUnreadDelta: Int
)
```

### Parameters

| Parameter       | Req | Type             | Description                                                                                                                                                                                       |
|-----------------|-----|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| conversationId  | Y   | String           | ID conversation, которой принадлежит ветка.                                                                                                                                                      |
| branchId        | Y   | String           | ID ветки, из которой удаляются сообщения.                                                                                                                                                        |
| peerId          | Y   | String           | ID собеседника; по нему пересчитывается его счётчик непрочитанных ветки.                                                                                                                         |
| commitIds       | Y   | List\<String\>   | ID удаляемых commit'ов. Пустой список запрещён.                                                                                                                                                  |
| lastCommit      | Y   | LastCommitParams | Что сделать с полями `lastCommit*` ветки после удаления:<br>\* keep — не трогать<br>\* replace — переставить на новое последнее сообщение (`text`, `at`; без `senderUid`)<br>\* clear — очистить (ветка опустела) |
| peerUnreadDelta | Y   | Int              | На сколько уменьшить счётчик непрочитанных ветки у собеседника — число удаляемых наших сообщений, которые он ещё не прочитал. `0` — счётчик не трогаем.                                            |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что удаление документов и пересчёт `lastCommit*` ветки и счётчика непрочитанных применены атомарно.

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "branchId": "branch-123",
  "peerId": "uid-bob",
  "commitIds": ["commit-def456", "commit-zzz"],
  "lastCommit": {
    "mode": "replace",
    "text": "See you tomorrow",
    "at": "2026-07-18T09:12:00Z"
  },
  "peerUnreadDelta": 2
}
```

Варианты `lastCommit` без пересчёта нового последнего сообщения:

```json
"lastCommit": { "mode": "keep" }
```
```json
"lastCommit": { "mode": "clear" }
```

### Response:

**Удалённые commit'ы** `conversations/conv-xyz789/commits/commit-def456`, `conversations/conv-xyz789/commits/commit-zzz` — документы удалены.

**Ветка** `conversations/conv-xyz789/branches/branch-123` — `lastCommit*` переставлены на новое последнее оставшееся сообщение (`mode: replace`):
```json
{
  "lastCommitText": "See you tomorrow",
  "lastCommitAt": "2026-07-18T09:12:00Z"
}
```

**Счётчик непрочитанных ветки у собеседника** `conversations/conv-xyz789/branches/branch-123/unreadCommits/uid-bob` — уменьшен на `peerUnreadDelta`, но не ниже `0`:
```json
{
  "count": 3
}
```

### Errors List:

| Exception                  | Condition                                                                    |
|----------------------------|------------------------------------------------------------------------------|
| IllegalArgumentException   | `commitIds` пуст — вызов без элементов считается багом на коллсайте.          |
| FirebaseFirestoreException | Ошибка сети, нарушение Firestore Security Rules или конфликт транзакции.      |
