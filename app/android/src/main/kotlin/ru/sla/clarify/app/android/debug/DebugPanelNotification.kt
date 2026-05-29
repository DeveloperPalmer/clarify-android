package ru.sla.clarify.app.android.debug

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.sla.clarify.core.resources.R

object DebugPanelNotification {
  const val ACTION_OPEN_DEBUG_PANEL = "ru.sla.clarify.app.android.debug.OPEN_DEBUG_PANEL"

  private const val CHANNEL_ID = "clarify_debug_panel"
  private const val NOTIFICATION_ID = 3719

  // POST_NOTIFICATIONS запрашивается в MainActivity до вызова install (API 33+)
  @SuppressLint("MissingPermission")
  fun install(context: Context) {
    val manager = NotificationManagerCompat.from(context)
    manager.createNotificationChannel(
      NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
        .setName(context.getString(R.string.debug_panel_notification_channel_name))
        .build()
    )
    val intent = Intent(ACTION_OPEN_DEBUG_PANEL).setPackage(context.packageName)
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      0,
      intent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_debug_24)
      .setContentTitle(context.getString(R.string.debug_panel_notification_title))
      .setContentText(context.getString(R.string.debug_panel_notification_text))
      .setContentIntent(pendingIntent)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setOngoing(true)
      .setAutoCancel(false)
      .build()
    manager.notify(NOTIFICATION_ID, notification)
  }

  fun remove(context: Context) {
    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
  }
}
