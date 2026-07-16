---
tags:
  - root
---
# Operations Log

Append-only журнал всех операций над wiki.

---

## 2026-07-16 — Контекстное меню сообщения в direct-треде

Задокументировано контекстное меню сообщения в direct-треде (код — ветка `dev`).

- `feature/chat-direct-thread`: добавлен раздел «Контекстное меню сообщения» — нажатие на сообщение вне режима выбора открывает приякоренное меню (создать ветку / копировать / выбрать / удалить); копирование текста со snackbar «Скопировано»; пункт «удалить» ведёт в общий диалог удаления одного сообщения. Обновлены Summary и Version agenda.
- Раздел «Ветки» реконсилен: вход в создание ветки восстановлен через пункт «создать ветку» контекстного меню (п. 3 «Запуск создания» переписан обратно с «точки входа нет» на действие из меню).
- Раздел «Выбор и удаление»: у п. 5 «Диалог удаления» появилась вторая точка входа — пункт «удалить» контекстного меню; «число выбранных сообщений» уточнено до «числа удаляемых».

## 2026-07-05 — Удаление сообщений в direct-треде

Задокументирована фича удаления сообщений в direct-треде (код — ветка `chat-direct-thread-delete`).

- `feature/chat-direct-thread`: добавлен раздел «Выбор и удаление» (вход в режим по долгому нажатию, переключение выбора, шапка выбора, диалог с переключателем «удалить у собеседника», удаление «у всех» через `delete-direct-commits` и «у себя» через `hide-commits`, ошибка). В «Данные и методы» добавлены пп. 13–14 (`hide-commits`, `delete-direct-commits`). Обновлены Summary/Description и Version agenda.
- Реконсилирован конфликт в разделе «Ветки»: долгое нажатие раньше запускало создание ветки, теперь оно занято выбором сообщений. П. 3 «Запуск создания» переписан — в direct-UI сейчас нет точки входа для создания ветки; контракт валидации и метод `create-branch` сохранены как применяемые при вызове формы.
- Создана method-страница `method/commit/delete-direct-commits.md` (транзакция: физическое удаление документов + пересчёт `lastCommit*` беседы по `LastCommitParams` keep/replace/clear + уменьшение счётчика непрочитанных собеседника на `peerUnreadDelta`). Страница `hide-commits` уже существовала — использована как образец.
- `index.md`: добавлена запись `delete-direct-commits` в раздел Commit.

## 2026-06-19 — `get*` → `read*` (чистый CRUD)

Все read-методы доступа к данным в Firestore.kt переименованы под CRUD-стиль `read*`:

- `getCurrentUser` → `readCurrentUser`, `getUser` → `readUser`, `getUserExistsByEmail` → `readUserExistsByEmail`, `getUserIdByEmail` → `readUserIdByEmail`, `getUsersByEmailPrefix` → `readUsersByEmailPrefix`, `getCommits` → `readCommits`.
- `gitUserExists` → `readUserExists` — попутно устранена опечатка `git`/`get`, на которую указывала запись от 19.06 ниже. Теперь имя в коде и в вики совпадают.

SDK-вызовы `.get()`/`.getLong()` Firestore не затронуты — это API Google, а не методы доступа приложения. Обновлены вызовы в `ConversationRepositoryImpl`, `GroupThreadRepositoryImpl`, `BranchRepositoryImpl`, `DirectThreadRepositoryImpl`, `DebugPanelRepositoryImpl`, `LoginRepositoryImpl`; имена методов репозиториев сохранены. Вики: 7 method-страниц переименованы в `read-*`, заголовки/сигнатуры/прозо-ссылки, `index.md` и ссылки в feature-файлах приведены в соответствие.

## 2026-06-19 — Согласование имён method-страниц с коммитом bc9673af

Write-слой Firestore.kt переименован (Post*/Patch* → Create*/Update*, observe* → *Live). Method-страницы приведены в соответствие с актуальными именами:

- Переименованы файлы и заголовки 29 method-страниц + обновлены сигнатуры и кросс-ссылки в описаниях.
- `observe-*` → `*Live`: `branchesLive`, `commitsLive`, `directCommitsLive`, `groupCommitsLive`, `conversationsLive`, `memberLive`, `membersLive`, `unreadCountLive`, `branchUnreadCountLive`, `userLive`.
- `post*`/`patch*` → `create*`/`update*`: `createBranch`, `createDirectCommit`, `createGroupCommit`, `createBranchCommit`, `createGroupConversation`, `updateConversationName`, `createUser`, `updateUser`, `updateReadWatermark`, `inviteMember`, `clearUnreadCount`, `clearBranchUnreadCount`.
- Merge request: `createOpenMergeRequest`, `updateMergeApproval`, `updateMergeFinalize`, `deleteMergeRequestApproval`; `deleteMergeRequest` имя сохранил.
- `deleteGroupConversation` → `deleteConversation`, `deleteMember` → `deleteConversationMember`, `leaveGroup` → `leaveConversation`.
- Заголовки `# observe-*` и `# get-user-exists*` нормализованы из kebab-case в camelCase под общий стиль страниц.
- Ссылка на param-класс `PatchUserParams` → `UpdateUserParams` в `update-user`.
- `index.md` обновлён: имена, пути, дата.

