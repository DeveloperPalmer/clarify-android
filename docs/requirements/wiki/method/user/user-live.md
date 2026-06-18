---
tags:
  - method
---
# userLive

**Summary**: Живая подписка на изменения документа пользователя.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-19

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Открывает Firestore snapshot listener на документ `users/{uid}`. Эмитит новое значение каждый раз, когда документ изменяется на сервере. Если документ не существует — эмитит `null`. Listener автоматически закрывается вместе с Flow через `awaitClose`. Открытие трекается через `FirestoreListenerGuard`. |


### Signature

```kotlin
fun userLive(id: UserId): Flow<UserNM?>
```

### Parameters

| Parameter | Req | Type   | Description                                                    |
|-----------|-----|--------|----------------------------------------------------------------|
| id        | Y   | UserId | UID пользователя, за изменениями которого нужно наблюдать. |

### Response parameters

| Parameter   | Req | Type      | Description                                  |
|-------------|-----|-----------|----------------------------------------------|
| id          | Y   | String    | UID пользователя.                            |
| displayName | Y   | String    | Отображаемое имя.                            |
| email       | Y   | String    | Email в lowercase.                           |
| photoUrl    | N   | String?   | URL аватарки, `null` если не задана.         |
| createdAt   | Y   | Timestamp | Серверный штамп создания аккаунта.           |
| updatedAt   | Y   | Timestamp | Серверный штамп последнего обновления.       |

### Request:

```json
{
  "arg0": "uid-abc123"
}
```

### Response:

Каждая эмиссия Flow:

```json
{
  "id": "uid-abc123",
  "displayName": "John Doe",
  "email": "john@example.com",
  "photoUrl": "https://example.com/photo.jpg",
  "createdAt": "2026-01-15T10:00:00Z",
  "updatedAt": "2026-06-13T12:00:00Z"
}
```

### Errors List:

| Exception                  | Condition                                                        |
|----------------------------|------------------------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети — Flow завершается с ошибкой через `close(error)`.  |
