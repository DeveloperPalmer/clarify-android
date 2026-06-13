---
tags:
  - method
---
# get-user-exists-by-email

**Summary**: Проверяет, зарегистрирован ли пользователь с указанным email.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Выполняет запрос к коллекции `users` с фильтром по полю `email` (lowercase), ограничивает выборку 1 документом. Возвращает `true` если хотя бы один документ найден. |


### Signature

```kotlin
suspend fun getUserExistsByEmail(email: Email): Boolean
```

### Parameters

| Parameter | Req | Type  | Description                                                                                     |
|-----------|-----|-------|-------------------------------------------------------------------------------------------------|
| email     | Y   | Email | Email для поиска. Нормализуется в lowercase перед запросом — email в Firestore хранятся в нижнем регистре. |

### Response parameters

| Значение | Тип     | Description                                                    |
|----------|---------|----------------------------------------------------------------|
| `true`   | Boolean | Найден хотя бы один пользователь с таким email.               |
| `false`  | Boolean | Ни одного пользователя с таким email в коллекции `users` нет. |

### Request:

```json
{
  "arg0": "john@example.com"
}
```

### Response:

```json
false
```

### Errors List:

| Exception                  | Condition                                           |
|----------------------------|-----------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules. |
