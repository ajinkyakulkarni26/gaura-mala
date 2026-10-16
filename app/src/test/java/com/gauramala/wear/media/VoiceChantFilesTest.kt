package com.gauramala.wear.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class VoiceChantFilesTest {
    @Test
    fun onlyPublishedNonemptyRecordingIsAvailable() {
        val directory = Files.createTempDirectory("gaura-chant-files-").toFile()
        try {
            val files = VoiceChantFiles(directory)
            assertFalse(files.hasRecording())
            assertFalse(files.publishTemporaryRecording())

            files.temporaryRecordingFile().writeBytes(byteArrayOf(1, 2, 3))
            assertTrue(files.publishTemporaryRecording())
            assertTrue(files.hasRecording())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun deleteAllRemovesSavedAndIncompleteRecordings() {
        val directory = Files.createTempDirectory("gaura-chant-files-").toFile()
        try {
            val files = VoiceChantFiles(directory)
            files.recordingFile.writeBytes(byteArrayOf(1, 2, 3))
            files.temporaryRecordingFile().writeBytes(byteArrayOf(4, 5, 6))

            files.deleteAll()

            assertFalse(files.hasRecording())
            assertFalse(files.temporaryRecordingFile().exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun incompleteReplacementDoesNotDeletePreviousRecording() {
        val directory = Files.createTempDirectory("gaura-chant-files-").toFile()
        try {
            val files = VoiceChantFiles(directory)
            val original = byteArrayOf(1, 2, 3)
            files.recordingFile.writeBytes(original)
            files.temporaryRecordingFile().writeBytes(byteArrayOf())

            assertFalse(files.publishTemporaryRecording())
            assertTrue(files.hasRecording())
            assertTrue(files.recordingFile.readBytes().contentEquals(original))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun publishingReplacementReplacesSavedRecordingAndConsumesTemporaryFile() {
        val directory = Files.createTempDirectory("gaura-chant-files-").toFile()
        try {
            val files = VoiceChantFiles(directory)
            files.recordingFile.writeBytes(byteArrayOf(1, 2, 3))
            val replacement = byteArrayOf(4, 5, 6, 7)
            files.temporaryRecordingFile().writeBytes(replacement)

            assertTrue(files.publishTemporaryRecording())
            assertTrue(files.recordingFile.readBytes().contentEquals(replacement))
            assertFalse(files.temporaryRecordingFile().exists())
        } finally {
            directory.deleteRecursively()
        }
    }
}
