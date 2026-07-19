# План: редактирование сообщения в direct-треде

Статус: не начат.
Правило: **каждый этап завершается отдельным коммитом** (сообщение — в стиле проекта, английский).
После завершения этапа — отметить чекбокс и записать хэш коммита.

## Ключевые решения (зафиксированы)

1. **Черновиков нет.** Вход в edit перезаписывает поле текстом оригинала (набранное теряется).
   Отмена — режим сбрасывается, поле очищается. Ничего не восстанавливаем.
2. **`ViewState` остаётся плоским.** Новое поле `editingMessage: Commit.Message?` (null = не редактируем).
   `selectedCommitIds` — как есть. `editModeEnabled` переименовывается в `selectionEnabled`
   (та же механика производного флага в `updateSelection`).
3. **Back не трогает edit.** BackHandler-ы остаются как сейчас: selection → menu.
   Выход с экрана убивает `DirectThreadScope` и режим вместе с ним.
   Selection и Edit живут параллельно; «назад» снимает только selection.
4. **`createdAt` неприкосновенен.** На нём сортировка, курсоры пагинации, live-окно,
   группировка пузырей, watermark. Правка пишет только `text` + `editedAt` (Timestamp).
5. **Только транзакция, никакого `set(merge)`.** Цель могла быть удалена «у всех» вне
   live-окна — merge создал бы документ-зомби. Паттерн — `hideCommits`:
   читаем снапшот, `!exists()` → типизированная ошибка.
6. **`lastCommitText` обновляется внутри транзакции по серверным данным**, не из кэша:
   читаем conversation-документ, пишем превью только если `lastCommitAt == createdAt`
   редактируемого коммита. Это закрывает гонку с параллельной отправкой нового сообщения.
7. **Оптимистичный кэш после успеха.** Транзакции Firestore не дают latency-компенсированных
   локальных событий — после `await()` руками `insertOrReplace` в кэш (паттерн `applyDeleteCommits`).
8. **Офлайн-редактирование невозможно** (транзакция требует сервер) — консистентно с
   удалением «у всех»; ошибка → snackbar.
9. **Пустой trim = send disabled, НЕ удаление.** Send активен при `trim != пусто && != оригинал`.
10. **Пункт меню виден только для своих text-сообщений** (`isSelf && type == Text`),
    видимость — через nullable-колбэк (group/branch экраны колбэк не передают — пункта нет).
11. **Live-синхронизация правок работает без доработок:** `oldestCommitCursor` — flow,
    окно слушателя расширяется при пагинации, весь локальный кэш всегда внутри окна;
    пагинация ходит с `Source.SERVER`. Отдельной логики не требуется.
12. **«Цель удалили» во время редактирования:** сброс `editingMessage` по `onEach(commits)`
    (паттерн `focusedMessage`) + очистка поля + snackbar «Сообщение удалено».
    Если цель умерла молча (вне live-окна) — ошибка транзакции → удалить строку из кэша,
    сбросить режим, snackbar.
13. **Анимируется весь меняющийся контент — сквозное требование.** Ни одно состояние
    не переключается скачком. Только существующая мотус-система (`AppTheme.motion`:
    tween-набор, emphasized easing, `mediumTransitionSpec`), никаких локальных
    констант длительностей. Конкретные точки:
    - **Принцип для анимаций размера: `animateContentSize` всегда ВНУТРИ `.surface()`.**
      Тогда фон и клип рисуются по анимированному размеру на каждом кадре — граница
      контейнера целая, «растёт» только содержимое. Анимация снаружи surface рисует фон
      сразу целевым размером: рост сверху вниз, обрезанные скругления, «стекание» при
      схлопывании (пройдено, исправлено).
    - **Поле ввода** — `animateContentSize(BottomStart)` в `TextFieldDecoration` между
      surface и контентом: нижняя кромка и курсор на месте, контейнер растёт снизу вверх.
      Внешний `animateContentSize` из `PrimaryTextField` удалён (двойная анимация);
      текст ошибки анимируется отдельным `AnimatedContent` (fade + expand).
    - **Шапка композера** — слот с `animateContentSize` + fade содержимого;
      непроявленная часть уходит вниз за поле ввода (рисуется под ним) — шапка
      поднимается из-за поля без клипа скруглений. `slideInFromBottom`/`slideOutToBottom`
      из `AppMotion` удалены за ненадобностью.
    - **Смена цели при открытой шапке** (выбрали другое сообщение) — кроссфейд
      сниппета через `AnimatedContent` + `mediumTransitionSpec`.
    - **Send-кнопка** — морф иконки send ↔ галочка через `AnimatedContent`
      (паттерн `SelectionIndicator`); цвета фона/иконки уже на `animateColorAsState`.
    - **Пузырь после правки** — `animateContentSize` (`mediumTween`) внутри surface
      `BubbleSurface`; сдвиги соседей уже покрыты `animateItem()`.
    - **Метка «изменено»** — fade-in при появлении, скачком не возникает.
    - **Текст внутри поля** — программная замена (вход/выход/пере-выбор цели) мягко
      проявляет новый текст через `contentFadeKey` (ключ = id цели редактирования);
      обычная печать ключ не меняет и не фейдится. Placeholder подчиняется тому же
      правилу: fade/кроссфейд только в кадре программного свапа (маркер-поколение
      в `ContentFadeState`), ручной ввод/вставка/удаление переключают его мгновенно.

