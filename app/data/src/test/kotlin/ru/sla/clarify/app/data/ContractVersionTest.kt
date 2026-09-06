package ru.sla.clarify.app.data

import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.sla.clarify.app.data.entity.ServerException

/**
 * Сторож единственного шва между двумя репозиториями: версия копии спеки уходит на сервер сама,
 * без участия вызывающего, а названный сервером отказ возвращается отдельной ошибкой — не теряется
 * среди прочих и не приходит ошибкой разбора.
 *
 * Имя заголовка проверяется буквой, а значение — константой: имя есть договорённость с сервером и
 * ломаться при переименовании обязано, а значение меняется с каждым обновлением копии спеки.
 *
 * Совпадение константы с `info.version` копии здесь не проверяется: копии в репозитории ещё нет,
 * сверять не с чем. Тест заводится вместе с ней.
 */
class ContractVersionTest {

  @Test
  fun `a major mismatch surfaces as a named failure, not a decode error`() = runTest {
    val client = clientOf(
      tokens = null,
      handler = {
        respond(
          """{"code":"contractVersionMismatch","message":"server speaks 2.x"}""",
          HttpStatusCode.UpgradeRequired,
          jsonHeaders()
        )
      }
    )

    val error = assertThrows<ServerException.ContractMismatch> { client.get("/user") }
    assertEquals(CONTRACT_VERSION, error.clientVersion)
    assertEquals("server speaks 2.x", error.serverMessage)
  }

  @Test
  fun `a mismatch keeps its name whatever status the server chose`() = runTest {
    val client = clientOf(
      tokens = null,
      handler = {
        respond(
          """{"code":"contractVersionMismatch","message":"server speaks 2.x"}""",
          HttpStatusCode.BadRequest,
          jsonHeaders()
        )
      }
    )

    // Статус отказа сервер выбирает сам, и различать расхождение версий по нему нельзя: код
    // называет его однозначно, а статус завтра окажется другим
    assertThrows<ServerException.ContractMismatch> { client.get("/user") }
  }
}
