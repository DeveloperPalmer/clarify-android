---
tags:
  - method
---
# getCurrentUser

**Summary**: Возвращает документ текущего авторизованного пользователя.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Читает UID из `AuthSessionPersistence`, затем выполняет одиночный `get` документа `users/{uid}`. Выбрасывает исключение, если пользователь не найден — ситуация считается нелегальной (метод вызывается только в авторизованном контексте). |


### Signature

```kotlin
suspend fun getCurrentUser(): UserNM
```

### Parameters

Параметров нет. UID текущего пользователя берётся из `AuthSessionPersistence`.

### Response parameters

| Parameter   | Req | Type      | Description                                  |
|-------------|-----|-----------|----------------------------------------------|
| id          | Y   | String    | UID пользователя (ID документа в Firestore). |
| displayName | Y   | String    | Отображаемое имя.                            |
| email       | Y   | String    | Email в lowercase.                           |
| photoUrl    | N   | String?   | URL аватарки, `null` если не задана.         |
| createdAt   | Y   | Timestamp | Серверный штамп создания аккаунта.           |
| updatedAt   | Y   | Timestamp | Серверный штамп последнего обновления.       |

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

| Exception             | Condition                                                                         |
|-----------------------|-----------------------------------------------------------------------------------|
| IllegalStateException | UID не найден в `AuthSessionPersistence` — пользователь не авторизован.           |
| IllegalStateException | Документ `users/{uid}` не существует в Firestore — аккаунт не создан или удалён. |
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.                          |
