package ru.sla.clarify.feature.chronology.ui.mapper

import ru.sla.clarify.core.domain.date.DATE_TIME_FORMATTER_DAY_MONTH_TIME
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.feature.chronology.domain.entity.ChronologyHistory
import ru.sla.clarify.feature.chronology.ui.components.canvas.branchColorIndexOf
import ru.sla.clarify.feature.chronology.ui.entity.ChronologyGraph
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranch
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranchStatus
import ru.sla.clarify.feature.chronology.ui.entity.GraphEpisode
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeDraft
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodePreview
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeRole
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

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
  val trunkBranchId = GraphBranch.Id(trunk.id.value)
  val trunkTimeByCommit = trunk.commits.associate { it.id to it.timestamp }

  val drafts = mutableListOf<GraphNodeDraft>()
  drafts += episodeDraftsOf(
    branchId = trunkBranchId,
    commits = trunk.commits,
    unreadCount = trunk.unreadCount,
    dim = false,
    membersById = membersById
  )
  // Фронт стоит на последнем событии магистрали, а не на «сейчас» по часам: §6.7 просит крайний
  // правый узел магистрали, а пустота между последним сообщением и текущей минутой — это не история.
  trunk.commits.lastOrNull()?.let { last ->
    drafts += GraphNodeDraft(
      id = GraphNode.Id("front"),
      branchId = trunkBranchId,
      role = GraphNodeRole.Front,
      at = last.timestamp
    )
  }

  val graphBranches = branches.mapIndexed { index, history ->
    val branch = history.branch
    val status = branch.mergeRequest.toGraphBranchStatus()
    val fork = forkDraftOf(
      branch = branch,
      trunk = trunk.id,
      trunkBranchId = trunkBranchId,
      trunkTimeByCommit = trunkTimeByCommit
    )
    val merge = mergeDraftOf(branch = branch, trunkBranchId = trunkBranchId, status = status)
    drafts += listOfNotNull(fork, merge)
    drafts += episodeDraftsOf(
      branchId = GraphBranch.Id(branch.id.value),
      commits = history.commits,
      unreadCount = branch.unreadCount,
      dim = status == GraphBranchStatus.Merged,
      membersById = membersById
    )
    GraphBranch(
      id = GraphBranch.Id(branch.id.value),
      // Порядковый номер по времени ветвления, а не номер дорожки: дорожка переиспользуется после
      // слияния, и цвет, взятый из неё, означал бы «номер ряда», а не «какая это тема».
      colorIndex = branchColorIndexOf(index + 1),
      forkedFrom = fork?.id,
      mergedAt = merge?.id,
      status = status
    )
  }

  val ordered = drafts.sortedWith(compareBy({ it.at }, { it.role.toSortOrder() }))
  return ChronologyGraph(
    nodes = ordered.toGraphNodes(),
    branches = graphBranches,
    branchNames = branches.associate { GraphBranch.Id(it.branch.id.value) to it.branch.name },
    episodeById = ordered.mapNotNull { draft -> draft.episode?.let { draft.id to it } }.toMap(),
    previewById = ordered.mapNotNull { draft -> draft.preview?.let { draft.id to it } }.toMap()
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
      // Идентификатор первого сообщения кластера, а не порядковый номер: номер съезжает, стоит
      // приехать сообщению в середину истории, и вместе с ним съезжает выбранный узел под открытой
      // карточкой.
      id = GraphNode.Id(first.id.value),
      branchId = branchId,
      role = GraphNodeRole.Episode,
      at = first.timestamp,
      episode = GraphEpisode(
        time = first.timestamp.format(DATE_TIME_FORMATTER_DAY_MONTH_TIME),
        count = cluster.size,
        snippet = last.text,
        myShare = cluster.count { it.isSelf }.toFloat() / cluster.size,
        unreadCount = unreadShares[index],
        dim = dim
      ),
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
 * @param trunk идентификатор магистрали в домене
 * @param trunkBranchId магистраль глазами раскладки: сама точка стоит на ней
 * @param trunkTimeByCommit время сообщений магистрали по их идентификаторам
 * @return узел-развилка или `null`, когда ставить его не на что
 */
private fun forkDraftOf(
  branch: Branch,
  trunk: Branch.Id,
  trunkBranchId: GraphBranch.Id,
  trunkTimeByCommit: Map<Commit.Id, LocalDateTime>
): GraphNodeDraft? {
  if (branch.parentBranchId != trunk) {
    return null
  }
  val at = trunkTimeByCommit[branch.branchedFromCommitId]
    ?: branch.createdAt.takeIf { it > 0 }?.toBranchTime()
    ?: return null
  return GraphNodeDraft(
    id = GraphNode.Id("fork-${branch.id.value}"),
    branchId = trunkBranchId,
    role = GraphNodeRole.Fork,
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
 * @param trunkBranchId магистраль глазами раскладки: точка стоит на ней, а не на дорожке ветки
 * @param status статус ветки, уже посчитанный вызывающим
 * @return узел-слияние или `null`, когда ветка не слита
 */
private fun mergeDraftOf(
  branch: Branch,
  trunkBranchId: GraphBranch.Id,
  status: GraphBranchStatus
): GraphNodeDraft? {
  if (status != GraphBranchStatus.Merged) {
    return null
  }
  val at = branch.mergeRequest?.mergedAt?.takeIf { it > 0 }?.toBranchTime() ?: return null
  return GraphNodeDraft(
    id = GraphNode.Id("merge-${branch.id.value}"),
    branchId = trunkBranchId,
    role = GraphNodeRole.Merge,
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
private fun List<GraphNodeDraft>.toGraphNodes(): List<GraphNode> {
  return mapIndexed { index, draft ->
    val previous = getOrNull(index - 1)
    GraphNode(
      id = draft.id,
      branchId = draft.branchId,
      role = draft.role,
      // Перед первым узлом паузы нет: отступ от края полотна дают поля, а не выдуманный зазор.
      gap = Duration.between(previous?.at ?: draft.at, draft.at).toTimeGap()
    )
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
private fun GraphNodeRole.toSortOrder(): Int {
  return when (this) {
    GraphNodeRole.Episode -> 0
    GraphNodeRole.Fork -> 1
    GraphNodeRole.Merge -> 2
    GraphNodeRole.Front -> 3
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
