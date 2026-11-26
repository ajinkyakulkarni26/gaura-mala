package com.gauramala.wear

import android.Manifest
import android.app.Notification
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

/** Keeps Auto Count reachable via a Foreground Service + Ongoing Activity until stopped. */
internal class AutoCountOngoingActivity(context: Context) {
    private val appContext = context.applicationContext
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

        if (!isActive) {
            stop()
            return
        }

        val notification = buildNotification(
            context = appContext,
            isRunning = isRunning,
            beadCount = beadCount,
            completedRounds = completedRounds
        )
        AutoCountForegroundService.startOrUpdate(
            context = appContext,
            isRunning = isRunning,
            beadCount = beadCount,
            completedRounds = completedRounds,
            notification = notification
        )
    }

    fun stop() {
        AutoCountForegroundService.stop(appContext)
        lastSessionState = SessionSnapshot(
            isActive = false,
            isRunning = false,
            beadCount = 0,
            completedRounds = 0,
            notificationsAllowed = notificationsEnabled(appContext)
        )
    }

    companion object {
        const val CHANNEL_ID = "auto_count_session"
        const val NOTIFICATION_ID = 108
        const val LOCUS_ID = "gaura_mala_auto_count"

        private const val REQUEST_PAUSE_OR_RESUME = 301
        private const val REQUEST_STOP = 302

        fun notificationsEnabled(context: Context): Boolean {
            val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        internal fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val appContext = context.applicationContext
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

        internal fun buildNotification(
            context: Context,
            isRunning: Boolean,
            beadCount: Int,
            completedRounds: Int
        ): Notification {
            val appContext = context.applicationContext
            createNotificationChannel(appContext)

            val returnIntent = PendingIntent.getActivity(
                appContext,
                NOTIFICATION_ID,
                Intent(appContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val toggleIntent = PendingIntent.getService(
                appContext,
                REQUEST_PAUSE_OR_RESUME,
                Intent(appContext, AutoCountForegroundService::class.java).apply {
                    action = if (isRunning) {
                        AutoCountForegroundService.ACTION_PAUSE
                    } else {
                        AutoCountForegroundService.ACTION_RESUME
                    }
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val stopIntent = PendingIntent.getService(
                appContext,
                REQUEST_STOP,
                Intent(appContext, AutoCountForegroundService::class.java).apply {
                    action = AutoCountForegroundService.ACTION_STOP
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
            val notificationBuilder = NotificationCompat.Builder(appContext, CHANNEL_ID)
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
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .addAction(
                    if (isRunning) R.drawable.ic_notification_pause else R.drawable.ic_notification_play,
                    appContext.getString(
                        if (isRunning) R.string.auto_count_action_pause else R.string.auto_count_action_resume
                    ),
                    toggleIntent
                )
                .addAction(
                    R.drawable.ic_notification_stop,
                    appContext.getString(R.string.auto_count_action_stop),
                    stopIntent
                )

            val ongoingStatus = Status.Builder()
                .addTemplate("#status#")
                .addPart("status", Status.TextPart(statusText))
                .build()

            val ongoingBuilder = OngoingActivity.Builder(appContext, NOTIFICATION_ID, notificationBuilder)
                .setOngoingActivityId(NOTIFICATION_ID)
                .setStaticIcon(R.drawable.ic_auto_count_ongoing)
                .setTitle(appContext.getString(R.string.auto_count_notification_title))
                .setStatus(ongoingStatus)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setLocusId(locusId)
                .setTouchIntent(returnIntent)
            if (isRunning) {
                ongoingBuilder.setAnimatedIcon(R.drawable.ic_auto_count_animated)
            }
            ongoingBuilder.build().apply(appContext)

            return notificationBuilder.build()
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
