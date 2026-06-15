---
tags:
  - root
---
# Wiki Index

**Summary**: Оглавление всей wiki проекта Clarify.

**Last updated**: 2026-06-15

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

- [observe-branches](method/branch/observe-branches.md) — живая подписка на список веток conversation.
- [post-branch](method/branch/post-branch.md) — создаёт новую ветку, ответвляясь от конкретного commit'а.

### Commit

- [get-commits](method/commit/get-commits.md) — загружает порцию сообщений ветки, опционально раньше указанного момента.
- [observe-commits](method/commit/observe-commits.md) — живая подписка на сообщения ветки по conversationId.
- [observe-direct-commits](method/commit/observe-direct-commits.md) — живая подписка на сообщения ветки в direct-чате.
- [observe-group-commits](method/commit/observe-group-commits.md) — живая подписка на сообщения корневой ветки группы.
- [post-commit](method/commit/post-commit.md) — отправляет сообщение в direct-чат, при необходимости создавая conversation.
- [post-group-commit](method/commit/post-group-commit.md) — отправляет сообщение в корневую ветку групповой беседы.
- [post-branch-commit](method/commit/post-branch-commit.md) — отправляет сообщение в существующую ветку.

### Conversation

- [observe-conversations](method/conversation/observe-conversations.md) — живая подписка на все conversations пользователя.
- [post-create-group-conversation](method/conversation/post-create-group-conversation.md) — создаёт группу и записывает создателя первым участником.
- [patch-group-name](method/conversation/patch-group-name.md) — обновляет название групповой беседы.
- [delete-conversations](method/conversation/delete-conversations.md) — пакетно удаляет несколько conversations.
- [delete-group-conversation](method/conversation/delete-group-conversation.md) — удаляет документ групповой беседы.

### Member

- [observe-member](method/member/observe-member.md) — живая подписка на документ конкретного участника.
- [observe-members](method/member/observe-members.md) — живая подписка на список участников группы.
- [post-invite-member](method/member/post-invite-member.md) — приглашает пользователя в группу.
- [delete-member](method/member/delete-member.md) — удаляет участника из группы.
- [leave-group](method/member/leave-group.md) — текущий пользователь покидает группу.
- [patch-read-watermark](method/member/patch-read-watermark.md) — записывает отметку о последнем прочитанном сообщении.

### Merge request

- [post-merge-request](method/merge-request/post-merge-request.md) — открывает merge request для ветки.
- [patch-merge-approval](method/merge-request/patch-merge-approval.md) — добавляет текущего пользователя в approvers.
- [delete-merge-approval](method/merge-request/delete-merge-approval.md) — отзывает одобрение текущего пользователя.
- [patch-merge-finalize](method/merge-request/patch-merge-finalize.md) — финализирует merge.
- [delete-merge-request](method/merge-request/delete-merge-request.md) — отменяет merge request.

### Unread count

- [observe-unread-count](method/unread-count/observe-unread-count.md) — живой счётчик непрочитанных conversation.
- [observe-branch-unread-count](method/unread-count/observe-branch-unread-count.md) — живой счётчик непрочитанных ветки.
- [patch-clear-unread-count](method/unread-count/patch-clear-unread-count.md) — сбрасывает счётчик непрочитанных conversation.
- [patch-branch-clear-unread-count](method/unread-count/patch-branch-clear-unread-count.md) — сбрасывает счётчик непрочитанных ветки.

### User

- [get-user](method/user/get-user.md) — возвращает документ пользователя по ID.
- [get-current-user](method/user/get-current-user.md) — возвращает документ текущего пользователя.
- [observe-user](method/user/observe-user.md) — живая подписка на изменения документа пользователя.
- [get-user-exists](method/user/get-user-exists.md) — проверяет существование документа пользователя по ID.
- [get-user-exists-by-email](method/user/get-user-exists-by-email.md) — проверяет регистрацию пользователя по email.
- [get-user-id-by-email](method/user/get-user-id-by-email.md) — находит UID пользователя по email.
- [get-users-by-email-prefix](method/user/get-users-by-email-prefix.md) — prefix-поиск пользователей по началу email.
- [post-user](method/user/post-user.md) — создаёт документ пользователя при первом входе.
- [patch-user](method/user/patch-user.md) — частично обновляет документ пользователя.
