# SQLDelight — соглашение об именовании

Имя `.sq`-запроса описывает **SQL-операцию**, не доменное действие. Доменная
семантика — ответственность Repository, который зовёт запрос.

## Правила

| SQL | Имя |
|---|---|
| `INSERT INTO ...` | `insert` |
| `INSERT OR REPLACE INTO ...` | `insertOrReplace` |
| `INSERT OR REPLACE` с partial-обновлением (`COALESCE` / подзапрос) | `insertOrReplace<Suffix>` (e.g. `insertOrReplaceMeta`) |
| `SELECT ... WHERE id = ?` | `selectById` |
| `SELECT ... WHERE <col> = ?` | `selectBy<Col>` (e.g. `selectByKey`, `selectByConversationId`) |
| `SELECT ... WHERE a = ? AND b = ?` | `selectBy<A>And<B>` (e.g. `selectByConversationAndId`) |
| `SELECT * FROM ... LIMIT 1` (singleton-таблица) | `select` |
| `SELECT <list> FROM ...` (все строки) | `selectAll` или `selectAll<Detail>` (e.g. `selectAllWithPeer`) |
| `SELECT <col> FROM ...` (отдельная колонка для всех строк) | `selectAll<Col>s` (e.g. `selectAllIds`) |
| `UPDATE ... SET <col> = ?` | `update<Col>` (e.g. `updateUnreadCount`) |
| `DELETE ... WHERE id = ?` | `deleteById` |
| `DELETE ... WHERE <col> = ?` | `deleteBy<Col>` (e.g. `deleteByKey`, `deleteByConversation`) |
| `DELETE FROM table` (вся таблица) | `deleteAll` |

**Ключевые правила:**

1. `DELETE` всегда с суффиксом — `deleteById` / `deleteBy<X>` / `deleteAll`. Голый `delete` не используем.
2. Никаких доменных синонимов (`save`, `get`, `upsert`, `insertOne`, `markAsRead`).
3. `INSERT OR REPLACE` → `insertOrReplace`, не `upsert`. В SQLite это и есть `INSERT OR REPLACE`.
4. Partial-обновление через `COALESCE` маркируется суффиксом (`insertOrReplaceMeta`).
5. Переименование запроса обязательно сопровождается обновлением всех call-sites в Kotlin.

## Примеры

INSERT OR REPLACE:

```sql
insertOrReplace:
INSERT OR REPLACE
INTO ConversationParticipant(conversationId, id, displayName, photoUrl)
VALUES (?, ?, ?, ?);
```

INSERT OR REPLACE с partial (через `COALESCE`):

```sql
insertOrReplaceMeta:
INSERT OR REPLACE
INTO ChatConversation(id, type, participantUids, lastCommit, lastCommitTimestamp, unreadCount)
VALUES (
  :id,
  :type,
  :participantUids,
  :lastCommit,
  :lastCommitTimestamp,
  COALESCE((SELECT unreadCount FROM ChatConversation WHERE id = :id), 0)
);
```

UPDATE одной колонки:

```sql
updateUnreadCount:
UPDATE ChatConversation
SET unreadCount = :unreadCount
WHERE id = :id;
```

SELECT с WHERE:

```sql
selectByConversationAndId:
SELECT id, displayName, photoUrl
FROM ConversationParticipant
WHERE conversationId = :conversationId AND id = :id;
```

DELETE:

```sql
deleteByConversation:
DELETE FROM ConversationParticipant
WHERE conversationId = ?;

deleteAll:
DELETE FROM ConversationParticipant;
```

## Антипаттерны

| Плохо | Хорошо | Почему |
|---|---|---|
| `upsert:` | `insertOrReplace:` | синоним, не SQL |
| `save:` | `insertOrReplace:` | доменное имя, не операция |
| `get:` | `selectById:` / `selectByKey:` | `get` ничего не говорит про WHERE |
| `insertOne:` | `insert:` | избыточный суффикс |
| `delete:` (без суффикса) | `deleteById:` / `deleteAll:` | согласованность, явность |
| `markAsRead:` | `updateUnreadCount:` | доменный интент → в Repository |

## Где живёт доменное имя

Доменные глаголы остаются в `Repository`/`Model`. Они зовут SQL-запросы под
капотом:

```kotlin
// :data
override suspend fun markAsRead(id: Conversation.Id) {
  inMemoryDB.chatConversationQueries.updateUnreadCount(
    id = id.value,
    unreadCount = 0
  )
}
```

Так SQL остаётся техническим, а доменная семантика живёт в нужном слое.

## Связанные документы

- [`architecture-layers-rules.md`](architecture-layers-rules.md) — слои и доступ к Repository.
- [`firestore-naming-rules.md`](firestore-naming-rules.md) — REST-нейминг для `Firestore.kt`.
- [`docs-template.md`](template/docs-template.md) — скелет, по которому пишутся документы в `docs/`.
