package com.gauramala.wear.media

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/** App-private, non-backed-up files used only for the user's optional local chant guide. */
internal class VoiceChantFiles(private val directory: File) {
    val recordingFile: File
        get() = File(directory, RECORDING_NAME)

    fun temporaryRecordingFile(): File = File(directory, TEMP_RECORDING_NAME)

    fun hasRecording(): Boolean = recordingFile.isFile && recordingFile.length() > 0L

    fun publishTemporaryRecording(): Boolean {
        val temporaryFile = temporaryRecordingFile()
        if (!temporaryFile.isFile || temporaryFile.length() == 0L) return false
        return runCatching {
            Files.move(temporaryFile.toPath(), recordingFile.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
            true
        }.getOrElse {
            runCatching {
                Files.move(temporaryFile.toPath(), recordingFile.toPath(), REPLACE_EXISTING)
                true
            }.getOrDefault(false)
        }
    }

    fun discardTemporaryRecording() = temporaryRecordingFile().delete()

    /** Drops interrupted recordings and trial files from older app versions. */
    fun clearPendingTakes() {
        temporaryRecordingFile().delete()
        File(directory, SELECTING_NAME).delete()
        (1..LEGACY_TRIAL_COUNT).forEach { File(directory, "chant-guide-trial-$it.m4a").delete() }
    }

    fun deleteAll() {
        clearPendingTakes()
        recordingFile.delete()
    }

    private companion object {
        const val LEGACY_TRIAL_COUNT = 3
        const val RECORDING_NAME = "chant-guide.m4a"
        const val TEMP_RECORDING_NAME = "chant-guide-recording.tmp"
        const val SELECTING_NAME = "chant-guide-selecting.tmp"
    }
}
