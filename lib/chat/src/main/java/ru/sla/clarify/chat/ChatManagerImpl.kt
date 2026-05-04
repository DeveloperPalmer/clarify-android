package ru.sla.clarify.chat

import android.content.Context
import com.squareup.anvil.annotations.ContributesBinding
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMLogListener
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMSDKConfig
import com.tencent.imsdk.v2.V2TIMSDKConfig.V2TIM_LOG_DEBUG
import com.tencent.imsdk.v2.V2TIMSDKListener
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.chat.entity.ChatLoginException
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.ApplicationContext
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.log.log
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ChatManagerImpl @Inject constructor(
  @ApplicationContext
  private val context: Context,
  private val authSessionPersistence: AuthSessionPersistence
) : ChatManager {

  companion object {
    const val SDK_APP_ID = 20039812
  }

  private val chatSdkListener = object : V2TIMSDKListener() {
    // TODO: @sla Chat. Handle sdk event logic
  }

  private val chatLogListener = object : V2TIMLogListener() {
    // TODO: @sla Chat. Add chat log logic
  }

  override fun initialize() {
    V2TIMManager.getInstance().addIMSDKListener(
      /* listener */
      chatSdkListener
    )
    V2TIMManager.getInstance().initSDK(
      /* context */
      context,
      /* sdkAppID */
      SDK_APP_ID,
      /* config */
      V2TIMSDKConfig().apply {
        logLevel = V2TIM_LOG_DEBUG
        logListener = chatLogListener
      }
    )
  }

  override suspend fun signIn(chatSignature: String) {
    val userId = requireNotNull(authSessionPersistence.withKey { readUserId(it) }) {
      "userId not found in cache"
    }
    return suspendCancellableCoroutine { cont ->
      val callback = object : V2TIMCallback {
        override fun onSuccess() {
          log { "Chat: chat sign in successfully" }
          cont.resume(Unit)
        }
        override fun onError(code: Int, description: String?) {
          cont.resumeWithException(ChatLoginException(description))
        }
      }
      V2TIMManager.getInstance().login(
        /* userId */
        "${userId.value}",
        /* chatSignature */
        chatSignature,
        /* listener */
        callback
      )
    }
  }

  override suspend fun signOut() {
    return suspendCancellableCoroutine { cont ->
      val callback = object : V2TIMCallback {
        override fun onSuccess() {
          log { "Chat: chat sign out successfully" }
          cont.resume(Unit)
        }
        override fun onError(code: Int, description: String?) {
          cont.resumeWithException(ChatLoginException(description))
        }
      }
      V2TIMManager.getInstance().logout(
        /* listener */
        callback
      )
    }
  }
}
