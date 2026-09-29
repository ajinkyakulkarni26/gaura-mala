package com.gauramala.wear.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Provides eyes-free tactile feedback for chanting.
 * Different haptic signatures convey single bead advances, milestone beads,
 * round completions (108 beads), and daily target completions.
 */
class HapticHelper(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(VibratorManager::class.java)
        vibratorManager?.defaultVibrator ?: context.getSystemService(Vibrator::class.java)
    } else {
        context.getSystemService(Vibrator::class.java)
    }

    private fun activeVibrator(): Vibrator? = vibrator?.takeIf { it.hasVibrator() }

    /**
     * Crisp, subtle tactile click for advancing 1 bead.
     * Mimics moving across a physical tulasi bead without visual attention.
     */
    fun beadClick(enabled: Boolean = true) {
        if (!enabled) return
        val deviceVibrator = activeVibrator() ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            deviceVibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            deviceVibrator.vibrate(25)
        }
    }

    /**
     * Subtle double tick for milestones (quarter rounds at 27, 54, and 81 beads).
     */
    fun milestoneAlert(enabled: Boolean = true) {
        if (!enabled) return
        val deviceVibrator = activeVibrator() ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 30, 60, 30)
            val amplitudes = intArrayOf(0, 160, 0, 160)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            deviceVibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            deviceVibrator.vibrate(longArrayOf(0, 30, 60, 30), -1)
        }
    }

    /**
     * Distinctive double-pulse vibration marking the completion of 1 full round (108 beads).
     * Strong and unmistakable, signaling the chanter to pause and offer respect before the next round.
     */
    fun roundCompletedAlert(enabled: Boolean = true) {
        if (!enabled) return
        val deviceVibrator = activeVibrator() ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // [delay, vibrate, pause, vibrate]
            val timings = longArrayOf(0, 140, 100, 260)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            deviceVibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            deviceVibrator.vibrate(longArrayOf(0, 140, 100, 260), -1)
        }
    }

    /**
     * Celebratory 3-pulse crescendo vibration when the daily vow (e.g. 16 rounds) is achieved.
     */
    fun dailyGoalAchievedAlert(enabled: Boolean = true) {
        if (!enabled) return
        val deviceVibrator = activeVibrator() ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 120, 80, 160, 80, 320)
            val amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            deviceVibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            deviceVibrator.vibrate(longArrayOf(0, 120, 80, 160, 80, 320), -1)
        }
    }

    /**
     * Subtle undo vibration to confirm bead deduction.
     */
    fun undoAlert(enabled: Boolean = true) {
        if (!enabled) return
        val deviceVibrator = activeVibrator() ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            deviceVibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            deviceVibrator.vibrate(15)
        }
    }
}
