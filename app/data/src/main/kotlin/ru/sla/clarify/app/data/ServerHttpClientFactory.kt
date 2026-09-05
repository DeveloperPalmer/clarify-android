package ru.sla.clarify.app.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.HttpClientCall
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.Sender
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerializationException
import me.tatarka.inject.annotations.Inject
import ru.kode.pathfinder.PathFinder
import ru.sla.clarify.app.data.entity.ServerErrorBody
import ru.sla.clarify.app.data.entity.ServerException
import ru.sla.clarify.app.data.mapper.toServerException
import ru.sla.clarify.auth.session.domain.AuthSessionRepository
import ru.sla.clarify.auth.session.domain.entity.AccessToken
import ru.sla.clarify.auth.session.domain.entity.AuthTokens
import ru.sla.clarify.auth.session.domain.entity.RefreshRejectedException
import ru.sla.clarify.core.domain.di.scope.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import java.io.IOException

/**
 * Собирает единственный на приложение [HttpClient]: адрес, таймауты, разбор JSON, токен сессии
 * и разворот не-2xx в [ServerException].
 *
 * Токен и разбор отказа сидят в одном перехватчике [HttpSend], а не расставлены по плагинам,
 * потому что второй заход со свежим токеном и превращение ответа в ошибку обязаны идти именно в
 * этом порядке. Разложенные по разным плагинам, они зависели бы от порядка установки — а он в
 * конфигурации не виден и молча меняется от добавления соседнего плагина.
 */
@SingleIn(AppScope::class)
@Inject
class ServerHttpClientFactory(
  private val pathFinder: PathFinder,
  private val authSessionRepository: AuthSessionRepository
) {

  fun create(): HttpClient {
    return withSessionSending(HttpClient(CIO) { configureServer(pathFinder) })
  }

  /**
   * Тот же клиент с подменённым движком. Нужен тесту, чтобы тот проверял боевую сборку целиком,
   * а не собранную заново копию, в которой перехватчик легко забыть.
   */
  internal fun create(engine: HttpClientEngine): HttpClient {
    return withSessionSending(HttpClient(engine) { configureServer(pathFinder) })
  }

  private fun withSessionSending(client: HttpClient): HttpClient {
    client.plugin(HttpSend).intercept { request -> sendAuthorized(request) }
    return client
  }

  private suspend fun Sender.sendAuthorized(request: HttpRequestBuilder): HttpClientCall {
    val token = readAccessToken()
    if (token != null) {
      request.setBearer(token)
    }

    var call = executeReportingFailure(request)
    if (call.response.status == HttpStatusCode.Unauthorized && token != null) {
      val fresh = refreshOrGiveUp(token)
      request.setBearer(fresh.accessToken)
      call = executeReportingFailure(request)
    }
    if (!call.response.status.isAccepted()) {
      throw readErrorBody(call.response).toServerException(call.response.status)
    }
    return call
  }

  private suspend fun readAccessToken(): AccessToken? {
    val key = authSessionRepository.readKey() ?: return null
    return authSessionRepository.readTokens(key)?.accessToken
  }

  /**
   * Отказ сессии приходит сюда её собственным исключением и здесь же переводится в термины
   * транспорта: вызывающий обещанного [ServerException] и ждёт, а про `auth-session` он не знает.
   */
  private suspend fun refreshOrGiveUp(stale: AccessToken): AuthTokens {
    return try {
      authSessionRepository.refresh(stale) ?: throw ServerException.Unauthorized(cause = null)
    } catch (e: RefreshRejectedException) {
      throw ServerException.Unauthorized(e)
    }
  }
}

private fun HttpClientConfig<*>.configureServer(pathFinder: PathFinder) {
  // Блок defaultRequest выполняется на каждом запросе, а не один раз при сборке клиента, поэтому
  // адрес читается заново: переключение стенда подхватывается без пересборки. Прочитанный при
  // создании, он замер бы до перезапуска приложения.
  defaultRequest {
    url(pathFinder.currentEnvironment.baseUrl)
    // Версия копии спеки уходит с каждой командой, а не только первым кадром канала: /auth/google
    // и догон отправляются раньше, чем канал открыт, и на устаревшей копии клиент упёрся бы там в
    // ошибку разбора ответа вместо понятного отказа. Рукопожатие канала идёт этим же клиентом,
    // поэтому заголовок оказывается и на нём. Имя заголовка — наше допущение: сверить с серверной
    // командой вместе с формой кадров канала, потом менять его будет уже нельзя
    header("X-Contract-Version", CONTRACT_VERSION)
  }
  install(ContentNegotiation) { json(ServerJson) }
  install(HttpTimeout) {
    // Команда, не уложившаяся в 30 секунд, уже не нужна тому, кто её отправил
    requestTimeoutMillis = 30_000
    connectTimeoutMillis = 15_000
    socketTimeoutMillis = 30_000
  }
}

/**
 * Тело ошибки, если оно вообще читается. Неразобранное тело — не повод потерять статус: иначе
 * `502` от балансировщика вышел бы наружу ошибкой разбора и увёл бы поиск в клиент.
 *
 * Обычная функция, а не расширение на [HttpResponse]: у Ktor ответ является `CoroutineScope`,
 * и suspend-расширение на нём даёт приёмник, в который можно случайно запустить корутину.
 */
private suspend fun readErrorBody(response: HttpResponse): ServerErrorBody? {
  return try {
    ServerJson.decodeFromString(ServerErrorBody.serializer(), response.bodyAsText())
  } catch (_: SerializationException) {
    null
  } catch (_: IllegalArgumentException) {
    null
  }
}

/**
 * Рукопожатие WebSocket отвечает `101 Switching Protocols`, и `isSuccess()` его не пропускает:
 * успехом считается только 2xx. Без этой оговорки канал разворачивался бы в [ServerException.Api]
 * на каждом подключении — а ради общего с командами клиента он через этот перехватчик и идёт.
 */
private fun HttpStatusCode.isAccepted(): Boolean {
  return isSuccess() || this == HttpStatusCode.SwitchingProtocols
}

private fun HttpRequestBuilder.setBearer(token: AccessToken) {
  headers.remove(HttpHeaders.Authorization)
  headers.append(HttpHeaders.Authorization, "Bearer ${token.value}")
}

/**
 * Отказ движка — оборванная сеть, а не ответ сервера, — и наружу он обязан выйти
 * [ServerException.Unreachable]: иначе вызывающий станет ловить исключения Ktor и тем самым
 * узнает про транспорт, который мы от него и прячем.
 */
private suspend fun Sender.executeReportingFailure(request: HttpRequestBuilder): HttpClientCall {
  return try {
    execute(request)
  } catch (e: IOException) {
    throw ServerException.Unreachable(e)
  }
}
