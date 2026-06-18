---
tags:
  - method
---
# readUserIdByEmail

**Summary**: Находит UID пользователя по его email-адресу.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-19

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Выполняет запрос к коллекции `users` с фильтром по полю `email` (lowercase), берёт первый результат и возвращает ID документа как `UserId`. Возвращает `null` если пользователь не найден. Используется для поиска собеседника при инициации direct-чата. |


### Signature

```kotlin
suspend fun readUserIdByEmail(email: Email): UserId?
```

### Parameters

| Parameter | Req | Type  | Description                                                                       |
|-----------|-----|-------|-----------------------------------------------------------------------------------|
| email     | Y   | Email | Email для поиска. Нормализуется в lowercase перед запросом. |

### Response parameters

| Значение  | Тип     | Description                                    |
|-----------|---------|------------------------------------------------|
| `UserId`  | UserId? | UID найденного пользователя.                   |
| `null`    | UserId? | Пользователь с таким email не зарегистрирован. |

### Request:

```json
{
  "arg0": "john@example.com"
}
```

### Response:

```json
"uid-abc123"
```

### Errors List:

| Exception                  | Condition                                           |
|----------------------------|-----------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules. |
