package ru.sla.clarify.chat

import android.util.Base64
import org.json.JSONObject
import java.util.zip.Deflater
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object UserSigGenerator {
  private const val TLS_VERSION = "2.0"
  private const val EXPIRE_TIME_SECONDS = 604_800L
  private const val HMAC_SHA_256 = "HmacSHA256"

  fun generate(
    userId: String,
    expireTimeSeconds: Long = EXPIRE_TIME_SECONDS
  ): String {
    val currentTimeSeconds = System.currentTimeMillis() / 1000
    val signature = hmacSha256(
      userId = userId,
      currentTimeSeconds = currentTimeSeconds,
      expireTimeSeconds = expireTimeSeconds
    )
    val payload = JSONObject()
      .put("TLS.ver", TLS_VERSION)
      .put("TLS.identifier", userId)
      .put("TLS.sdkappid", ChatManagerImpl.SDK_APP_ID)
      .put("TLS.expire", expireTimeSeconds)
      .put("TLS.time", currentTimeSeconds)
      .put("TLS.sig", signature)
      .toString()

    return deflate(payload.encodeToByteArray()).toTencentBase64Url()
  }

  private fun hmacSha256(
    userId: String,
    currentTimeSeconds: Long,
    expireTimeSeconds: Long
  ): String {
    val contentToBeSigned = buildString {
      append("TLS.identifier:$userId\n")
      append("TLS.sdkappid:${ChatManagerImpl.SDK_APP_ID}\n")
      append("TLS.time:$currentTimeSeconds\n")
      append("TLS.expire:$expireTimeSeconds\n")
    }
    val mac = Mac.getInstance(HMAC_SHA_256).apply {
      init(SecretKeySpec(SECRET_KEY.encodeToByteArray(), HMAC_SHA_256))
    }
    return Base64.encodeToString(
      mac.doFinal(contentToBeSigned.encodeToByteArray()),
      Base64.NO_WRAP
    )
  }

  private fun deflate(input: ByteArray): ByteArray {
    val deflater = Deflater()
    return try {
      deflater.setInput(input)
      deflater.finish()

      val buffer = ByteArray(2048)
      val length = deflater.deflate(buffer)
      buffer.copyOfRange(0, length)
    } finally {
      deflater.end()
    }
  }

  private fun ByteArray.toTencentBase64Url(): String {
    return Base64.encodeToString(this, Base64.NO_WRAP)
      .replace('+', '*')
      .replace('/', '-')
      .replace('=', '_')
  }
}

private const val SECRET_KEY = "a77696afa8ec27da73ef65bf493ebaa2aa38257833f876864754bc8bac787992"
