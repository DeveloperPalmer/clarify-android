package ru.sla.clarify.feature.chronology.ui.mapper

import ru.sla.atlas.entity.BasicNode
import ru.sla.atlas.entity.Graph
import ru.sla.atlas.entity.TimeGap
import ru.sla.clarify.core.domain.date.DATE_TIME_FORMATTER_DAY_MONTH_TIME
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.feature.chronology.domain.entity.ChronologyHistory
import ru.sla.clarify.feature.chronology.ui.entity.ChronologyGraph
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeDraft
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodePreview
import ru.sla.clarify.feature.chronology.ui.entity.Node
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import ru.sla.atlas.entity.Branch as GraphBranch

/**
 * История беседы — в граф, который рисует полотно.
 *
 * **Порядок узлов задаёт время, а не ветка.** Ось X накапливается по порядку списка, поэтому набор,
 * сгруппированный по веткам, поставил бы ветку от 8 марта левее ветки от 6-го — прямое нарушение
 * §4.1 брифа. Узлы всех веток поэтому собираются в один список и сортируются один раз, и только
 * после этого у каждого появляется пауза: она считается от соседа **по времени**, а кто сосед,
 * до сортировки неизвестно.
 *
 * **Точки ветвления и слияния — настоящие узлы списка, а не отметки на линии.** На этом держится то,
 * что вертикаль ребра не может пересечь чужую плашку: накопительная ось даёт каждому узлу
 * собственный отрезок по X, и соседи лежат либо левее, либо правее — но не под ним.
 *
 * Дорожек, цветов дорожек и координат здесь нет: дорожка — результат раскладки, а не свойство узла,
 * и считает её `graphLanesOf` по занятости. Отсюда уходит только идентичность ветки — её оттенок.
 *
 * @return граф целиком: узлы, ветки и содержимое узлов, которые нельзя подменять порознь
 */
internal fun ChronologyHistory.toChronologyGraph(): ChronologyGraph {
  val membersById = members.associateBy { it.id.value }
  val baselineBranchId = GraphBranch.Id(baseline.id.value)
  val baselineTimeByCommit = baseline.commits.associate { it.id to it.timestamp }

  val drafts = mutableListOf<GraphNodeDraft>()
  drafts += episodeDraftsOf(
    branchId = baselineBranchId,
    commits = baseline.commits,
    unreadCount = baseline.unreadCount,
    dim = false,
    membersById = membersById
  )
  // Фронт стоит на последнем событии магистрали, а не на «сейчас» по часам: §6.7 просит крайний
  // правый узел магистрали, а пустота между последним сообщением и текущей минутой — это не история.
  baseline.commits.lastOrNull()?.let { last ->
    drafts += GraphNodeDraft(
      node = Node.Front(id = BasicNode.Id("front"), gap = UNSET_GAP),
      branchId = baselineBranchId,
      at = last.timestamp
    )
  }

  val graphBranches = branches.mapIndexed { index, history ->
    val branch = history.branch
    val status = branch.mergeRequest.toBranchStatus()
    val fork = forkDraftOf(
      branch = branch,
      baseline = baseline.id,
      baselineBranchId = baselineBranchId,
      baselineTimeByCommit = baselineTimeByCommit
    )
    val merge = mergeDraftOf(branch = branch, baselineBranchId = baselineBranchId, status = status)
    drafts += listOfNotNull(fork, merge)
    drafts += episodeDraftsOf(
      branchId = GraphBranch.Id(branch.id.value),
      commits = history.commits,
      unreadCount = branch.unreadCount,
      dim = status == GraphBranch.Status.Merged,
      membersById = membersById
    )
    GraphBranch(
      id = GraphBranch.Id(branch.id.value),
      // Состав ветки заполняется ниже, когда узлы всех веток сведены в один список и отсортированы:
      // порядок узлов — свойство графа, и до сортировки его нет ни у кого.
      nodeIds = emptyList(),
      // Порядковый номер по времени ветвления, а не номер дорожки: дорожка переиспользуется после
      // слияния, и цвет, взятый из неё, означал бы «номер ряда», а не «какая это тема».
      colorIndex = GraphBranch.colorIndexOf(
        order = index + 1,
        // Шесть оттенков идентичности — `graphLane1`…`graphLane6`, см. `Int.toBranchColor`.
        paletteSize = 6
      ),
      forkedFrom = fork?.node?.id,
      mergedAt = merge?.node?.id,
      status = status
    )
  }

  val ordered = drafts.sortedWith(compareBy({ it.at }, { it.node.toSortOrder() }))
  val nodes = ordered.toNodes()
  val placed = ordered.zip(nodes)
  val nodeIdsByBranch = placed.groupBy({ (draft, _) -> draft.branchId }, { (_, node) -> node.id })
  return ChronologyGraph(
    layout = Graph(
      nodes = nodes,
      // Магистраль — такая же ветка, как остальные, и в графе она названа отдельно: её узлы иначе
      // не принадлежали бы никому. Цвет нулевой, развилки и слияния у неё нет по определению.
      baseline = GraphBranch(
        id = baselineBranchId,
        nodeIds = nodeIdsByBranch[baselineBranchId].orEmpty(),
        colorIndex = 0,
        forkedFrom = null,
        mergedAt = null,
        status = GraphBranch.Status.Alive
      ),
      branches = graphBranches.map { it.copy(nodeIds = nodeIdsByBranch[it.id].orEmpty()) }
    ),
    branchNames = branches.associate { GraphBranch.Id(it.branch.id.value) to it.branch.name },
    previewById = placed.mapNotNull { (draft, node) ->
      draft.preview?.let { node.id to it }
    }.toMap()
  )
}

