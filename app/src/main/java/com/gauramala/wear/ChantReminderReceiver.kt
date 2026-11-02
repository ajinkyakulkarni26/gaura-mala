package com.gauramala.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gauramala.wear.data.MantraPreferences
import com.gauramala.wear.presentation.ChantReminderPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Handles scheduled chanting reminder alarms and notification actions without opening the app. */
class ChantReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleAction(appContext, action)
            } finally {
                pendingResult.finish()
            }
        }
    }

    internal suspend fun handleAction(
        context: Context,
        action: String,
        now: LocalDateTime = LocalDateTime.now()
    ) {
        val preferences = MantraPreferences(context)
        val scheduler = ChantReminderScheduler(context)
        when (action) {
            ChantReminderScheduler.ACTION_MARK_GOAL_DONE -> {
                preferences.markDailyGoalCompleted()
                scheduler.cancelAll()
            }
            ChantReminderScheduler.ACTION_SHOW_REMINDER -> {
                val snapshot = preferences.userPreferencesFlow.first()
                if (!ChantReminderPolicy.shouldDeliverReminderNow(
                        preferences = snapshot,
                        today = now.toLocalDate().toString(),
                        hourOfDay = now.hour
                    )
                ) {
                    scheduler.cancelAll()
                    return
                }
                val content = ChantReminderPolicy.buildReminderContent(
                    completedRounds = snapshot.completedRounds,
                    dailyGoalRounds = snapshot.dailyGoalRounds,
                    previousTemplateIndex = snapshot.lastReminderTemplateIndex
                )
                preferences.updateLastReminderTemplateIndex(content.templateIndex)
                scheduler.showNotification(content)
                scheduler.scheduleNext(now)
            }
        }
    }
}

