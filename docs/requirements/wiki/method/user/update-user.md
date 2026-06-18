---
tags:
  - method
---
# updateUser

**Summary**: Частично обновляет документ пользователя, не затрагивая поля, которые не переданы.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-19

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Обновляет `users/{uid}` через `set(merge)`. Поля с `null` не попадают в запрос (`@EncodeDefault(NEVER)` в `UpdateUserParams`), поэтому существующее значение остаётся нетронутым. Поле `updatedAt` всегда перезаписывается серверным штампом. Не предназначен для удаления поля (например, очистки аватарки) — для этого нужен отдельный метод с sentinel `Delete`. |


### Signature

```kotlin
suspend fun updateUser(
  id: UserId,
  email: String,
  displayName: String,
  photoUrl: String?
)
```

### Parameters

| Parameter   | Req | Type    | Description                                                                                             |
|-------------|-----|---------|---------------------------------------------------------------------------------------------------------|
| id          | Y   | UserId  | UID пользователя. Определяет, какой документ будет обновлён.                                            |
| email       | Y   | String  | Актуальный email — перезаписывается при каждом вызове.                                                  |
| displayName | Y   | String  | Актуальное отображаемое имя — перезаписывается при каждом вызове.                                       |
| photoUrl    | N   | String? | `null` — поле `photoUrl` не трогается. Непустая строка — перезаписывается. Нельзя передать `""` для удаления. |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что изменения применены через merge.

### Request:

```json
{
  "id": "uid-abc123",
  "email": "john.new@example.com",
  "displayName": "John Smith",
  "photoUrl": null
}
```

### Response:

Документ `users/uid-abc123` в Firestore после merge-обновления:

```json
{
  "displayName": "John Smith",
  "email": "john.new@example.com",
  "photoUrl": "https://example.com/photo.jpg",
  "createdAt": "2026-06-13T10:00:00Z",
  "updatedAt": "2026-06-13T15:30:00Z"
}
```

### Errors List:

| Exception                  | Condition                                             |
|----------------------------|-------------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.   |
