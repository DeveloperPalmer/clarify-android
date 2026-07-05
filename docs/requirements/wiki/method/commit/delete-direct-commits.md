---
tags:
  - method
---
# deleteDirectCommits

**Summary**: Удаляет сообщения direct-чата «у всех», физически стирая документы и пересчитывая `lastCommit*` беседы и счётчик непрочитанных собеседника.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-07-05

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-07-05 |
| Description      | Одной транзакцией физически удаляет все указанные commit'ы (в отличие от `hideCommits`, который скрывает сообщение только у текущего пользователя). Затем по `lastCommit` пересчитывает поля `lastCommit*` беседы: `keep` — оставить как есть (удалили не последнее сообщение); `replace` — переставить на новое последнее оставшееся (`text`, `senderUid`, `at`); `clear` — очистить (корень опустел). Если `peerUnreadDelta` > 0 — читает текущий счётчик непрочитанных собеседника и уменьшает его на `peerUnreadDelta`, но не ниже `0` (это наши сообщения, которые собеседник ещё не прочитал). При `peerUnreadDelta` = 0 счётчик не читается и не трогается. |


### Signature

```kotlin
suspend fun deleteDirectCommits(
  conversationId: String,
  peerId: UserId,
  commitIds: List<String>,
  lastCommit: LastCommitParams,
  peerUnreadDelta: Int
)
```

### Parameters

| Parameter       | Req | Type             | Description                                                                                                                                                                          |
|-----------------|-----|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| conversationId  | Y   | String           | ID conversation, из которой удаляются сообщения.                                                                                                                                    |
| peerId          | Y   | UserId           | ID собеседника; по нему пересчитывается его счётчик непрочитанных.                                                                                                                  |
| commitIds       | Y   | List\<String\>   | ID удаляемых commit'ов. Пустой список запрещён.                                                                                                                                     |
| lastCommit      | Y   | LastCommitParams | Что сделать с полями `lastCommit*` беседы после удаления:<br>\* keep — не трогать<br>\* replace — переставить на новое последнее сообщение (`text`, `senderUid`, `at`)<br>\* clear — очистить (корень опустел) |
| peerUnreadDelta | Y   | Int              | На сколько уменьшить счётчик непрочитанных собеседника — число удаляемых наших сообщений, которые он ещё не прочитал. `0` — счётчик не трогаем.                                       |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что удаление документов и пересчёт `lastCommit*` и счётчика непрочитанных применены атомарно.

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "peerId": "uid-bob",
  "commitIds": ["commit-def456", "commit-zzz"],
  "lastCommit": {
    "mode": "replace",
    "text": "See you tomorrow",
    "senderUid": "uid-alice",
    "at": "2026-07-05T09:12:00Z"
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

**Беседа** `conversations/conv-xyz789` — `lastCommit*` переставлены на новое последнее оставшееся сообщение (`mode: replace`):
```json
{
  "lastCommitText": "See you tomorrow",
  "lastCommitSenderUid": "uid-alice",
  "lastCommitAt": "2026-07-05T09:12:00Z"
}
```

**Счётчик непрочитанных собеседника** `conversations/conv-xyz789/unreadCommits/uid-bob` — уменьшен на `peerUnreadDelta`, но не ниже `0`:
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