Сигнатуры параметров правок не требовали — они уже соответствовали актуальному коду.

## 2026-06-19 — `clearUnreadCount`/`clearBranchUnreadCount` → `update*`

В Firestore.kt write-методы переименованы под CRUD-стиль: `clearUnreadCount` → `updateUnreadCount`, `clearBranchUnreadCount` → `updateBranchUnreadCount` (param-класс уже был `UpdateUnreadCountParams`). Семантика «clear» осталась на уровне репозитория (`markAsRead`/`markReadUpTo`). Обновлены вызовы в `GroupThreadRepositoryImpl`, `BranchRepositoryImpl`, `DirectThreadRepositoryImpl`. Method-страницы переименованы (`update-unread-count`, `update-branch-unread-count`), заголовки/сигнатуры и `index.md` приведены в соответствие.

## 2026-06-19 — CRUD-имена для `inviteMember`/`leaveConversation`

Доведены до CRUD-стиля оставшиеся write-методы Firestore.kt:

- `inviteMember` → `createCommitInviteMember` (метод одним batch'ом создаёт системный commit-приглашение + добавляет участника; имя совпадает с param-классом `CreateCommitInviteMemberParams`).
- `leaveConversation` → перегрузка `deleteConversationMember(conversationId)`, делегирующая в `deleteConversationMember(conversationId, userId)` с текущим UID.

Доменные методы репозитория (`inviteGroupMembers`, `leaveConversation`) имена сохранили — «leave» как смысл остаётся на уровне репозитория. Обновлены вызовы в `GroupThreadRepositoryImpl`. Значение поля данных commit'а `type: "inviteMember"` (`CommitNM.Type.InviteMember`) не менялось — это не имя метода.

Вики: `invite-member` → `create-commit-invite-member`. Страница `leave-conversation` стала `delete-conversation-member-self` (заголовок `# deleteConversationMember`, сигнатура с одним аргументом) — две перегрузки оставлены отдельными страницами с одинаковым именем метода. Обновлены `index.md` и ссылки в feature-файлах.

Расхождение: в Firestore.kt метод записан как `gitUserExists` (опечатка, ожидалось `getUserExists`). В wiki сохранено корректное имя `getUserExists`; опечатку нужно исправить в коде, а не в требованиях.

## 2026-07-05 — `deletedFor` → `visibleFor`: серверная фильтрация скрытых сообщений

Поле commit-документа `deletedFor` (кто скрыл) заменено на инвертированное `visibleFor` (кто видит): Firestore не поддерживает «array-not-contains», а `whereArrayContains(visibleFor, uid)` позволяет фильтровать скрытые сообщения на сервере. Скрытые сообщения больше не занимают окно live-подписки и страницы пагинации; когда сообщение скрыли все участники, документ физически удаляется (GC «последним скрывшим»).

- `create-direct-commit`, `create-branch-commit`, `create-group-commit`: commit записывается с `visibleFor` = участники беседы на момент отправки.
- `read-commits`, `commits-live`: новый параметр `onlyVisible`; в `read-commits` параметр `count: Int` актуализирован до `limit: Long` по коду.
- `direct-commits-live`: фильтр видимости включён всегда; `group-commits-live`: фильтр не применяется (в группах скрытие не поддерживается), но `visibleFor` в документы пишется.
- Новая страница `hide-commits`: транзакционное скрытие через `arrayRemove` с физическим удалением опустевших. Добавлена в `index.md`.

Запросы с фильтром требуют композитный индекс `commits`: `visibleFor` (array-contains) + `branchId` (asc) + `createdAt` (desc). Индекс и security rules ведутся вне репозитория.

## 2026-07-05 — Единый composite-индекс: `visibleFor`-фильтр во всех тредах

Запрос сообщений унифицирован — фильтр по видимости (`whereArrayContains(visibleFor, uid)`) теперь применяется всегда, флаг `onlyVisible` из `readCommits`/`commitsLive` убран. Раньше группы ходили без фильтра, из-за чего требовался отдельный composite-индекс `commits: branchId + createdAt`. Теперь все треды (direct, ветки, группы) используют одну форму запроса и один индекс `commits: visibleFor (array-contains) + branchId + createdAt`; старый индекс без `visibleFor` можно удалить.

- `read-commits`, `commits-live`: убран параметр `onlyVisible`, описания — «фильтр применяется всегда».
- `group-commits-live`: теперь фильтруется по `visibleFor`; участник видит сообщения с момента вступления. Действия «скрыть» в группах пока нет — `visibleFor` не убавляется, фильтр на текущем поведении ничего не отсекает (задел на будущее).
- `create-commit-invite-member`: добавлен параметр `memberUids` → поле `visibleFor` системного коммита (состав группы с учётом приглашённых), иначе системное сообщение не прошло бы фильтр.
