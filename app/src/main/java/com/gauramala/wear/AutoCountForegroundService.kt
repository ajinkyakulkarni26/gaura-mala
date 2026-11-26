package com.gauramala.wear

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate

/**
 * Foreground service backing active Auto Count sessions so Wear OS surfaces the Ongoing Activity
 * indicator on the watch face and Recent Apps launcher chip while keeping timed haptics and
 * optional local chant playback alive in the background.
 */
class AutoCountForegroundService : Service() {

    private val rolloverReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val viewModel = AutoCountSessionStore.peek() ?: return
            val recordedDate = viewModel.uiState.value.lastRecordedDate
            if (recordedDate.isNotEmpty() && recordedDate != LocalDate.now().toString()) {
                viewModel.refreshDailySession()
                if (!viewModel.uiState.value.autoCountActive) {
                    stopForegroundAndSelf()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        activeService = this
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(
            this,
            rolloverReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        activeService = this
        when (intent?.action) {
            ACTION_PAUSE -> {
                AutoCountSessionStore.getOrCreate(application).pauseAutoCount()
                return START_NOT_STICKY
            }
            ACTION_RESUME -> {
                AutoCountSessionStore.getOrCreate(application).resumeAutoCount()
                return START_NOT_STICKY
            }
            ACTION_STOP -> {
                AutoCountSessionStore.getOrCreate(application).stopAutoCount()
                stopForegroundAndSelf()
                return START_NOT_STICKY
            }
        }

        val state = AutoCountSessionStore.peek()?.uiState?.value
        val isActive = intent?.getBooleanExtra(EXTRA_IS_ACTIVE, state?.autoCountActive ?: false) ?: false
        if (!isActive) {
            val fallbackNotification = AutoCountOngoingActivity.buildNotification(
                context = applicationContext,
                isRunning = false,
                beadCount = 0,
                completedRounds = 0
            )
            promoteToForeground(fallbackNotification)
            stopForegroundAndSelf()
            return START_NOT_STICKY
        }

        val isRunning = intent.getBooleanExtra(EXTRA_IS_RUNNING, state?.autoCountRunning ?: false)
        val beadCount = intent.getIntExtra(EXTRA_BEAD_COUNT, state?.beadCount ?: 0)
        val completedRounds = intent.getIntExtra(EXTRA_COMPLETED_ROUNDS, state?.completedRounds ?: 0)
        val notification = AutoCountOngoingActivity.buildNotification(
            context = applicationContext,
            isRunning = isRunning,
            beadCount = beadCount,
            completedRounds = completedRounds
        )
        promoteToForeground(notification)
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        AutoCountSessionStore.peek()?.pauseAutoCount()
        stopForegroundAndSelf()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(rolloverReceiver) }
        if (activeService === this) {
            activeService = null
        }
        super.onDestroy()
    }

    internal fun applyForegroundNotification(notification: Notification) {
        promoteToForeground(notification)
    }

    internal fun stopForegroundAndSelf() {
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        NotificationManagerCompat.from(applicationContext).cancel(AutoCountOngoingActivity.NOTIFICATION_ID)
        stopSelf()
    }

    private fun promoteToForeground(notification: Notification) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    AutoCountOngoingActivity.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(AutoCountOngoingActivity.NOTIFICATION_ID, notification)
            }
        }
    }

    companion object {
        const val ACTION_START_OR_UPDATE = "com.gauramala.wear.action.START_OR_UPDATE_AUTO_COUNT"
        const val ACTION_PAUSE = "com.gauramala.wear.action.PAUSE_AUTO_COUNT"
        const val ACTION_RESUME = "com.gauramala.wear.action.RESUME_AUTO_COUNT"
        const val ACTION_STOP = "com.gauramala.wear.action.STOP_AUTO_COUNT"

        private const val EXTRA_IS_ACTIVE = "extra_is_active"
        private const val EXTRA_IS_RUNNING = "extra_is_running"
        private const val EXTRA_BEAD_COUNT = "extra_bead_count"
        private const val EXTRA_COMPLETED_ROUNDS = "extra_completed_rounds"

        @Volatile
        private var activeService: AutoCountForegroundService? = null

        internal fun startOrUpdate(
            context: Context,
            isRunning: Boolean,
            beadCount: Int,
            completedRounds: Int,
            notification: Notification
        ) {
            val appContext = context.applicationContext
            val running = activeService
            if (running != null) {
                running.applyForegroundNotification(notification)
                return
            }

            val serviceIntent = Intent(appContext, AutoCountForegroundService::class.java).apply {
                action = ACTION_START_OR_UPDATE
                putExtra(EXTRA_IS_ACTIVE, true)
                putExtra(EXTRA_IS_RUNNING, isRunning)
                putExtra(EXTRA_BEAD_COUNT, beadCount)
                putExtra(EXTRA_COMPLETED_ROUNDS, completedRounds)
            }
            val started = runCatching {
                ContextCompat.startForegroundService(appContext, serviceIntent)
                true
            }.getOrDefault(false)
            if (started || AutoCountOngoingActivity.notificationsEnabled(appContext)) {
                runCatching {
                    NotificationManagerCompat.from(appContext)
                        .notify(AutoCountOngoingActivity.NOTIFICATION_ID, notification)
                }
            }
        }

        internal fun stop(context: Context) {
            val appContext = context.applicationContext
            val running = activeService
            if (running != null) {
                running.stopForegroundAndSelf()
            } else {
                runCatching {
                    appContext.stopService(Intent(appContext, AutoCountForegroundService::class.java))
                }
                NotificationManagerCompat.from(appContext).cancel(AutoCountOngoingActivity.NOTIFICATION_ID)
            }
        }
    }
}