## Этапы

### Этап 1 — Firestore API
- [x] Commit: `[firestore] Add updateDirectCommit with edit transaction` — `92bd9a17`
- `lib/google/firestore/.../Firestore.kt`: `suspend fun updateDirectCommit(conversationId, commitId, text)`.
  - `require(text.isNotBlank())` на границе (fail loud).
  - Транзакция: чтение коммита → `!exists()` → типизированная ошибка (например, `CommitNotFoundException`).
  - Запись `text`, `editedAt = Timestamp.now()` через `transaction.update` (не `set`).
  - Чтение conversation-документа; если `lastCommitAt == createdAt` коммита —
    обновить `lastCommitText` (senderUid/lastCommitAt не меняются).
- `entity/CommitNM.kt`: поле `editedAt: Timestamp? = null`.
- Params-класс записи (по образцу `HideCommitParams` / `UpdateLastCommitParams`).

### Этап 2 — Кэш и доменная модель
- [x] Commit: `[database] Add editedAt to commit cache and domain model` — `6f592206`
- `database/.../chat/ChatCommit.sq`: колонка `editedAtNanos INTEGER` (nullable),
  добавить в `selectByBranchId`. БД in-memory — миграция не нужна.
- `entity/.../chat/Commit.kt`: `editedAt: LocalDateTime?` в `Commit.Message`
  (в интерфейс не поднимать — правится только text).
- `mapper/data/CommitMappers.kt`: прокладка в `toDomainModel` и `mapToCommit`.
  Компилятор найдёт остальные точки (group/branch репозитории проносят поле транзитом).

### Этап 3 — Repository и Model
- [x] Commit: `[direct-thread] Add editCommit to repository and model` — `9c2b71f0`
- `DirectThreadRepository` + `DirectThreadRepositoryImpl`: `editCommit(id: Commit.Id, text: String)`
  → `firestore.updateDirectCommit(...)` → после успеха оптимистичный `insertOrReplace`
  строки с новым text/editedAt (статус не трогать).
  При `CommitNotFoundException`: удалить строку из кэша, пробросить ошибку дальше.
- `DirectThreadModel`: remo-task `editCommit` (по образцу `deleteCommits`).

### Этап 4 — uikit: пузырь
- [x] Commit: `[uikit] Show edited label in bubble time slot` — `92463513`
- `BubbleMessage`: поле `edited: Boolean`.
- `BubbleMessageItem` / `BubbleTimeStatus`: локализуемая метка «изменено» перед временем
  (`--type-caption`, цвет как у времени). Слот TimeStatus меряется как placeable —
  раскладка `SubcomposeLayout` учтёт ширину сама.
- Анимации (решение 13): `animateContentSize` с `mediumTween` на смену размера пузыря
  после правки; fade-in метки «изменено».
- `mapper/ui/CommitMappers.kt`: `edited = commit.editedAt != null`.

### Этап 5 — uikit: композер
- [x] Commit: `[uikit] Add composer header and send icon slot to ChatTextField` — `e49d484f`
- Новый `ComposerHeader` (uikit): иконка + вертикальная полоса + заголовок (accent) +
  сниппет (1 строка, ellipsis) + крестик. Generic — под будущий Reply.
- `ChatTextField`: опциональный слот шапки над полем; параметр иконки send
  (галочка в режиме edit); `placeholder` уже параметризован.
