package ru.sla.clarify.core.data.network.errorconverter

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import retrofit2.Response
import ru.sla.clarify.core.data.network.entity.ApiErrorBody
import ru.sla.clarify.core.domain.entity.ApiError
import ru.sla.clarify.core.domain.logError
import ru.sla.log.asLog
import java.io.IOException

class ErrorResponseBodyConverter(
  private val json: Json
) : Function1<Response<*>, Throwable?> {
  override fun invoke(response: Response<*>): Throwable? {
    return convertToApiErrorModel(response)?.let {
      ApiError(
        code = it.code,
        description = it.message
      )
    }
  }

  @OptIn(ExperimentalSerializationApi::class)
  private fun convertToApiErrorModel(response: Response<*>): ApiErrorBody? {
    return if (response.errorBody()?.hasJsonType == true) {
      try {
        response.errorBody()?.let {
          json.decodeFromStream(it.source().inputStream())
        }
      } catch (e: IOException) {
        logError { e.asLog("failed to convert error to a predefined error format") }
        null
      } catch (e: SerializationException) {
        logError { e.asLog("failed to convert error to a predefined error format") }
        null
      } catch (e: IllegalArgumentException) {
        logError { e.asLog("failed to convert error to a predefined error format") }
        null
      }
    } else {
      null
    }
  }
}

private val ResponseBody.hasJsonType: Boolean
  get() {
    // TODO should simply use "return this.contentType() == MEDIA_TYPE_JSON"
    //   but this is impossible due to https://github.com/square/okhttp/issues/6096.
    //   Simplify to the above code when this bug is fixed
    return (
      this.contentType()?.type == MEDIA_TYPE_JSON.type &&
        this.contentType()?.subtype == MEDIA_TYPE_JSON.subtype
      ).also { success ->
      if (!success) {
        logError {
          "expected content type of \"$MEDIA_TYPE_JSON\" " +
            "but was: \"${contentType()?.toString().orEmpty()}\""
        }
      }
    }
  }

private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()
