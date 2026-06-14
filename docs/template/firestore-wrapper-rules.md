# FirestoreWrapper — соглашение о доступе к Firestore

`lib/google/firestore/FirestoreWrapper.kt` — единственное место, где
строятся references и queries Firestore. `Firestore.kt` использует **только**
методы wrapper'а; обращаться к `remoteDB` напрямую за `collection(...)` /
`document(...)` нельзя. Цель — один взгляд = понятно, куда идёт запрос.

## Правила

| # | Правило | Уточнение |
|---|---|---|
| 1 | Каждый метод wrapper'а выписывает полный путь от `remoteDB` | не переиспользует другие методы того же wrapper'а |
| 2 | Любой `collection(...)` / `document(...)` / `whereXxx(...)` живёт в wrapper'е | в `Firestore.kt` строить пути вручную **запрещено** |
| 3 | Имена методов — `xDocumentRef` / `xCollectionRef` / `xQuery` | по виду reference, который возвращают |
| 4 | Имена параметров query-методов — **firestore-операция**, не семантика | `whereEqualTo`, `whereArrayContains`; не `whereEqualToEmail` |
| 5 | Семантика значения видна на стороне caller'а | `usersQuery(whereEqualTo = email.lowercase())` — `email` читается на месте вызова |
| 6 | Если нужна новая query — сначала builder в wrapper, потом вызов | не «временно напрямую через `remoteDB`» |

## Примеры

Document reference — полный путь от `remoteDB`:

```kotlin
override fun branchDocumentRef(
  conversationId: FirestoreConversation.Id,
  branchId: FirestoreBranch.Id
): DocumentReference {
  return remoteDB
    .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
    .document(conversationId.value)
    .collection(FirestoreSchema.BRANCHES_COLLECTION)
    .document(branchId.value)
}
```

Query c фильтром — имя параметра по firestore-операции:

```kotlin
override fun usersQuery(whereEqualTo: String): Query {
  return remoteDB
    .collection(FirestoreSchema.USERS_COLLECTION)
    .whereEqualTo(FirestoreSchema.USER_EMAIL, whereEqualTo)
}
```

Caller в `Firestore.kt` — берёт builder из wrapper'а, контекст про значение
читается прямо на вызове:

```kotlin
suspend fun getPeerIdByEmail(email: String): Peer.Id? {
  val snapshot = usersQuery(whereEqualTo = email.lowercase())
    .limit(1)
    .get()
    .await()
  val uid = snapshot.documents.firstOrNull()?.id ?: return null
  return Peer.Id(uid)
}
```

Query с несколькими фильтрами — параметры всё равно по операции, не по
значению:

```kotlin
override fun conversationsQuery(
  whereEqualTo: ConversationType,
  whereArrayContains: UserId
): Query {
  return remoteDB
    .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
    .whereEqualTo(FirestoreSchema.CONVERSATION_TYPE, whereEqualTo.value)
    .whereArrayContains(FirestoreSchema.CONVERSATION_MEMBER_UIDS, whereArrayContains.value)
}
```

## Антипаттерны

| Плохо | Хорошо | Почему |
|---|---|---|
| `branchDocumentRef` вызывает `conversationDocumentRef(...).collection(...).document(...)` | каждый метод заново пишет путь от `remoteDB` | при дебаге нужно одним взглядом видеть полный путь, без скачков по файлу |
| В `Firestore.kt`: `remoteDB.collection(USERS).whereEqualTo(EMAIL, x).get()` | сначала `usersQuery(whereEqualTo = ...)` в wrapper'е, потом вызов | бизнес-код в `Firestore.kt` не должен знать про структуру коллекций |
| `usersQuery(whereEqualToEmail: String)` | `usersQuery(whereEqualTo: String)` | имя параметра отражает firestore-операцию; семантика читается на месте вызова |
| `conversationsQuery(type: ConversationType, member: UserId)` | `conversationsQuery(whereEqualTo: ConversationType, whereArrayContains: UserId)` | по имени параметра должно быть видно, какой это фильтр |
| `usersByEmailQuery(email)` | `usersQuery(whereEqualTo = email)` | имя метода — про коллекцию + вид reference, фильтр — параметром |

## Где живёт `remoteDB`

`remoteDB` — `private val` внутри `FirestoreWrapper`. В `FirestoreWrapperProvider`
поля нет, наружу не торчит. Все SDK-операции, которые раньше выписывались как
`remoteDB.batch()` / `remoteDB.runTransaction { ... }`, доступны через
wrapper-методы:

```kotlin
// FirestoreWrapperProvider
fun writeBatch(): WriteBatch
fun <T> runTransaction(block: Transaction.Function<T>): Task<T>
```

Сигнатуры зеркалят Firestore SDK, поэтому call-sites выглядят как раньше:

```kotlin
// В Firestore.kt
suspend fun deleteConversations(ids: List<String>) {
  val batch = writeBatch()                    // вместо remoteDB.batch()
  val reference = conversationCollectionRef()
  ids.forEach { id -> batch.delete(reference.document(id)) }
  batch.commit().await()
}

suspend fun patchMergeApproval(...) {
  val transaction = runTransaction { txn ->   // вместо remoteDB.runTransaction
    val snapshot = txn.get(branchRef)
    /* ... */
  }
  transaction.await()
}
```

Если понадобится новая SDK-операция, которой в wrapper'е нет — сначала
добавляется метод-обёртка в `FirestoreWrapperProvider` + `FirestoreWrapper`,
потом используется в `Firestore.kt`. Прямого доступа к `remoteDB` снаружи
wrapper'а быть не должно.

## Связанные документы

- [`firestore-naming-rules.md`](firestore-naming-rules.md) — конвенция имён методов в `Firestore.kt` (GET/POST/PATCH/DELETE/Live).
- [`architecture-layers-rules.md`](architecture-layers-rules.md) — где живёт `Firestore` относительно других слоёв.
