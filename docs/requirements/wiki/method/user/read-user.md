---
tags:
  - method
---
# readUser

**Summary**: Возвращает документ пользователя по его ID, или `null` если не найден.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-19

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Выполняет одиночный `get` документа `users/{uid}`. В отличие от `readCurrentUser`, не выбрасывает исключение при отсутствии документа — возвращает `null`. Подходит для загрузки профиля произвольного пользователя (например, для отображения карточки участника беседы). |


### Signature

```kotlin
suspend fun readUser(id: UserId): UserNM?
```

### Parameters

| Parameter | Req | Type   | Description                                               |
|-----------|-----|--------|-----------------------------------------------------------|
| id        | Y   | UserId | UID пользователя, данные которого нужно получить.         |

### Response parameters

| Parameter   | Req | Type      | Description                                  |
|-------------|-----|-----------|----------------------------------------------|
| id          | Y   | String    | UID пользователя (ID документа в Firestore). |
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

| Exception                  | Condition                                           |
|----------------------------|-----------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules. |
