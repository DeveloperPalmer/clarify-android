package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Разложение — единственное, что удерживает бросок на его направлении, поэтому проверяется само по
 * себе, без камеры и без анимации.
 *
 * Главное утверждение — предпоследний тест: у покомпонентного затухания отношение пройденных
 * смещений уезжает от отношения скоростей на десятки процентов, а здесь обязано быть постоянным на
 * всём интервале. Реализация, переписанная на затухание по каждой оси отдельно, этот тест провалит.
 */
class FlingDirectionTest {

  @Test
  fun `magnitude is the speed regardless of direction`() {
    assertEquals(500f, FlingDirection(Velocity(x = 300f, y = 400f)).magnitude)
    assertEquals(500f, FlingDirection(Velocity(x = -300f, y = -400f)).magnitude)
  }

  @Test
  fun `a decomposed distance keeps its length`() {
    val direction = FlingDirection(Velocity(x = 1200f, y = -700f))

    val step = direction.offsetOf(distance = 240f)

    assertEquals(240f, sqrt(step.x * step.x + step.y * step.y), 1e-3f)
  }

  @Test
  fun `a decomposed distance keeps the release angle`() {
    val direction = FlingDirection(Velocity(x = 1200f, y = -700f))

    val near = direction.offsetOf(distance = 3f)
    val far = direction.offsetOf(distance = 900f)

    assertEquals(-700f / 1200f, near.y / near.x, 1e-5f, "направление задано в момент отпускания")
    assertEquals(near.y / near.x, far.y / far.x, 1e-5f, "и не меняется по дороге")
  }

  @Test
  fun `the decomposition matches the platform angle formula`() {
    // Допуск, а не точное равенство: платформа считает через atan2 с косинусом, здесь — делением на
    // модуль, и во float это одно и то же с точностью до последнего разряда, а не побитово.
    listOf(1200f to 700f, -1200f to 700f, 1200f to -700f, -1200f to -700f, 0f to 900f, 900f to 0f)
      .forEach { (vx, vy) ->
        val step = FlingDirection(Velocity(x = vx, y = vy)).offsetOf(distance = 320f)
        val angle = atan2(y = vy, x = vx)

        assertEquals(abs(cos(angle) * 320f) * sign(vx), step.x, 1e-3f, "ось X при v=($vx, $vy)")
        assertEquals(abs(sin(angle) * 320f) * sign(vy), step.y, 1e-3f, "ось Y при v=($vx, $vy)")
      }
  }

  @Test
  fun `a zero velocity has no direction`() {
    val direction = FlingDirection(Velocity.Zero)

    assertEquals(0f, direction.magnitude)
    assertEquals(Offset.Zero, direction.offsetOf(distance = 500f))
  }

  @Test
  fun `a NaN velocity has no magnitude`() {
    val direction = FlingDirection(Velocity(x = Float.NaN, y = 400f))

    assertTrue(direction.magnitude.isNaN(), "битую скорость обязан отбить вызывающий, а не сплайн")
  }
}
