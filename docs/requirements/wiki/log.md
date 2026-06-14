---
tags:
  - root
---
# Operations Log

Append-only журнал всех операций над wiki.

---

- **2026-06-13** — Инициализация wiki: создан скелет (index.md, log.md, папки raw/, feature/, umbrella/, method/).
- **2026-06-13** — Добавлены 6 feature-страниц: authorization, conversation-list, direct-chat, group-chat, branches, profile.
- **2026-06-13** — Добавлены 38 method-страниц для класса `Firestore.kt`: users (9), conversations (5), members (6), commits (7), unread counts (4), branches (2), merge requests (5). Обновлён index.md.
- **2026-06-13** — Реструктуризация method/: плоская папка разбита на 7 подпапок (user/, conversation/, member/, commit/, unread-count/, branch/, merge-request/). Переименования: `is-*` → `get-*` (boolean-методы), `*-live` → `observe-*` (подписки). Обновлены заголовки переименованных файлов и index.md.
