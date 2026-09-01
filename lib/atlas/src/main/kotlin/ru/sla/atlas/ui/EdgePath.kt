package ru.sla.atlas.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import ru.sla.atlas.entity.Edge
import kotlin.math.abs

/**
 * Достраивает путь по точкам излома ребра: прямые, скруглённые углы и мостики.
 *
 * Живёт отдельно от покраски, потому что это геометрия, а не оформление: где линия поворачивает и
 * где горбится, выводится из самого ребра, а чем её провести — толщиной, штрихом, цветом — решает
 * рисующий. Пока обе половины стояли в одной функции, она читала и точки излома, и статус ветки, и
 * кадр анимации, которой в библиотеке нет.
 *
 * Углы срезаются вручную квадратичной Безье, а не `PathEffect.cornerPathEffect`, и это решение с
 * причиной. Эффект скругляет **все** вершины контура, включая полученные из дуги мостика, — то есть
 * портит ровно тот приём, ради которого мостик заведён; вдобавок он молча ужимает радиус до
 * половины сегмента, чего в его документации нет. Отклонение параболы от настоящей дуги при радиусе
 * 8 dp — 0.49 dp, и увидеть его нельзя.
 *
 * @param edge ребро в координатах полотна
 * @param cornerRadius радиус скругления углов
 * @param hopRadius радиус мостика
 */
fun Path.addOrthogonalRoute(edge: Edge, cornerRadius: Float, hopRadius: Float) {
  val points = edge.points
  moveTo(points.first().x, points.first().y)
  points.indices.drop(1).forEach { index ->
    val target = points[index]
    val previous = points[index - 1]
    val isLast = index == points.lastIndex
    // Угол срезается на радиус с обеих сторон: до угла ведёт прямая, сам угол — контрольная точка
    // квадратичной Безье, ровно как в прототипе.
    val corner = if (isLast) 0f else minOf(cornerRadius, distanceTo(target, points[index + 1]) / 2f)
    val approach = shortenedTowards(previous, target, corner)
    if (previous.y == target.y) {
      addHorizontalWithHops(previous, approach, edge.hops, hopRadius)
    } else {
      lineTo(approach.x, approach.y)
    }
    if (!isLast) {
      val departure = shortenedTowards(points[index + 1], target, corner)
      quadraticTo(target.x, target.y, departure.x, departure.y)
    }
  }
}

/**
 * Ведёт горизонтальный участок, поднимая полукруглый мостик над каждой чужой вертикалью.
 *
 * Мостик — ровно полуокружность: хорда 12 dp при радиусе 6 равна двум радиусам, поэтому дуга
 * поднимается на 6 dp и возвращается на линию. Дуга идёт вверх независимо от направления линии —
 * так же, как в схемах метро и в git-графах, откуда приём и взят.
 */
private fun Path.addHorizontalWithHops(
  from: Offset,
  to: Offset,
  hops: List<Float>,
  hopRadius: Float
) {
  val forward = to.x >= from.x
  val inside = hops.filter { hop ->
    if (forward) {
      hop > from.x + hopRadius && hop < to.x - hopRadius
    } else {
      hop < from.x - hopRadius && hop > to.x + hopRadius
    }
  }
  val ordered = if (forward) inside.sorted() else inside.sortedDescending()
  ordered.forEach { hop ->
    val entry = if (forward) hop - hopRadius else hop + hopRadius
    val exit = if (forward) hop + hopRadius else hop - hopRadius
    lineTo(entry, from.y)
    arcTo(
      rect = Rect(
        left = hop - hopRadius,
        top = from.y - hopRadius,
        right = hop + hopRadius,
        bottom = from.y + hopRadius
      ),
      startAngleDegrees = if (forward) 180f else 0f,
      sweepAngleDegrees = if (forward) 180f else -180f,
      forceMoveTo = false
    )
    lineTo(exit, from.y)
  }
  lineTo(to.x, to.y)
}

/** Точка на отрезке `from → to`, отступающая от `to` на `distance`. */
private fun shortenedTowards(from: Offset, to: Offset, distance: Float): Offset {
  return when {
    from.x == to.x -> Offset(to.x, to.y + distance * if (from.y > to.y) 1f else -1f)
    else -> Offset(to.x + distance * if (from.x > to.x) 1f else -1f, to.y)
  }
}

/** Длина отрезка между соседними точками ортогональной ломаной. */
private fun distanceTo(from: Offset, to: Offset): Float {
  return abs(to.x - from.x) + abs(to.y - from.y)
}
