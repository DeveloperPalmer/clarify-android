---
tags:
  - method
---
# postUser

**Summary**: Создаёт документ пользователя в Firestore при первом входе.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Первичная вставка документа `users/{uid}`. Записывает payload-поля и серверные штампы `createdAt`/`updatedAt`. Использует `set` без `merge` — если документ уже существует, он будет перезаписан целиком. Вызывать только после проверки через `getUserExists`. |


### Signature

```kotlin
suspend fun postUser(
  id: UserId,
  email: String,
  displayName: String,
  photoUrl: String?
)
```

### Parameters

| Parameter   | Req | Type    | Description                                                       |
|-------------|-----|---------|-------------------------------------------------------------------|
| id          | Y   | UserId  | UID пользователя из Firebase Auth. Становится именем документа.   |
| email       | Y   | String  | Email — нормализуется в lowercase внутри кодека перед записью.    |
| displayName | Y   | String  | Отображаемое имя пользователя.                                    |
| photoUrl    | N   | String? | URL аватарки. `null` — поле не записывается в документ.           |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что документ записан в Firestore.

### Request:

```json
{
  "id": "uid-abc123",
  "email": "john@example.com",
  "displayName": "John Doe",
  "photoUrl": "https://example.com/photo.jpg"
}
```

### Response:

Документ `users/uid-abc123` в Firestore после записи:

```json
{
  "displayName": "John Doe",
  "email": "john@example.com",
  "photoUrl": "https://example.com/photo.jpg",
  "createdAt": "2026-06-13T10:00:00Z",
  "updatedAt": "2026-06-13T10:00:00Z"
}
```

### Errors List:

| Exception                  | Condition                                                |
|----------------------------|----------------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.      |
