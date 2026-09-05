package ru.sla.clarify.app.data.mapper

import io.ktor.http.HttpStatusCode
import ru.sla.clarify.app.data.CONTRACT_VERSION
import ru.sla.clarify.app.data.entity.ServerErrorBody
import ru.sla.clarify.app.data.entity.ServerException

/**
 * Разворачивает не-2xx в типизированную ошибку.
 *
 * Маппер чистый: тело уже прочитано вызывающим. Так он проверяется без клиента и без движка —
 * и, что важнее, не получает в приёмники `HttpResponse`, который у Ktor является `CoroutineScope`.
 *
 * @param status статус ответа: в самом теле его нет, а различать по нему приходится.
 */
internal fun ServerErrorBody?.toServerException(status: HttpStatusCode): ServerException {
  // Расхождение версий разбирается раньше статуса и независимо от него: код называет сам сервер,
  // а статус он волен выбрать любой. Код — наше допущение до первой версии спеки
  if (this?.code == "contract_version_mismatch") {
    return ServerException.ContractMismatch(CONTRACT_VERSION, message)
  }
  if (status == HttpStatusCode.Unauthorized) {
    return ServerException.Unauthorized(cause = null)
  }
  return ServerException.ApiError(
    status = status.value,
    code = this?.code,
    serverMessage = this?.message
  )
}