/**
 * Эпизоды одной ветки: её лента, разбитая на кластеры, вместе с содержимым плашек и карточек.
 *
 * @param branchId ветка, которой принадлежат узлы
 * @param commits лента ветки по возрастанию времени
 * @param unreadCount непрочитанное ветки целиком — разойдётся по эпизодам с конца
 * @param dim ветка слита: её эпизоды рисуются приглушёнными
 * @param membersById участники беседы по идентификатору пользователя
 * @return узлы-эпизоды в порядке ленты
 */
private fun episodeDraftsOf(
  branchId: GraphBranch.Id,
  commits: List<Commit>,
  unreadCount: Long,
  dim: Boolean,
  membersById: Map<String, Member>
): List<GraphNodeDraft> {
  val clusters = commits.toEpisodeClusters()
  val unreadShares = clusters.map { it.size }.toUnreadShares(unreadCount)
  return clusters.mapIndexed { index, cluster ->
    val first = cluster.first()
    val last = cluster.last()
    val author = membersById[last.senderId.value]
    GraphNodeDraft(
      node = Node.Episode(
        // Идентификатор первого сообщения кластера, а не порядковый номер: номер съезжает, стоит
        // приехать сообщению в середину истории, и вместе с ним съезжает выбранный узел под
        // открытой карточкой.
        id = BasicNode.Id(first.id.value),
        gap = UNSET_GAP,
        time = first.timestamp.format(DATE_TIME_FORMATTER_DAY_MONTH_TIME),
        count = cluster.size,
        snippet = last.text,
        myShare = cluster.count { it.isSelf }.toFloat() / cluster.size,
        unreadCount = unreadShares[index],
        dim = dim
      ),
      branchId = branchId,
      at = first.timestamp,
      preview = GraphNodePreview(
        authorName = author?.displayName.orEmpty(),
        authorPhotoUrl = author?.photoUrl,
        text = last.text,
        time = last.timestamp.format(DATE_TIME_FORMATTER_DAY_MONTH_TIME)
      )
    )
  }
}

