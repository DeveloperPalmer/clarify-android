---
tags:
  - method
---
# readMember

**Summary**: Возвращает данные участника беседы по его ID, или `null` если участника нет.
**Sources**: `lib/google/firestore/src/main/kotlin/ru/sla/clarify/lib/google/firestore/Firestore.kt`
**Last updated**: 2026-09-05

---

| Analyst          | Claude     |
|------------------|------------|
| Publication date | 2026-09-05 |
| Description      | Выполняет одиночный `get` документа `conversations/{conversationId}/members/{memberId}`. Разовое чтение того же документа, за которым следит `memberLive`: подписка нужна экрану, разовое чтение — записи. Основной потребитель — удаление сообщений «у всех»: перед записью нужен `lastReadAt` собеседника, чтобы посчитать, на сколько уменьшить его счётчик непрочитанных. Отсутствие документа ошибкой не является — возвращается `null`, и поправка к счётчику не применяется. |

### Signature

```kotlin
suspend fun readMember(
  conversationId: String,
  memberId: String
): MemberNM?
```

### Parameters

| Parameter      | Req | Type   | Description                                    |
|----------------|-----|--------|------------------------------------------------|
| conversationId | Y   | String | ID беседы, участник которой запрашивается.     |
| memberId       | Y   | String | UID участника, данные которого нужно получить. |

### Response parameters

| Parameter  | Req | Type       | Description                                                 |
|------------|-----|------------|-------------------------------------------------------------|
| id         | Y   | String     | UID участника (дублирует ID документа).                     |
| lastReadAt | N   | Timestamp? | Время последнего прочитанного сообщения. `null` — не читал. |

### Request:

```json
{
  "conversationId": "conv-xyz789",
  "memberId": "uid-bob"
}
```

### Response:

```json
{
  "id": "uid-bob",
  "lastReadAt": "2026-06-13T14:35:00Z"
}
```

### Errors List:

| Exception                  | Condition                                           |
|----------------------------|-----------------------------------------------------|
| FirebaseFirestoreException | Ошибка сети или нарушение Firestore Security Rules. |
