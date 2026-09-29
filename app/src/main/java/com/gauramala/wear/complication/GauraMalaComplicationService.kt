package com.gauramala.wear.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.gauramala.wear.MainActivity
import com.gauramala.wear.data.MantraPreferences
import kotlinx.coroutines.flow.first

class GauraMalaComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("8/16").build(),
                    contentDescription = PlainComplicationText.Builder("8 rounds completed").build()
                ).setTitle(PlainComplicationText.Builder("Mala").build()).build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = 8f,
                    min = 0f,
                    max = 16f,
                    contentDescription = PlainComplicationText.Builder("8 of 16 rounds").build()
                ).setText(PlainComplicationText.Builder("8/16").build()).build()
            }
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val prefs = MantraPreferences(this)
        prefs.resetIfNewDay()
        val userPrefs = prefs.userPreferencesFlow.first()

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completed = userPrefs.completedRounds
        val goal = userPrefs.dailyGoalRounds

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("$completed/$goal").build(),
                    contentDescription = PlainComplicationText.Builder("$completed of $goal rounds").build()
                )
                .setTitle(PlainComplicationText.Builder("Mala").build())
                .setTapAction(pendingIntent)
                .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = completed.toFloat().coerceIn(0f, goal.toFloat()),
                    min = 0f,
                    max = goal.toFloat(),
                    contentDescription = PlainComplicationText.Builder("$completed of $goal rounds").build()
                )
                .setText(PlainComplicationText.Builder("$completed").build())
                .setTitle(PlainComplicationText.Builder("Rounds").build())
                .setTapAction(pendingIntent)
                .build()
            }
            else -> null
        }
    }
}
