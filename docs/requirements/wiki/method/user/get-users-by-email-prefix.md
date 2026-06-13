---
tags:
  - method
---
# getUsersByEmailPrefix

**Summary**: Prefix-поиск пользователей по началу email-адреса.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Выполняет Firestore-запрос `orderBy(email).startAt(prefix).endAt(prefix + "")` для поиска по префиксу. Email в `users` нормализованы в lowercase, поэтому `prefix` тоже должен быть в lowercase. `limit` ограничивает список (по дизайну — 10). Используется в UI поиска собеседников при создании чата. |


### Signature

```kotlin
suspend fun getUsersByEmailPrefix(prefix: String, limit: Long): List<UserNM>
```

### Parameters

| Parameter | Req | Type   | Description                                                                                |
|-----------|-----|--------|--------------------------------------------------------------------------------------------|
| prefix    | Y   | String | Начало email для поиска. Должен быть в **lowercase** — email в Firestore хранятся в нижнем регистре. |
| limit     | Y   | Long   | Максимальное количество результатов. По дизайну — 10.                                      |

### Response parameters

| Parameter   | Req | Type      | Description                                  |
|-------------|-----|-----------|----------------------------------------------|
| id          | Y   | String    | UID пользователя.                            |
| displayName | Y   | String    | Отображаемое имя.                            |
| email       | Y   | String    | Email в lowercase.                           |
| photoUrl    | N   | String?   | URL аватарки.                                |
| createdAt   | Y   | Timestamp | Серверный штамп создания аккаунта.           |
| updatedAt   | Y   | Timestamp | Серверный штамп последнего обновления.       |

### Request:

```json
{
  "prefix": "john",
  "limit": 10
}
```

### Response:

```json
[
  {
    "id": "uid-abc123",
    "displayName": "John Doe",
    "email": "john@example.com",
    "photoUrl": "https://example.com/photo.jpg",
    "createdAt": "2026-01-15T10:00:00Z",
    "updatedAt": "2026-06-13T12:00:00Z"
  },
  {
    "id": "uid-def456",
    "displayName": "Johnny Smith",
    "email": "johnny@example.com",
    "photoUrl": null,
    "createdAt": "2026-03-01T09:00:00Z",
    "updatedAt": "2026-06-10T08:00:00Z"
  }
]
```

### Errors List:

| Exception                  | Condition                                           |
|----------------------------|-----------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules. |