/**
 * Точка ветвления на магистрали — §6.4 брифа.
 *
 * Стоит на сообщении-развилке — там, где разговор разошёлся, а не там, где пользователь нажал
 * кнопку. Когда самого сообщения в кэше нет (лента страничится с конца и до него не дошла), точка
 * встаёт на момент создания ветки: величина другая, но тоже настоящая. Ветка, у которой не нашлось
 * ни той, ни другой, точки не получает вовсе — узел в нулевой секунде эпохи утянул бы начало графа
 * в 1970 год и сжал бы всю остальную историю в точку.
 *
 * Ветки от ветки точки на магистрали тоже не получают: их развилка лежит не на ней. Показывается
 * такая ветка всё равно — раз она создана, она существует (решение владельца, журнал, итерация 39).
 *
 * @param branch ветка, которая уходит
 * @param baseline идентификатор магистрали в домене
 * @param baselineBranchId магистраль глазами раскладки: сама точка стоит на ней
 * @param baselineTimeByCommit время сообщений магистрали по их идентификаторам
 * @return узел-развилка или `null`, когда ставить его не на что
 */
private fun forkDraftOf(
  branch: Branch,
  baseline: Branch.Id,
  baselineBranchId: GraphBranch.Id,
  baselineTimeByCommit: Map<Commit.Id, LocalDateTime>
): GraphNodeDraft? {
  if (branch.parentBranchId != baseline) {
    return null
  }
  val at = baselineTimeByCommit[branch.branchedFromCommitId]
    ?: branch.createdAt.takeIf { it > 0 }?.toBranchTime()
    ?: return null
  return GraphNodeDraft(
    node = Node.Fork(id = BasicNode.Id("fork-${branch.id.value}"), gap = UNSET_GAP),
    branchId = baselineBranchId,
    at = at
  )
}

/**
 * Точка слияния на магистрали — §6.6 брифа.
 *
 * Только у завершённого merge request: до финализации точки на магистрали нет вовсе, и открытый
 * запрос рисуется пунктиром линии, а не узлом.
 *
 * @param branch ветка, которая вернулась
 * @param baselineBranchId магистраль глазами раскладки: точка стоит на ней, а не на дорожке ветки
 * @param status статус ветки, уже посчитанный вызывающим
 * @return узел-слияние или `null`, когда ветка не слита
 */
private fun mergeDraftOf(
  branch: Branch,
  baselineBranchId: GraphBranch.Id,
  status: GraphBranch.Status
): GraphNodeDraft? {
  if (status != GraphBranch.Status.Merged) {
    return null
  }
  val at = branch.mergeRequest?.mergedAt?.takeIf { it > 0 }?.toBranchTime() ?: return null
  return GraphNodeDraft(
    node = Node.Merge(id = BasicNode.Id("merge-${branch.id.value}"), gap = UNSET_GAP),
    branchId = baselineBranchId,
    at = at
  )
}

/**
 * Черновики в узлы: у каждого появляется пауза, отделяющая его от соседа слева.
 *
 * Считается по **соседу в списке**, а не по соседу той же ветки: зазор раздвигает узлы по общей оси
 * времени, и пауза внутри ветки, посчитанная в обход чужих узлов, поставила бы её плашки поверх них.
 *
 * @return узлы в том же порядке, уже с паузами
 */
private fun List<GraphNodeDraft>.toNodes(): List<Node> {
  return mapIndexed { index, draft ->
    val previous = getOrNull(index - 1)
    // Перед первым узлом паузы нет: отступ от края полотна дают поля, а не выдуманный зазор.
    draft.node.withGap(Duration.between(previous?.at ?: draft.at, draft.at).toTimeGap())
  }
}

/**
 * Тот же узел с проставленной паузой.
 *
 * Перечисление здесь неизбежно: пауза лежит в каждом роде узла своим полем, и общего `copy` у
 * запечатанного типа нет. Зато оно полное — род, забытый в этом `when`, не компилируется, а
 * забытый в заглушке просто уехал бы на экран с чужим зазором.
 *
 * @param gap пауза, посчитанная по соседу слева
 * @return узел, готовый попасть в граф
 */
private fun Node.withGap(gap: TimeGap): Node {
  return when (this) {
    is Node.Episode -> copy(gap = gap)
    is Node.Fork -> copy(gap = gap)
    is Node.Merge -> copy(gap = gap)
    is Node.Front -> copy(gap = gap)
  }
}

