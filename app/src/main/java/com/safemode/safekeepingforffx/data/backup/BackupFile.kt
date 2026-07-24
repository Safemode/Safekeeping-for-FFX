package com.safemode.safekeepingforffx.data.backup

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Identifies the file as one of ours. A restore checks this before anything else, so pointing the
 * picker at an unrelated .json fails with a clear message instead of a half-applied import.
 */
const val BACKUP_FORMAT = "safekeeping-for-ffx-backup"

/**
 * The format version written today. Bump it only when the shape changes in a way an older app
 * could not read; adding an optional section does not need a bump, because a missing section
 * decodes as empty.
 */
const val BACKUP_VERSION = 1

/** Versions [BackupCodec.decode] will read. */
private val SUPPORTED_BACKUP_VERSIONS = setOf(1)

/**
 * Everything the player has done, as one plain-JSON document: their settings, every checklist tick,
 * every Monster Arena count, and the whole Sphere Grid Planner - node edits, per-character paths and
 * the saved routes library.
 *
 * Rows are carried close to how the database stores them (ids, counts, `seq` ordering) rather than
 * as a prettied-up view, because a restore has to reproduce the state exactly - including the
 * timeline order a saved route replays in. Reference data is never included: the item, fiend and
 * grid definitions ship with the app, so a backup only holds what the player themselves produced.
 * That also keeps the file small and readable enough to inspect or hand-edit.
 */
@JsonClass(generateAdapter = true)
data class BackupFile(
    /** Always [BACKUP_FORMAT] on a file we wrote. */
    val format: String = BACKUP_FORMAT,
    val version: Int = BACKUP_VERSION,
    /** Which app build produced the file. Informational - a restore never refuses on these. */
    val appVersion: String? = null,
    val appVersionCode: Int? = null,
    /** ISO-8601 UTC, e.g. `2026-07-24T14:32:10Z`. Informational, same as the app version. */
    val createdAt: String? = null,
    val settings: BackupSettings? = null,
    val checklists: List<BackupChecklistEntry> = emptyList(),
    val monsterCaptures: List<BackupMonsterCapture> = emptyList(),
    val sphereGridEdits: List<BackupSphereGridEdit> = emptyList(),
    val sphereGridActivations: List<BackupSphereGridActivation> = emptyList(),
    val sphereGridRoutes: List<BackupSphereGridRoute> = emptyList()
) {
    /** How many rows of each kind, for the "restored X, Y, Z" confirmation. */
    val counts: BackupCounts
        get() = BackupCounts(
            checkedItems = checklists.count { it.isChecked },
            capturedFiends = monsterCaptures.count { it.count > 0 },
            gridEdits = sphereGridEdits.size,
            gridActivations = sphereGridActivations.size,
            savedRoutes = sphereGridRoutes.size
        )
}

/**
 * The Settings screen's own choices. Stored by enum *name* rather than ordinal, exactly as
 * DataStore holds them, so reordering an enum in a later release cannot silently change what a
 * restore applies. Every field is nullable: a file missing one leaves that setting alone.
 */
@JsonClass(generateAdapter = true)
data class BackupSettings(
    val gameVersion: String? = null,
    val theme: String? = null,
    val showHelp: Boolean? = null,
    val sphereGridTapActivates: Boolean? = null,
    val sphereGridFullNodeEditor: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class BackupChecklistEntry(
    val categoryId: String,
    val itemId: String,
    val isChecked: Boolean,
    val updatedAt: Long = 0
)

@JsonClass(generateAdapter = true)
data class BackupMonsterCapture(
    val monsterId: String,
    val count: Int,
    val updatedAt: Long = 0
)

@JsonClass(generateAdapter = true)
data class BackupSphereGridEdit(
    val nodeId: String,
    val content: String,
    val seq: Long = 0
)

@JsonClass(generateAdapter = true)
data class BackupSphereGridActivation(
    val character: String,
    val nodeId: String,
    val seq: Long = 0
)

/**
 * A saved route. The database row's `id` is deliberately not carried: a restore inserts routes as
 * new rows, so merging a file into a library that already has rows can never collide on a key.
 */
@JsonClass(generateAdapter = true)
data class BackupSphereGridRoute(
    val name: String,
    val gridType: String,
    val createdAt: Long,
    val payload: String
)

/** Row counts for a summary message. */
data class BackupCounts(
    val checkedItems: Int,
    val capturedFiends: Int,
    val gridEdits: Int,
    val gridActivations: Int,
    val savedRoutes: Int
) {
    val isEmpty: Boolean
        get() = checkedItems == 0 && capturedFiends == 0 && gridEdits == 0 &&
            gridActivations == 0 && savedRoutes == 0
}

/**
 * Reads and writes [BackupFile] as indented JSON. Indented on purpose: a backup is a file the
 * player keeps, and being able to open it in any text editor is worth the extra bytes.
 *
 * This is plain Kotlin with no Android dependency, so the format is unit-testable without a device.
 */
object BackupCodec {

    private val adapter = Moshi.Builder().build()
        .adapter(BackupFile::class.java)
        .indent("  ")

    fun encode(backup: BackupFile): String = adapter.toJson(backup)

    /**
     * Strict about the envelope, same rule the build-code codec follows: a file that isn't ours, or
     * is from a format version this app doesn't know, is refused outright rather than partly applied.
     * The failure message is written to be shown to the player as-is.
     */
    fun decode(text: String): Result<BackupFile> {
        val backup = runCatching { adapter.fromJson(text.trim()) }.getOrNull()
            ?: return Result.failure(IllegalArgumentException("That file isn't a Safekeeping backup."))
        if (backup.format != BACKUP_FORMAT) {
            return Result.failure(IllegalArgumentException("That file isn't a Safekeeping backup."))
        }
        if (backup.version !in SUPPORTED_BACKUP_VERSIONS) {
            return Result.failure(
                IllegalArgumentException(
                    "That backup was made by a newer version of the app and can't be read."
                )
            )
        }
        return Result.success(backup)
    }
}

/**
 * The suggested filename for a backup: app name, then the date, then the time - e.g.
 * `safekeeping-for-ffx-backup-2026-07-24-1432.json`. The date is what the player will scan the
 * folder by; the time is there so two backups on one day don't collide or need a "(1)" suffix.
 *
 * Local time on purpose, since the name is read by a person, not parsed.
 */
fun backupFileName(now: Date = Date()): String {
    val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(now)
    return "safekeeping-for-ffx-backup-$stamp.json"
}

/** The `createdAt` stamp inside the file: ISO-8601 UTC, so it sorts and parses anywhere. */
fun backupTimestamp(now: Date = Date()): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(now)
