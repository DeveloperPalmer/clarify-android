# Firestore.kt — соглашение об именовании

`lib/google/firestore/Firestore.kt` — это **тонкий гейтвей** к Firestore. Он
ничего не знает про бизнес-интент. Имена функций описывают **сетевую операцию**,
а не «зачем её зовут». Перевод доменного действия (`approveMerge`,
`markAsRead`, `cancelX`) в техническую операцию — обязанность репозитория, не
Firestore.

## Правила

| Операция | Префикс/суффикс | Пример |
|---|---|---|
| GET (one-shot чтение) | `getX` | `getParticipants`, `getCommits` |
| POST (создание) | `postX` | `postCommit`, `postBranch`, `postMergeRequest` |
| PATCH (частичное обновление, `set + merge`) | `patchX` | `patchUser`, `patchUnreadCount`, `patchMergeApproval` |
| DELETE | `deleteX` | `deleteConversations`, `deleteMergeRequest`, `deleteMergeApproval` |
| Stream (snapshot listener / callbackFlow) | суффикс `Live` | `conversationsLive`, `unreadCountLive`, `branchesLive` |

Стримы используют суффикс `Live` по аналогии с `LiveData`. Не `observe*`, не
`stream*`, не `live*` префиксом.

## Шпаргалка «как выбрать»

1. **Возвращает `Flow<T>` через snapshot listener?** → суффикс `Live`.
2. **`suspend`, читает данные, возвращает их вызвавшему?** → `getX`.
3. **`suspend`, создаёт документ?** → `postX`.
4. **`suspend`, частично обновляет (через `set + merge` или `update`)?** →
   `patchX`. Под это попадает и «сбросить счётчик», и «дописать в массив»,
   и `runTransaction { ... update(...) }`.
5. **`suspend`, удаляет?** → `deleteX`.

## Примеры

### Стрим
```kotlin
fun conversationsLive(): Flow<List<FirestoreConversation>> = callbackFlow {
  val userId = requireUserId()
  listenerGuard.trackOpen("conversationsLive:${userId.value}")
  val listener = conversationsQuery(whereArrayContains = userId)
    .addSnapshotListener { snapshot, error -> /* ... */ }
  awaitClose { listener.remove() }
}
```

### One-shot чтение
```kotlin
suspend fun getParticipants(
  conversationId: FirestoreConversation.Id
): List<FirestoreParticipant> {
  return participantsCollectionRef(conversationId)
    .get()
    .await()
    .documents
    .map(::extractParticipantFB)
}
```

### Создание
```kotlin
suspend fun postBranch(
  conversationId: FirestoreConversation.Id,
  parentBranchId: FirestoreBranch.Id,
  branchedFromCommitId: FirestoreCommit.Id,
  name: String
): FirestoreBranch { /* ... */ }
```

### Частичное обновление
```kotlin
suspend fun patchUnreadCount(conversationId: FirestoreConversation.Id) {
  val userId = requireUserId()
  unreadCommitsDocumentRef(conversationId, userId)
    .set(mapOf(FirestoreSchema.UNREAD_COMMITS_COUNT to 0L), SetOptions.merge())
    .await()
}
```

### Удаление
```kotlin
suspend fun deleteConversations(ids: List<String>) {
  val batch = remoteDB.batch()
  val reference = conversationCollectionRef()
  ids.forEach { id -> batch.delete(reference.document(id)) }
  batch.commit().await()
}
```

## Антипаттерны

| Неправильно | Правильно | Почему |
|---|---|---|
| `mergeUser` | `patchUser` | «merge» — деталь реализации SDK, не операция |
| `markConversationAsRead` | `patchUnreadCount` | «mark as read» — бизнес-интент; в Firestore это просто PATCH счётчика |
| `requestMerge` / `approveMerge` / `cancelMergeRequest` | `postMergeRequest` / `patchMergeApproval` / `deleteMergeRequest` | доменные глаголы протекают из репозитория |
| `sendCommit` | `postCommit` | «send» — про транспорт, REST это POST |
| `createBranch` | `postBranch` | префиксы CRUD не используем — только REST-глаголы |
| `observeConversations` / `streamConversations` / `liveConversations` | `conversationsLive` | стримы — суффиксом `Live` |
| `loadParticipants` / `fetchParticipants` | `getParticipants` | в Firestore-слое чтение — это GET. `fetch*` зарезервирован за репозиторием (пишет в кэш, возвращает `Unit`) |
| `historyCommits` | `getCommits` | подмножество (history) задаётся параметрами, не именем |

## Где живёт доменный интент

Доменные глаголы вида `markAsRead`, `approveMerge`, `cancelMergeRequest`,
`requestMerge` остаются в `Repository` (например, `BranchRepository`,
`ConversationRepository`, `ThreadRepository`). Репозиторий принимает доменное
название и зовёт под капотом нужный Firestore-метод:

```kotlin
// BranchRepository (домен)
suspend fun approveMerge(branchId: Branch.Id)

// BranchRepositoryImpl
override suspend fun approveMerge(branchId: Branch.Id) = withContext(Dispatchers.IO) {
  firestore.patchMergeApproval(
    conversationId = threadMediator.requireConversationId(),
    branchId = FirestoreBranch.Id(branchId.value),
    participantUids = threadMediator.directParticipantUids()
  )
}
```

## Связанные правила

- `fetch*` функции возвращают `Unit` и пишут в локальный кэш — наблюдение
  идёт через отдельный `Flow`. Поэтому в Firestore-слое **никаких** `fetch*` —
  у нас Firestore не владеет кэшем. Используй `getX` для one-shot чтения.
- `Firestore.kt` использует `FirestoreWrapper`-методы для построения путей
  (`userDocumentRef`, `conversationDocumentRef`, ...). Не строй пути вручную.
