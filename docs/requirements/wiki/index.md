---
tags:
  - root
---
# Wiki Index

**Summary**: Оглавление всей wiki проекта Clarify.

**Last updated**: 2026-09-05

---

## Features

- [Authorization](feature/authorization.md) — вход через Google, выбор стартового экрана по состоянию сессии, сохранение сессии между запусками.
- [Chat-Conversation](feature/chat-conversation.md) — главный экран: список всех переписок, создание чатов, навигация.
- [Chat-Direct-Thread](feature/chat-direct-thread.md) — личная переписка между двумя пользователями с поддержкой веток.
- [Chat-Group-Thread](feature/chat-group-thread.md) — переписка нескольких участников с ролью владельца.
- [Chat-Branch](feature/chat-branch.md) — подтреды (ветки) внутри переписки и их слияние через merge request.
- [Profile](feature/profile.md) — экран профиля текущего пользователя с выходом из аккаунта.

## Methods

### Branch

- [branches-live](method/branch/branches-live.md) — живая подписка на список веток conversation.
- [branch-live](method/branch/branch-live.md) — живая подписка на документ одной ветки.
- [create-branch](method/branch/create-branch.md) — создаёт новую ветку, ответвляясь от конкретного commit'а.

### Commit

- [read-commits](method/commit/read-commits.md) — загружает порцию сообщений ветки, опционально раньше указанного курсора.
- [commits-live](method/commit/commits-live.md) — живая подписка на сообщения ветки по conversationId.
- [commits-tail-live](method/commit/commits-tail-live.md) — живая подписка на сообщения ветки от курсора и дальше, без верхней границы.
- [direct-commits-live](method/commit/direct-commits-live.md) — живая подписка на сообщения ветки в direct-чате.
- [group-commits-live](method/commit/group-commits-live.md) — живая подписка на сообщения корневой ветки группы.
- [create-direct-commit](method/commit/create-direct-commit.md) — отправляет сообщение в direct-чат, при необходимости создавая conversation.
- [update-direct-commit](method/commit/update-direct-commit.md) — редактирует текст сообщения direct-чата, помечая его как изменённое; при правке последнего обновляет превью беседы.
- [update-branch-commit](method/commit/update-branch-commit.md) — редактирует текст сообщения ветки, помечая его как изменённое; при правке последнего обновляет превью ветки.
- [create-group-commit](method/commit/create-group-commit.md) — отправляет сообщение в корневую ветку групповой беседы.
- [create-branch-commit](method/commit/create-branch-commit.md) — отправляет сообщение в существующую ветку.
- [hide-commits](method/commit/hide-commits.md) — скрывает сообщения «только у себя», физически удаляя документ, когда его не видит больше никто.
- [delete-direct-commits](method/commit/delete-direct-commits.md) — удаляет сообщения direct-чата «у всех», пересчитывая `lastCommit*` беседы и счётчик непрочитанных собеседника.
- [delete-branch-commits](method/commit/delete-branch-commits.md) — удаляет сообщения ветки «у всех», пересчитывая `lastCommit*` ветки и счётчик непрочитанных ветки у собеседника.

### Conversation

- [conversations-live](method/conversation/conversations-live.md) — живая подписка на все conversations пользователя.
- [create-group-conversation](method/conversation/create-group-conversation.md) — создаёт группу и записывает создателя первым участником.
- [update-conversation-name](method/conversation/update-conversation-name.md) — обновляет название групповой беседы.
- [delete-conversations](method/conversation/delete-conversations.md) — пакетно удаляет несколько conversations.
- [delete-conversation](method/conversation/delete-conversation.md) — удаляет документ групповой беседы.

### Member

- [read-member](method/member/read-member.md) — возвращает данные участника беседы по его ID.
- [member-live](method/member/member-live.md) — живая подписка на документ конкретного участника.
- [members-live](method/member/members-live.md) — живая подписка на список участников группы.
- [create-commit-invite-member](method/member/create-commit-invite-member.md) — приглашает пользователя в группу.
- [delete-conversation-member](method/member/delete-conversation-member.md) — удаляет участника из группы.
- [delete-conversation-member-self](method/member/delete-conversation-member-self.md) — текущий пользователь покидает группу.
- [update-read-watermark](method/member/update-read-watermark.md) — записывает отметку о последнем прочитанном сообщении.

### Merge request

- [create-open-merge-request](method/merge-request/create-open-merge-request.md) — открывает merge request для ветки.
- [update-merge-approval](method/merge-request/update-merge-approval.md) — добавляет текущего пользователя в approvers.
- [delete-merge-request-approval](method/merge-request/delete-merge-request-approval.md) — отзывает одобрение текущего пользователя.
- [update-merge-finalize](method/merge-request/update-merge-finalize.md) — финализирует merge.
- [delete-merge-request](method/merge-request/delete-merge-request.md) — отменяет merge request.

### Unread count

- [unread-count-live](method/unread-count/unread-count-live.md) — живой счётчик непрочитанных conversation.
- [branch-unread-count-live](method/unread-count/branch-unread-count-live.md) — живой счётчик непрочитанных ветки.
- [update-unread-count](method/unread-count/update-unread-count.md) — сбрасывает счётчик непрочитанных conversation.
- [update-branch-unread-count](method/unread-count/update-branch-unread-count.md) — сбрасывает счётчик непрочитанных ветки.

### User

- [read-user](method/user/read-user.md) — возвращает документ пользователя по ID.
- [read-current-user](method/user/read-current-user.md) — возвращает документ текущего пользователя.
- [user-live](method/user/user-live.md) — живая подписка на изменения документа пользователя.
- [read-user-exists](method/user/read-user-exists.md) — проверяет существование документа пользователя по ID.
- [read-user-exists-by-email](method/user/read-user-exists-by-email.md) — проверяет регистрацию пользователя по email.
- [read-user-id-by-email](method/user/read-user-id-by-email.md) — находит UID пользователя по email.
- [read-users-by-email-prefix](method/user/read-users-by-email-prefix.md) — prefix-поиск пользователей по началу email.
- [create-user](method/user/create-user.md) — создаёт документ пользователя при первом входе.
- [update-user](method/user/update-user.md) — частично обновляет документ пользователя.
