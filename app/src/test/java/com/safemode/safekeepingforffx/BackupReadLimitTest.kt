package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.ui.screens.settings.BackupTooLargeException
import com.safemode.safekeepingforffx.ui.screens.settings.readBackupText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * A restore reads the picked file with a size cap, so a large file picked by mistake is refused
 * partway instead of being read whole into memory - and a real backup still comes back intact.
 */
class BackupReadLimitTest {

    private fun streamOf(text: String) = ByteArrayInputStream(text.toByteArray(Charsets.UTF_8))

    @Test
    fun aFileUnderTheLimitIsReadWhole() {
        // Longer than the reader's 64 KB buffer and full of two-byte characters, so a character
        // split across two reads has to come back whole.
        val text = "{\"name\": \"Café été\"}".repeat(10_000)
        assertEquals(text, readBackupText(streamOf(text), limit = 1_000_000))
    }

    @Test
    fun aFileExactlyAtTheLimitIsRead() {
        val text = "x".repeat(1_000)
        assertEquals(text, readBackupText(streamOf(text), limit = 1_000))
    }

    @Test
    fun aFileOverTheLimitIsRefused() {
        val text = "x".repeat(1_001)
        assertThrows(BackupTooLargeException::class.java) {
            readBackupText(streamOf(text), limit = 1_000)
        }
    }
}
