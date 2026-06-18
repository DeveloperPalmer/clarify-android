---
tags:
  - method
---
# deleteConversations

**Summary**: Пакетно удаляет несколько корневых документов conversations.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-06-19

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-06-13 |
| Description      | Формирует один Firestore batch и удаляет в нём все перечисленные документы из коллекции `conversations`. Используется для массовой очистки (например, при тестировании или сбросе данных). Как и `deleteConversation`, удаляет только корневые документы — подколлекции остаются. |


### Signature

```kotlin
suspend fun deleteConversations(ids: List<String>)
```

### Parameters

| Parameter | Req | Type          | Description                                      |
|-----------|-----|---------------|--------------------------------------------------|
| ids       | Y   | List\<String\> | Список ID conversations для удаления. Может быть пустым — в этом случае batch пустой и ничего не происходит. |

### Response parameters

Метод возвращает `Unit`. Успешное завершение означает, что все перечисленные документы удалены атомарно.

### Request:

```json
{
  "ids": ["conv-xyz789", "conv-group001", "conv-old123"]
}
```

### Response:

```
conversations/conv-xyz789    ← УДАЛЁН
conversations/conv-group001  ← УДАЛЁН
conversations/conv-old123    ← УДАЛЁН
```

### Errors List:

| Exception                  | Condition                                              |
|----------------------------|--------------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules.    |
