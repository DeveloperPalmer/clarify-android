---
tags:
  - method
---
# readUserExists

**Summary**: Проверяет, существует ли документ пользователя в Firestore по его ID.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-19

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Выполняет одиночный `get` документа `users/{uid}` и возвращает `exists()`. Используется перед `createUser`, чтобы избежать перезаписи существующего аккаунта. |


### Signature

```kotlin
suspend fun readUserExists(id: UserId): Boolean
```

### Parameters

| Parameter | Req | Type   | Description                            |
|-----------|-----|--------|----------------------------------------|
| id        | Y   | UserId | UID пользователя для проверки наличия. |

### Response parameters

| Значение | Тип     | Description                                    |
|----------|---------|------------------------------------------------|
| `true`   | Boolean | Документ `users/{uid}` существует в Firestore. |
| `false`  | Boolean | Документ не найден.                            |

### Request:

```json
{
  "arg0": "uid-abc123"
}
```

### Response:

```json
true
```

### Errors List:

| Exception                  | Condition                                           |
|----------------------------|-----------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules. |