/**
 * Лента — в кластеры подряд идущих сообщений, разделённые паузой (§5 брифа).
 *
 * Системные события в кластеры не попадают: приглашение участника — не реплика, и попав в кластер,
 * оно раздуло бы счётчик сообщений и подменило бы сниппет пустой строкой, потому что текста у него
 * нет.
 *
 * @return кластеры в порядке ленты; пустых кластеров не бывает
 */
private fun List<Commit>.toEpisodeClusters(): List<List<Commit.Message>> {
  val clusters = mutableListOf<MutableList<Commit.Message>>()
  filterIsInstance<Commit.Message>().forEach { message ->
    val current = clusters.lastOrNull()
    if (current == null || current.isClosedFor(message)) {
      clusters += mutableListOf(message)
    } else {
      current += message
    }
  }
  return clusters
}

/**
 * Пора ли начинать новый кластер.
 *
 * @param next следующее сообщение ленты
 * @return `true`, когда сообщение в этот кластер уже не входит
 */
private fun List<Commit.Message>.isClosedFor(next: Commit.Message): Boolean {
  // Порог паузы — три часа, потолок кластера — двадцать четыре сообщения: длиннее эпизод режется
  // принудительно, иначе один плотный день стал бы единственной плашкой (§5 брифа).
  return size >= 24 || Duration.between(last().timestamp, next.timestamp) > Duration.ofHours(3)
}

/**
 * Непрочитанное ветки — по её эпизодам, начиная с последнего.
 *
 * Домен даёт одно число на ветку, а бейдж живёт на плашке, и распределение здесь не выдумка:
 * непрочитанными считаются сообщения после отметки прочтения, то есть ровно последние `N` ленты.
 * Раскладывать их с конца — единственный способ показать то же самое число, ничего не пересчитывая
 * (решение владельца: прочитанность показываем, не считаем).
 *
 * @param unreadCount непрочитанное ветки целиком
 * @return сколько непрочитанного приходится на каждый эпизод, в порядке эпизодов
 */
private fun List<Int>.toUnreadShares(unreadCount: Long): List<Long> {
  var rest = unreadCount
  val shares = LongArray(size)
  for (index in indices.reversed()) {
    val share = minOf(rest, this[index].toLong())
    shares[index] = share
    rest -= share
  }
  return shares.toList()
}

/**
 * Порядок узлов, попавших на одну и ту же секунду.
 *
 * Совпадение это не экзотика, а обычное дело: фронт стоит на последнем сообщении магистрали, а
 * развилка — на том самом сообщении, от которого ушла ветка, и оно бывает первым в своём эпизоде.
 *
 * Порядок смысловой: сначала то, что было сказано, потом то, что из сказанного следует. Ветка ушла
 * **от** сообщения, значит после него; вернулась она позже, чем ушла; фронт замыкает магистраль.
 *
 * @return ключ сортировки внутри одной секунды
 */
private fun Node.toSortOrder(): Int {
  return when (this) {
    is Node.Episode -> 0
    is Node.Fork -> 1
    is Node.Merge -> 2
    is Node.Front -> 3
  }
}

/**
 * Секунды эпохи — в локальное время.
 *
 * Отдельно от `Long.toLocalDateTime` из `mapper/data`: тот считает миллисекунды, а ветка и её merge
 * request хранят время в секундах — Firestore отдаёт `Timestamp`, и запись в кэш округляет его до
 * секунды. Одно имя на две единицы разошлось бы молча, и разошлось бы в тысячу раз.
 *
 * @return момент по часам устройства
 */
private fun Long.toBranchTime(): LocalDateTime {
  return Instant.ofEpochSecond(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
}

/**
 * Пауза, которой ещё нет.
 *
 * Узел собирается до того, как выяснится, кто стоит от него слева, а пауза у него не необязательное
 * поле, и быть им не должна: в графе узла без паузы не бывает. Значение здесь поэтому произвольное
 * — важно не оно, а то, что `toNodes` меняет паузу **каждому** узлу и мимо неё в граф не пройти.
 */
private val UNSET_GAP = TimeGap.Minutes
