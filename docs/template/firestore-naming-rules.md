# Firestore.kt — соглашение об именовании

`lib/google/firestore/Firestore.kt` — тонкий гейтвей к Firestore. Имена методов
описывают **сетевую операцию** (REST-глагол), а не доменный интент. Перевод
доменного действия в техническое — обязанность Repository.

## Правила

| Операция | Имя | Пример |
|---|---|---|
| GET (one-shot чтение) | `getX` | `getMembers`, `getCommits` |
| POST (создание) | `postX` | `postCommit`, `postBranch`, `postMergeRequest` |
| PATCH (частичное обновление, `set + merge`, `update`) | `patchX` | `patchUser`, `patchUnreadCount`, `patchMergeApproval` |
| DELETE | `deleteX` | `deleteConversations`, `deleteMergeRequest` |
| Stream (snapshot listener / callbackFlow) | суффикс `Live` | `conversationsLive`, `unreadCountLive`, `branchesLive` |

**Как выбрать:**

1. Возвращает `Flow<T>` через snapshot listener? → суффикс `Live`.
2. `suspend`, читает и возвращает данные? → `getX`.
3. `suspend`, создаёт документ? → `postX`.
4. `suspend`, частично обновляет (через `set + merge` / `update` / транзакция с `update`)? → `patchX`.
5. `suspend`, удаляет? → `deleteX`.

## Примеры

Стрим:

```kotlin
fun conversationsLive(): Flow<List<FirestoreConversation>> = callbackFlow {
  val userId = requireUserId()
  listenerGuard.trackOpen("conversationsLive:${userId.value}")
  val listener = conversationsQuery(whereArrayContains = userId)
    .addSnapshotListener { snapshot, error -> /* ... */ }
  awaitClose { listener.remove() }
}
```

One-shot чтение:

```kotlin
suspend fun getMembers(
  conversationId: FirestoreConversation.Id
): List<FirestoreMember> {
  return membersCollectionRef(conversationId)
    .get()
    .await()
    .documents
    .map(::extractMemberFB)
}
```

Создание:

```kotlin
suspend fun postBranch(
  conversationId: FirestoreConversation.Id,
  parentBranchId: FirestoreBranch.Id,
  branchedFromCommitId: FirestoreCommit.Id,
  name: String
): FirestoreBranch { /* ... */ }
```

Частичное обновление:

```kotlin
suspend fun patchUnreadCount(conversationId: FirestoreConversation.Id) {
  val userId = requireUserId()
  unreadCommitsDocumentRef(conversationId, userId)
    .set(buildMap { put(FirestoreSchema.UNREAD_COMMITS_COUNT, 0L) }, SetOptions.merge())
    .await()
}
```

Удаление:

```kotlin
suspend fun deleteConversations(ids: List<String>) {
  val batch = writeBatch()
  val reference = conversationCollectionRef()
  ids.forEach { id -> batch.delete(reference.document(id)) }
  batch.commit().await()
}
```

## Антипаттерны

| Плохо | Хорошо | Почему |
|---|---|---|
| `mergeUser` | `patchUser` | `merge` — деталь SDK, не операция |
| `markConversationAsRead` | `patchUnreadCount` | бизнес-интент; в Firestore это PATCH счётчика |
| `requestMerge` / `approveMerge` / `cancelMergeRequest` | `postMergeRequest` / `patchMergeApproval` / `deleteMergeRequest` | доменные глаголы протекают из Repository |
| `sendCommit` | `postCommit` | `send` — про транспорт, REST это POST |
| `createBranch` | `postBranch` | CRUD-префиксы не используем — только REST |
| `observeConversations` / `streamConversations` / `liveConversations` | `conversationsLive` | стримы — суффиксом `Live` |
| `loadMembers` / `fetchMembers` | `getMembers` | в Firestore-слое чтение — это GET. `fetch*` зарезервирован за Repository |
| `historyCommits` | `getCommits` | подмножество задаётся параметрами, не именем |

## Где живёт доменный интент

Доменные глаголы (`markAsRead`, `approveMerge`, `cancelMergeRequest`, `requestMerge`)
остаются в `Repository`. Repository принимает доменное имя и зовёт нужный
Firestore-метод:

```kotlin
// BranchRepository (домен)
suspend fun approveMerge(branchId: Branch.Id)

// BranchRepositoryImpl
override suspend fun approveMerge(branchId: Branch.Id) = withContext(Dispatchers.IO) {
  firestore.patchMergeApproval(
    conversationId = threadMediator.requireConversationId(),
    branchId = FirestoreBranch.Id(branchId.value),
    memberUids = threadMediator.directMemberIds()
  )
}
```

## Связанные документы

- [`firestore-wrapper-rules.md`](firestore-wrapper-rules.md) — как `Firestore.kt` обращается к `FirestoreWrapper` (пути, queries, naming параметров).
- [`architecture-layers-rules.md`](architecture-layers-rules.md) — слои и доступ к Repository.
- [`sqldelight-naming-rules.md`](sqldelight-naming-rules.md) — имя `.sq`-запроса = SQL-операция.
- [`docs-template.md`](docs-template.md) — скелет, по которому пишутся документы в `docs/`.
