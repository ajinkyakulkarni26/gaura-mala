package com.gauramala.wear

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.LocusIdCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status

/** Keeps Auto Count reachable while running and leaves a paused return notification until stopped. */
internal class AutoCountOngoingActivity(context: Context) {
    private val appContext = context.applicationContext
    private val notifications = NotificationManagerCompat.from(appContext)
    private var lastSessionState: SessionSnapshot? = null

    fun update(
        isActive: Boolean,
        isRunning: Boolean,
        beadCount: Int = 0,
        completedRounds: Int = 0
    ) {
        val notificationsAllowed = notificationsEnabled(appContext)
        val sessionState = SessionSnapshot(
            isActive = isActive,
            isRunning = isRunning,
            beadCount = beadCount,
            completedRounds = completedRounds,
            notificationsAllowed = notificationsAllowed
        )
        if (sessionState == lastSessionState) return
        lastSessionState = sessionState

        if (!isActive || !notificationsAllowed) {
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
        val statusText = if (isRunning) {
            appContext.getString(
                R.string.auto_count_notification_running_status,
                completedRounds + 1,
                beadCount
            )
        } else {
            appContext.getString(
                R.string.auto_count_notification_paused_status,
                beadCount
            )
        }
        val locusId = LocusIdCompat(LOCUS_ID)
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_auto_count_ongoing)
            .setContentTitle(appContext.getString(R.string.auto_count_notification_title))
            .setContentText(statusText)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setLocusId(locusId)
            .setContentIntent(returnIntent)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(true)

        val ongoingStatus = Status.Builder()
            .addTemplate("#status#")
            .addPart("status", Status.TextPart(statusText))
            .build()

        OngoingActivity.Builder(appContext, NOTIFICATION_ID, notification)
            .setAnimatedIcon(R.drawable.ic_auto_count_ongoing)
            .setStaticIcon(R.drawable.ic_auto_count_ongoing)
            .setTitle(appContext.getString(R.string.auto_count_notification_title))
            .setStatus(ongoingStatus)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setLocusId(locusId)
            .setTouchIntent(returnIntent)
            .build()
            .apply(appContext)

        runCatching { notifications.notify(NOTIFICATION_ID, notification.build()) }
    }

    fun stop() {
        notifications.cancel(NOTIFICATION_ID)
        lastSessionState = SessionSnapshot(
            isActive = false,
            isRunning = false,
            beadCount = 0,
            completedRounds = 0,
            notificationsAllowed = notificationsEnabled(appContext)
        )
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
        const val LOCUS_ID = "gaura_mala_auto_count"

        fun notificationsEnabled(context: Context): Boolean {
            val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private data class SessionSnapshot(
        val isActive: Boolean,
        val isRunning: Boolean,
        val beadCount: Int,
        val completedRounds: Int,
        val notificationsAllowed: Boolean
    )
}