- `AppMotion`: `slideInFromBottom`/`slideOutToBottom` — вертикальные зеркала
  существующих горизонтальных слайдов.
- Анимации (решение 13): `AnimatedVisibility` шапки (появление/скрытие),
  `AnimatedContent` + `mediumTransitionSpec` на смену сниппета при пере-выборе цели
  и на морф иконки send ↔ галочка.

### Этап 6 — uikit: пункт меню
- [x] Commit: `[uikit] Add edit action to message context menu` — `2f3720a3`
- `ChatCommits`: колбэк `onEditMessage: ((Commit.Message) -> Unit)?`,
  пункт «Редактировать» в `BubbleMessagePopup` (иконка `ic_pencil_24`),
  позиция — после «Ответить»-места, фактически вторым после «Создать ветку»
  (сейчас: ветка → **редактировать** → копировать → выбрать → удалить).
- Единый стиль пунктов проекта (без акцентных групп и красного из макета).

### Этап 7 — Feature UI: проводка режима
- [x] Commit: `[direct-thread] Wire message editing mode` — `f2725857`
- `ViewState`: `editModeEnabled` → `selectionEnabled` (переименование);
  новое поле `editingMessage: Commit.Message?`.
- `ViewIntents`: `startEditMessage(Commit.Message)`, `cancelEditMessage`,
  `submitEditMessage(String)`.
- `DirectThreadViewModel`:
  - `startEditMessage` — только для `isSelf` text-сообщений; закрывает меню.
  - `submitEditMessage` — trim, запуск `editCommit`; успех → сброс режима;
    ошибка → snackbar (режим остаётся).
  - В `onEach(commits)`: сброс `editingMessage`, если цель ушла из ленты
    (рядом с существующим сбросом `focusedMessage`) + snackbar «Сообщение удалено».
- `DirectThreadScreen` / `BottomArea`: префилл поля по смене `editingMessage`
  (локальное состояние, без черновиков), шапка `ComposerHeader`, send-галочка,
  disabled при `trim == пусто || trim == оригинал`. BackHandler-ы не трогаем.

### Этап 8 — Строки и тесты
- [x] Commit: `[direct-thread] Add message editing view model tests` — `35f01bbb`
  (строки добавлены раньше — по этапам 4/6/7, вместе с использующим их кодом)
- `strings.xml` (en) + `values-ru`: пункт меню, заголовок шапки, snackbar-ы,
  placeholder пустого поля.
- `DirectThreadViewModelTest`: вход в режим / отмена / успешная отправка /
  ошибка отправки / цель удалили при открытом режиме / selection поверх edit /
  удаление цели через selection сбрасывает режим.
- Анимации юнит-тестами не покрываются — ручная визуальная QA
  (как для контекстного меню).

### Этап 9 — Требования (wiki)
- [x] Commit: `[docs] Document message editing in requirements wiki` — `614f31ea`
- `docs/requirements/wiki/method/commit/update-direct-commit.md` — контракт метода
  (по стилю `create-direct-commit.md`; только публичный контракт).
- `docs/requirements/wiki/feature/chat-direct-thread.md` — раздел «Редактирование»,
  пункт меню в «Контекстное меню сообщения», запись в Version agenda.
- `wiki/index.md` — ссылка на новую страницу.

### Этап 10 — Внешняя зависимость: security rules
- [ ] Вне репозитория (деплой отдельно, как для групповых чатов).
- Разрешить `update` коммита только `senderUid == request.auth.uid`,
  изменяемые поля — только `text` и `editedAt`.
- Проверить, что запись `lastCommitText` в conversation участником разрешена
  (удаление уже пишет туда — скорее всего да).
- **До деплоя правил этап 3+ проверять на QA нельзя** — транзакция упадёт по правам,
  если текущие правила не разрешают update участникам.

## Вне скоупа (зафиксировано)

- Ответ на сообщение (Reply) — следующая фича, `ComposerHeader` делается generic под неё.
- Реакции, «Переслать», статус «в сети», чипы дат из макетов дизайнера — не существуют в проекте.
- Акцентные группы и красный «Удалить» в меню — стиль `Popup` проекта приоритетнее макета.
- Черновики композера — отказ (решение 1).
- Редактирование в group-треде и ветках — не проводится (колбэк не передаётся).
