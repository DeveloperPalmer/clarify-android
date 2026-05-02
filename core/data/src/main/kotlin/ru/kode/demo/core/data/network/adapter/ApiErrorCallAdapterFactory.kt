package ru.kode.demo.core.data.network.adapter

import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Adapts retrofit functions and maps :
 */
class ApiErrorCallAdapterFactory(
  /**
   * Converts an error response body to a standard application error format
   */
  private val errorResponseBodyConverter: (Response<*>) -> Throwable?,
  /**
   * A mapper for non-api errors which contain no standardized error body, for example these are
   * http transport errors, network connectivity errors, etc
   */
  private val nonApiErrorConverter: (Throwable) -> Throwable
) : CallAdapter.Factory() {
  override fun get(returnType: Type, annotations: Array<out Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
    if (getRawType(returnType) != Call::class.java ||
      returnType !is ParameterizedType ||
      returnType.actualTypeArguments.size != 1
    ) {
      return null
    }
    val delegate = retrofit.nextCallAdapter(this, returnType, annotations)
      ?: error("no call adapter for $returnType")
    @Suppress("UNCHECKED_CAST")
    return ErrorsCallAdapter(
      delegateAdapter = delegate as CallAdapter<Any, Call<*>>,
      errorResponseBodyConverter = errorResponseBodyConverter,
      nonApiErrorConverter = nonApiErrorConverter
    )
  }
}

private class ErrorsCallAdapter(
  private val delegateAdapter: CallAdapter<Any, Call<*>>,
  /**
   * Converts an error response body to a standard application error format
   */
  private val errorResponseBodyConverter: (Response<*>) -> Throwable?,
  /**
   * A mapper for non-api errors which contain no standardized error body, for example these are
   * http transport errors, network connectivity errors, etc
   */
  private val nonApiErrorConverter: (Throwable) -> Throwable
) : CallAdapter<Any, Call<*>> by delegateAdapter {

  override fun adapt(call: Call<Any>): Call<*> {
    return delegateAdapter.adapt(CallWithErrorHandling(call, errorResponseBodyConverter, nonApiErrorConverter))
  }
}

class CallWithErrorHandling(
  private val delegate: Call<Any>,
  /**
   * Converts an error response body to a standard application error format
   */
  private val errorResponseBodyConverter: (Response<*>) -> Throwable?,
  /**
   * A mapper for non-api errors which contain no standardized error body, for example these are
   * http transport errors, network connectivity errors, etc
   */
  private val nonApiErrorConverter: (Throwable) -> Throwable
) : Call<Any> by delegate {

  override fun enqueue(callback: Callback<Any>) {
    delegate.enqueue(object : Callback<Any> {
      override fun onResponse(call: Call<Any>, response: Response<Any>) {
        if (response.isSuccessful) {
          callback.onResponse(call, response)
        } else {
          val exception = errorResponseBodyConverter(response) ?: HttpException(response)
          callback.onFailure(call, exception)
        }
      }

      override fun onFailure(call: Call<Any>, t: Throwable) {
        callback.onFailure(call, nonApiErrorConverter(t))
      }
    })
  }

  override fun clone() = CallWithErrorHandling(delegate.clone(), errorResponseBodyConverter, nonApiErrorConverter)
}
