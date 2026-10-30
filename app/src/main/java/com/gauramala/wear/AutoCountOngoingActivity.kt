package com.gauramala.wear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ongoing.OngoingActivity

/** Keeps Auto Count reachable while running and leaves a paused return notification until stopped. */
internal class AutoCountOngoingActivity(context: Context) {
    private val appContext = context.applicationContext
    private val notifications = NotificationManagerCompat.from(appContext)
    private var lastSessionState: Pair<Boolean, Boolean>? = null

    fun update(isActive: Boolean, isRunning: Boolean) {
        val sessionState = isActive to isRunning
        if (sessionState == lastSessionState) return
        lastSessionState = sessionState

        if (!isActive || !notifications.areNotificationsEnabled()) {
            stop()
            return
        }

        createNotificationChannel()
        val returnIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_ID,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_auto_count_ongoing)
            .setContentTitle(appContext.getString(R.string.auto_count_notification_title))
            .setContentText(
                appContext.getString(
                    if (isRunning) R.string.auto_count_notification_running
                    else R.string.auto_count_notification_paused
                )
            )
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(returnIntent)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(true)

        OngoingActivity.Builder(appContext, NOTIFICATION_ID, notification)
            .setStaticIcon(R.drawable.ic_auto_count_ongoing)
            .setTouchIntent(returnIntent)
            .setTitle(appContext.getString(R.string.auto_count_notification_title))
            .build()
            .apply(appContext)

        runCatching { notifications.notify(NOTIFICATION_ID, notification.build()) }
    }

    fun stop() {
        notifications.cancel(NOTIFICATION_ID)
        lastSessionState = false to false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.auto_count_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = appContext.getString(R.string.auto_count_notification_running)
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    companion object {
        const val CHANNEL_ID = "auto_count_session"
        const val NOTIFICATION_ID = 108

        fun notificationsEnabled(context: Context): Boolean =
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
