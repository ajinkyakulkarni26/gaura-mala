package com.gauramala.wear.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build

/** Records and plays one short chant entirely inside the app's non-backed-up private directory. */
internal class VoiceChantAudio(context: Context) {
    private val files = VoiceChantFiles(context.noBackupFilesDir)
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    fun hasRecording(): Boolean = files.hasRecording()

    fun recordingDurationMillis(): Long? {
        if (!files.hasRecording()) return null
        val retriever = MediaMetadataRetriever()
        val duration = try {
            retriever.setDataSource(files.recordingFile.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
        return duration?.takeIf { it > 0L }
    }

    fun startRecording(onLimitReached: () -> Unit): Boolean {
        if (recorder != null) return false
        val temporaryFile = files.temporaryRecordingFile()
        temporaryFile.parentFile?.mkdirs()
        temporaryFile.delete()

        val activeRecorder = MediaRecorder()
        return try {
            activeRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) setPrivacySensitive(true)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64_000)
                setAudioSamplingRate(22_050)
                setMaxDuration(MAX_RECORDING_MILLIS)
                setOutputFile(temporaryFile.absolutePath)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        finishRecording()
                        onLimitReached()
                    }
                }
                prepare()
            }
            recorder = activeRecorder
            activeRecorder.start()
            true
        } catch (_: Exception) {
            recorder = null
            runCatching { activeRecorder.reset() }
            runCatching { activeRecorder.release() }
            temporaryFile.delete()
            false
        }
    }

    fun finishRecording(): Boolean {
        val activeRecorder = recorder ?: return false
        recorder = null
        val stopped = runCatching {
            activeRecorder.stop()
            true
        }.getOrDefault(false)
        runCatching { activeRecorder.release() }

        if (!stopped) {
            files.temporaryRecordingFile().delete()
            return false
        }
        return files.publishTemporaryRecording()
    }

    fun cancelRecording() {
        val activeRecorder = recorder
        recorder = null
        if (activeRecorder != null) {
            runCatching { activeRecorder.stop() }
            runCatching { activeRecorder.release() }
        }
        files.temporaryRecordingFile().delete()
    }

    fun deleteRecording() {
        cancelRecording()
        stopPlayback()
        files.deleteAll()
    }

    fun preview(onFinished: () -> Unit): Boolean = play {
        onFinished()
        false
    }

    fun playLoop(onChantCompleted: () -> Boolean): Boolean = play(onChantCompleted)

    fun stopPlayback() {
        val activePlayer = player
        player = null
        if (activePlayer != null) {
            runCatching { if (activePlayer.isPlaying) activePlayer.stop() }
            runCatching { activePlayer.release() }
        }
    }

    fun release() {
        cancelRecording()
        stopPlayback()
    }

    private fun play(onCompletion: () -> Boolean): Boolean {
        if (!files.hasRecording()) return false
        stopPlayback()
        var activePlayer: MediaPlayer? = null
        return try {
            activePlayer = MediaPlayer()
            activePlayer.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(files.recordingFile.absolutePath)
                setOnCompletionListener { completedPlayer ->
                    if (onCompletion()) {
                        runCatching {
                            completedPlayer.seekTo(0)
                            completedPlayer.start()
                        }.onFailure { stopPlayback() }
                    } else {
                        stopPlayback()
                    }
                }
                prepare()
                player = this
                start()
            }
            true
        } catch (_: Exception) {
            if (player === activePlayer) player = null
            runCatching { activePlayer?.release() }
            false
        }
    }

    private companion object {
        const val MAX_RECORDING_MILLIS = 20_000
    }
}
