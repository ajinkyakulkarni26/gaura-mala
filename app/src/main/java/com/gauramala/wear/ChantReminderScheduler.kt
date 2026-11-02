package com.gauramala.wear

import android.Manifest
import android.app.AlarmManager
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
import com.gauramala.wear.presentation.ChantReminderContent
import com.gauramala.wear.presentation.ChantReminderPolicy
import com.gauramala.wear.presentation.MantraUiState
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules and displays gentle local chanting reminders when the user has started their rounds
 * today and paused for 2.5 hours without yet reaching their daily goal.
 */
internal class ChantReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val notifications = NotificationManagerCompat.from(appContext)
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun canPostNotifications(): Boolean {
        val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        return runtimeGranted && notifications.areNotificationsEnabled()
    }

    fun sync(state: MantraUiState, now: LocalDateTime = LocalDateTime.now()) {
        dismissNotification()
        if (!ChantReminderPolicy.isEligibleForReminder(state) || !canPostNotifications()) {
            cancelScheduledReminder()
            return
        }
        scheduleNext(now)
    }

    fun scheduleNext(now: LocalDateTime = LocalDateTime.now()) {
        if (!canPostNotifications()) {
            cancelScheduledReminder()
            return
        }
        val nextTime = ChantReminderPolicy.nextReminderTime(now)
        if (nextTime == null) {
            cancelScheduledReminder()
            return
        }
        val triggerAtMillis = nextTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        alarmManager?.setWindow(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            ALARM_WINDOW_MILLIS,
            showReminderPendingIntent()
        )
    }

    fun cancelAll() {
        cancelScheduledReminder()
        dismissNotification()
    }

    fun cancelScheduledReminder() {
        alarmManager?.cancel(showReminderPendingIntent())
    }

    fun dismissNotification() {
        notifications.cancel(NOTIFICATION_ID)
    }

    fun showNotification(content: ChantReminderContent) {
        if (!canPostNotifications()) return
        createNotificationChannel()

        val openAppIntent = PendingIntent.getActivity(
            appContext,
            REQUEST_OPEN_APP,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val logRoundsIntent = PendingIntent.getActivity(
            appContext,
            REQUEST_LOG_ROUNDS,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_ROUND_PICKER, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markGoalDoneIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_MARK_GOAL_DONE,
            Intent(appContext, ChantReminderReceiver::class.java).apply {
                action = ACTION_MARK_GOAL_DONE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_auto_count_ongoing)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_auto_count_ongoing,
                appContext.getString(R.string.chant_reminder_action_goal_done),
                markGoalDoneIntent
            )
            .addAction(
                R.drawable.ic_auto_count_ongoing,
                appContext.getString(R.string.chant_reminder_action_log_rounds),
                logRoundsIntent
            )
            .build()

        runCatching { notifications.notify(NOTIFICATION_ID, notification) }
    }

    private fun showReminderPendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            REQUEST_SHOW_REMINDER,
            Intent(appContext, ChantReminderReceiver::class.java).apply {
                action = ACTION_SHOW_REMINDER
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.chant_reminder_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = appContext.getString(R.string.chant_reminder_notification_description)
            }
        )
    }

    companion object {
        const val CHANNEL_ID = "chant_reminders"
        const val NOTIFICATION_ID = 109
        const val ACTION_SHOW_REMINDER = "com.gauramala.wear.action.SHOW_CHANT_REMINDER"
        const val ACTION_MARK_GOAL_DONE = "com.gauramala.wear.action.MARK_GOAL_DONE"
        const val EXTRA_OPEN_ROUND_PICKER = "com.gauramala.wear.extra.OPEN_ROUND_PICKER"

        private const val REQUEST_SHOW_REMINDER = 201
        private const val REQUEST_OPEN_APP = 202
        private const val REQUEST_MARK_GOAL_DONE = 203
        private const val REQUEST_LOG_ROUNDS = 204
        private const val ALARM_WINDOW_MILLIS = 15 * 60 * 1_000L
    }
}

