---
tags:
  - method
---
# patchGroupName

**Summary**: Обновляет название групповой беседы.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-13

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Записывает новое значение поля `name` в документ `conversations/{conversationId}` через `set(merge)`. Остальные поля conversation не затрагиваются. |


### Signature

```kotlin
suspend fun patchGroupName(conversationId: String, name: String)
```

### Parameters

| Parameter      | Req | Type   | Description                                  |
|----------------|-----|--------|----------------------------------------------|
| conversationId | Y   | String | ID группового conversation.                  |
| name           | Y   | String | Новое название группы.                       |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что поле `name` обновлено.

### Request:

```json
{
  "conversationId": "conv-group001",
  "name": "Design Team"
}
```

### Response:

Поле `name` в документе `conversations/conv-group001` после обновления:

```json
{
  "name": "Design Team"
}
```

### Errors List:

| Exception                  | Condition                                             |
|----------------------------|-------------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.   |
