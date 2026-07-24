package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.backup.BACKUP_FORMAT
import com.safemode.safekeepingforffx.data.backup.BACKUP_VERSION
import com.safemode.safekeepingforffx.data.backup.BackupChecklistEntry
import com.safemode.safekeepingforffx.data.backup.BackupCodec
import com.safemode.safekeepingforffx.data.backup.BackupFile
import com.safemode.safekeepingforffx.data.backup.BackupMonsterCapture
import com.safemode.safekeepingforffx.data.backup.BackupSettings
import com.safemode.safekeepingforffx.data.backup.BackupSphereGridActivation
import com.safemode.safekeepingforffx.data.backup.BackupSphereGridEdit
import com.safemode.safekeepingforffx.data.backup.BackupSphereGridRoute
import com.safemode.safekeepingforffx.data.backup.backupFileName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * A backup file is the player's only copy of a playthrough they may have spent months on, so the
 * format has to round-trip every section exactly and refuse anything it cannot safely apply. The
 * envelope is strict - a file that isn't ours, or is from a newer format, fails outright rather
 * than restoring half of itself over live progress.
 */
class BackupCodecTest {

    private fun sample() = BackupFile(
        appVersion = "0.8.1",
        appVersionCode = 18,
        createdAt = "2026-07-24T14:32:10Z",
        settings = BackupSettings(
            gameVersion = "ORIGINAL_PS2",
            theme = "MIDNIGHT",
            showHelp = false,
            sphereGridTapActivates = true,
            sphereGridFullNodeEditor = null
        ),
        checklists = listOf(
            BackupChecklistEntry("al_bhed_primers", "primer_01", true, 1_700_000_000_000),
            BackupChecklistEntry("al_bhed_primers", "primer_02", false, 1_700_000_000_001)
        ),
        monsterCaptures = listOf(BackupMonsterCapture("dingo", 7, 1_700_000_000_002)),
        sphereGridEdits = listOf(BackupSphereGridEdit("s_n12", "A:STRENGTH:4", 3)),
        sphereGridActivations = listOf(
            BackupSphereGridActivation("TIDUS", "s_n12", 4),
            BackupSphereGridActivation("YUNA", "s_n40", 5)
        ),
        sphereGridRoutes = listOf(
            BackupSphereGridRoute("Tidus opener", "STANDARD", 1_700_000_000_003, "{\"v\":2}")
        )
    )

    @Test
    fun everySectionRoundTrips() {
        val backup = sample()
        assertEquals(backup, BackupCodec.decode(BackupCodec.encode(backup)).getOrThrow())
    }

    @Test
    fun emptyBackupRoundTrips() {
        // A brand new install has nothing to back up. That must still produce a readable file
        // rather than something the restore later chokes on.
        val backup = BackupFile(createdAt = "2026-07-24T14:32:10Z")
        val decoded = BackupCodec.decode(BackupCodec.encode(backup)).getOrThrow()
        assertEquals(backup, decoded)
        assertTrue(decoded.counts.isEmpty)
    }

    @Test
    fun writesFormatAndVersion() {
        val json = BackupCodec.encode(sample())
        assertTrue(json.contains("\"format\": \"$BACKUP_FORMAT\""))
        assertTrue(json.contains("\"version\": $BACKUP_VERSION"))
    }

    @Test
    fun jsonIsIndentedForReading() {
        // A backup is a file the player keeps and may want to open. Being readable is part of it.
        assertTrue(BackupCodec.encode(sample()).contains("\n  \""))
    }

    @Test
    fun countsOnlyReportWhatWasKept() {
        // Unchecked rows are carried in the file but are not "progress" to report back.
        val counts = sample().counts
        assertEquals(1, counts.checkedItems)
        assertEquals(1, counts.capturedFiends)
        assertEquals(1, counts.gridEdits)
        assertEquals(2, counts.gridActivations)
        assertEquals(1, counts.savedRoutes)
        assertFalse(counts.isEmpty)
    }

    @Test
    fun rejectsAnotherAppsJson() {
        val result = BackupCodec.decode("""{"format":"something-else","version":1}""")
        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsNonJson() {
        assertTrue(BackupCodec.decode("this is not a backup").isFailure)
    }

    @Test
    fun rejectsNewerFormatVersion() {
        val result = BackupCodec.decode("""{"format":"$BACKUP_FORMAT","version":99}""")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("newer version"))
    }

    @Test
    fun missingSectionsDecodeAsEmpty() {
        // Only the envelope is required, so a hand-written or partial file still restores.
        val decoded = BackupCodec.decode(
            """{"format":"$BACKUP_FORMAT","version":$BACKUP_VERSION}"""
        ).getOrThrow()
        assertTrue(decoded.checklists.isEmpty())
        assertTrue(decoded.monsterCaptures.isEmpty())
        assertTrue(decoded.sphereGridEdits.isEmpty())
        assertTrue(decoded.sphereGridActivations.isEmpty())
        assertTrue(decoded.sphereGridRoutes.isEmpty())
    }

    @Test
    fun fileNameCarriesAppNameAndDate() {
        val name = backupFileName()
        assertTrue(name.startsWith("safekeeping-for-ffx-backup-"))
        assertTrue(name.endsWith(".json"))
        val stamp = name.removePrefix("safekeeping-for-ffx-backup-").removeSuffix(".json")
        // Parses as a real date and time, so two backups on one day never collide.
        assertNotNull(SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).apply { isLenient = false }.parse(stamp))
    }
}
