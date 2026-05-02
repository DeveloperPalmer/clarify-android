package ru.kode.demo.core.data.network.errorconverter

import ru.kode.demo.core.domain.entity.ApiError
import ru.kode.demo.core.domain.entity.ConnectivityError
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Maps exceptions which are not convertible to [ApiError]
 */
class NonApiErrorConverter : Function1<Throwable, Throwable> {
  override fun invoke(error: Throwable): Throwable {
    return when (error) {
      is SSLException -> ConnectivityError.SSLError(cause = error)
      is ConnectException -> ConnectivityError.NoConnection(cause = error)
      is UnknownHostException -> ConnectivityError.NoConnection(cause = error)
      is SocketTimeoutException -> ConnectivityError.TimeOut(cause = error)
      else -> error
    }
  }
}
