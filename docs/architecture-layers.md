# Архитектурные слои

Поток зависимостей в проекте — строго **однонаправленный**:

```
data       ->   RepositoryImpl, источники данных (Firestore, SQLDelight, ...)
domain     ->   Repository (interface), Model (фасад над Repository), entity
ui         ->   ViewModel (умеет только Model), Compose-скрины
```

То есть: **`data <- domain <- ui`**. Слой не должен видеть то, что ниже него по
этой цепочке.

## Правила

### 1. ViewModel инжектит только Model

`ViewModel` обращается к домену **только через `Model`**. Никаких
`Repository`, никаких прямых вызовов источников данных, никаких `Firestore` /
`SQLDelight` импортов в UI-модуле.

```kotlin
// Плохо
class BranchViewModel @AssistedInject constructor(
  private val threadModel: ThreadModel,
  private val branchRepository: BranchRepository, // ← Repository в UI
  @Assisted branchIdValue: String
) : ViewModel<...>()

// Хорошо
class BranchViewModel @AssistedInject constructor(
  private val threadModel: ThreadModel,
  @Assisted branchIdValue: String
) : ViewModel<...>()
```

Если в `Model` нет нужной операции — добавь её туда, не тащи `Repository`
в `ViewModel`.

### 2. Model — фасад над Repository

`Model` живёт в `:domain` и инкапсулирует:

- **tasks** (`task<Input, Output> { ... }`) — для команд (sendMessage,
  approveMerge, ...);
- **observe-Flow'ы** — для подписок на состояние сущностей.

`Model` может инжектить несколько `Repository` и комбинировать их. Это его
основная работа.

```kotlin
@SingleIn(ThreadScope::class)
class ThreadModel @Inject constructor(
  private val threadRepository: ThreadRepository,
  private val branchRepository: BranchRepository,
  private val conversationRepository: ConversationRepository,
  private val authSessionRepository: AuthSessionRepository
) : ReactiveModel() {

  // observe — прокси к Repository
  fun branch(id: Branch.Id): Flow<Branch?> = branchRepository.branch(id)

  // task — команда с асинхронным результатом
  val approveMerge = task<Branch.Id, Unit>(name = "approveMerge") { branchId ->
    branchRepository.approveMerge(branchId)
  }
}
```

### 3. Repository — интерфейс в `:domain`, реализация в `:data`

В `:domain` лежит `interface Repository`. Реализация (`RepositoryImpl`) —
в `:data`, привязывается через `@ContributesBinding`. Repository знает про
источники данных (Firestore, SQLDelight) — это его зона ответственности.

```kotlin
// :domain
interface BranchRepository {
  fun branches(): Flow<List<Branch>>
  fun branch(id: Branch.Id): Flow<Branch?>
  suspend fun approveMerge(branchId: Branch.Id)
}

// :data
@SingleIn(ThreadScope::class)
@ContributesBinding(ThreadScope::class)
class BranchRepositoryImpl @Inject constructor(
  private val firestore: Firestore,
  private val inMemoryDB: InMemoryDB,
  private val threadMediator: ThreadMediator
) : BranchRepository { ... }
```

### 4. Никаких «срезок» снизу вверх

`Repository` не знает про `Model`. `RepositoryImpl` не знает про `ViewModel`.
`:data` не должен зависеть от `:ui`. Любая попытка «срезать» через нижний слой
вверх — нарушение.

## Антипаттерны

| Нарушение | Как переделать |
|---|---|
| `ViewModel` инжектит `Repository` (даже доменный интерфейс) | вынеси нужный метод в `Model`, инжекть `Model` |
| `ViewModel` напрямую дёргает `Firestore` / `SQLDelight` | соответствующий метод в `Repository` → прокси в `Model` |
| `Repository` зовёт `Model` | архитектурный wrong-way; пересмотри ответственности |
| `:data` зависит от `:ui` (build.gradle) | гарантированный знак нарушения; разруливай |

## Проверка PR

При ревью убедись:

1. В `:ui` модуле в `import` нет ни `*.Repository`, ни `*.RepositoryImpl`,
   ни `*.Firestore*`, ни `*.InMemoryDB*`.
2. `ViewModel`-конструкторы принимают только `*Model` (плюс
   `FlowEventSink`, `@Assisted`-параметры, и т. п.).
3. `:domain/build.gradle` не зависит от `:data`.
4. `:data/build.gradle` не зависит от `:ui`.

## Связанные документы

- [`firestore-naming.md`](firestore-naming.md) — REST-нейминг для `Firestore.kt`.
- [`sqldelight-naming.md`](sqldelight-naming.md) — имя `.sq`-запроса = SQL-операция.
- [`docs-template.md`](template/docs-template.md) — скелет, по которому пишутся документы в `docs/`.
