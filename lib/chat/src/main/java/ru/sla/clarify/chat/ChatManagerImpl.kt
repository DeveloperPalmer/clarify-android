package ru.sla.clarify.chat

import android.content.Context
import com.squareup.anvil.annotations.ContributesBinding
import com.tencent.imsdk.v2.V2TIMLogListener
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMSDKConfig
import com.tencent.imsdk.v2.V2TIMSDKConfig.V2TIM_LOG_DEBUG
import com.tencent.imsdk.v2.V2TIMSDKListener
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.ApplicationContext
import ru.sla.clarify.core.domain.di.scope.SingleIn
import javax.inject.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ChatManagerImpl @Inject constructor(
  @ApplicationContext
  private val context: Context
) : ChatManager {

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
}

// TODO: @sla Chat. Add secure storage logic
@Suppress("UnderscoresInNumericLiterals")
private const val SDK_APP_ID = 20039812
