---
tags:
  - root
---
# Operations Log

Append-only журнал всех операций над wiki.

---

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
