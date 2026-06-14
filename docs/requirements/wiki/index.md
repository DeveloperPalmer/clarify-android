---
tags:
  - root
---
# Wiki Index

**Summary**: Оглавление всей wiki проекта Clarify.

**Last updated**: 2026-06-13

---

## Features

- [authorization](feature/authorization.md) — Вход через Google, управление сессией
- [chat-conversation](feature/chat-conversation.md) — Главный экран: список личных и групповых чатов
- [chat-direct-thread](feature/chat-direct-thread.md) — Личная переписка 1:1 с поддержкой веток
- [chat-group-thread](feature/chat-group-thread.md) — Групповой чат с ролями владельца и участника
- [chat-branch](feature/chat-branch.md) — Ветки внутри личного чата с merge request процессом
- [profile](feature/profile.md) — Профиль пользователя и выход из аккаунта

## Umbrellas

## Methods

### user/
- [get-current-user](method/user/get-current-user.md) — Получить документ текущего авторизованного пользователя
- [get-user](method/user/get-user.md) — Получить документ пользователя по ID
- [get-user-exists](method/user/get-user-exists.md) — Проверить существование пользователя по ID → Boolean
- [get-user-exists-by-email](method/user/get-user-exists-by-email.md) — Проверить существование пользователя по email → Boolean
- [get-user-id-by-email](method/user/get-user-id-by-email.md) — Найти UID пользователя по email
- [get-users-by-email-prefix](method/user/get-users-by-email-prefix.md) — Prefix-поиск пользователей по email
- [observe-user](method/user/observe-user.md) — Живая подписка на документ пользователя
- [patch-user](method/user/patch-user.md) — Частично обновить документ пользователя
- [post-user](method/user/post-user.md) — Создать документ пользователя (первичная вставка)

### conversation/
- [delete-conversations](method/conversation/delete-conversations.md) — Пакетно удалить conversations
- [delete-group-conversation](method/conversation/delete-group-conversation.md) — Удалить групповую беседу
- [observe-conversations](method/conversation/observe-conversations.md) — Живая подписка на все conversations текущего пользователя
- [patch-group-name](method/conversation/patch-group-name.md) — Обновить название группы
- [post-group-conversation](method/conversation/post-group-conversation.md) — Создать групповую беседу

### member/
- [delete-member](method/member/delete-member.md) — Удалить участника из группы
- [leave-group](method/member/leave-group.md) — Текущий пользователь покидает группу
- [observe-member](method/member/observe-member.md) — Живая подписка на документ участника
- [observe-members](method/member/observe-members.md) — Живая подписка на список участников группы
- [patch-read-watermark](method/member/patch-read-watermark.md) — Записать отметку о прочтении
- [post-invite-member](method/member/post-invite-member.md) — Пригласить пользователя в группу

### commit/
- [get-commits](method/commit/get-commits.md) — Загрузить порцию сообщений ветки (пагинация)
- [observe-commits](method/commit/observe-commits.md) — Живая подписка на сообщения ветки
- [observe-direct-commits](method/commit/observe-direct-commits.md) — Живая подписка на сообщения direct-чата
- [observe-group-commits](method/commit/observe-group-commits.md) — Живая подписка на сообщения группового чата
- [post-branch-commit](method/commit/post-branch-commit.md) — Отправить сообщение в ветку
- [post-commit](method/commit/post-commit.md) — Отправить сообщение в direct-чат
- [post-group-commit](method/commit/post-group-commit.md) — Отправить сообщение в групповой чат

### unread-count/
- [observe-branch-unread-count](method/unread-count/observe-branch-unread-count.md) — Живая подписка на счётчик непрочитанных ветки
- [observe-unread-count](method/unread-count/observe-unread-count.md) — Живая подписка на счётчик непрочитанных conversation
- [patch-branch-clear-unread-count](method/unread-count/patch-branch-clear-unread-count.md) — Сбросить счётчик непрочитанных ветки
- [patch-clear-unread-count](method/unread-count/patch-clear-unread-count.md) — Сбросить счётчик непрочитанных conversation

### branch/
- [observe-branches](method/branch/observe-branches.md) — Живая подписка на список веток
- [post-branch](method/branch/post-branch.md) — Создать новую ветку

### merge-request/
- [delete-merge-approval](method/merge-request/delete-merge-approval.md) — Отозвать одобрение merge request
- [delete-merge-request](method/merge-request/delete-merge-request.md) — Отменить merge request
- [patch-merge-approval](method/merge-request/patch-merge-approval.md) — Одобрить merge request
- [patch-merge-finalize](method/merge-request/patch-merge-finalize.md) — Финализировать merge
- [post-merge-request](method/merge-request/post-merge-request.md) — Открыть merge request для ветки
