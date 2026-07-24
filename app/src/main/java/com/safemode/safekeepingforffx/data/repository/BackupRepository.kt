package com.safemode.safekeepingforffx.data.repository

import androidx.room.withTransaction
import com.safemode.safekeepingforffx.data.backup.BackupChecklistEntry
import com.safemode.safekeepingforffx.data.backup.BackupCodec
import com.safemode.safekeepingforffx.data.backup.BackupCounts
import com.safemode.safekeepingforffx.data.backup.BackupFile
import com.safemode.safekeepingforffx.data.backup.BackupMonsterCapture
import com.safemode.safekeepingforffx.data.backup.BackupSettings
import com.safemode.safekeepingforffx.data.backup.BackupSphereGridActivation
import com.safemode.safekeepingforffx.data.backup.BackupSphereGridEdit
import com.safemode.safekeepingforffx.data.backup.BackupSphereGridRoute
import com.safemode.safekeepingforffx.data.backup.backupTimestamp
import com.safemode.safekeepingforffx.data.local.ChecklistProgressDao
import com.safemode.safekeepingforffx.data.local.ChecklistProgressEntity
import com.safemode.safekeepingforffx.data.local.FfxDatabase
import com.safemode.safekeepingforffx.data.local.MonsterCaptureDao
import com.safemode.safekeepingforffx.data.local.MonsterCaptureEntity
import com.safemode.safekeepingforffx.data.local.SphereGridActivationDao
import com.safemode.safekeepingforffx.data.local.SphereGridActivationEntity
import com.safemode.safekeepingforffx.data.local.SphereGridNodeDao
import com.safemode.safekeepingforffx.data.local.SphereGridNodeEntity
import com.safemode.safekeepingforffx.data.local.SphereGridRouteDao
import com.safemode.safekeepingforffx.data.local.SphereGridRouteEntity
import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.MAX_CAPTURES

/**
 * Writes and reads the whole-app backup file.
 *
 * Everything the player produced lives in one of two places - the Room database (checklists, capture
 * counts, the Sphere Grid Planner) and the DataStore settings - so this is the one class that spans
 * both. It deliberately reads the DAOs directly rather than going through the feature repositories:
 * a backup wants the stored rows verbatim, including the `seq` ordering a saved route replays in,
 * not the joined-with-reference-data view those repositories expose.
 *
 * Nothing here touches files or content URIs. It speaks JSON strings in and out, which keeps it
 * testable and leaves the actual picking and writing to the UI layer.
 */
class BackupRepository(
    private val database: FfxDatabase,
    private val checklistDao: ChecklistProgressDao,
    private val monsterCaptureDao: MonsterCaptureDao,
    private val nodeDao: SphereGridNodeDao,
    private val activationDao: SphereGridActivationDao,
    private val routeDao: SphereGridRouteDao,
    private val settingsRepository: SettingsRepository,
    private val appVersion: String,
    private val appVersionCode: Int
) {

    /** The player's whole state as a JSON document, ready to write to a file. */
    suspend fun exportJson(): String = BackupCodec.encode(snapshot())

    /** The player's whole state as a [BackupFile]. Split out from [exportJson] so it can be tested. */
    suspend fun snapshot(): BackupFile {
        val settings = settingsRepository.snapshot()
        return BackupFile(
            appVersion = appVersion,
            appVersionCode = appVersionCode,
            createdAt = backupTimestamp(),
            settings = BackupSettings(
                gameVersion = settings.gameVersion,
                theme = settings.theme,
                showHelp = settings.showHelp,
                sphereGridTapActivates = settings.sphereGridTapActivates,
                sphereGridFullNodeEditor = settings.sphereGridFullNodeEditor
            ),
            checklists = checklistDao.snapshot().map {
                BackupChecklistEntry(it.categoryId, it.itemId, it.isChecked, it.updatedAt)
            },
            monsterCaptures = monsterCaptureDao.getAll().map {
                BackupMonsterCapture(it.monsterId, it.count, it.updatedAt)
            },
            sphereGridEdits = nodeDao.snapshot().map {
                BackupSphereGridEdit(it.nodeId, it.content, it.seq)
            },
            sphereGridActivations = activationDao.snapshot().map {
                BackupSphereGridActivation(it.character, it.nodeId, it.seq)
            },
            sphereGridRoutes = routeDao.snapshot().map {
                BackupSphereGridRoute(it.name, it.gridType, it.createdAt, it.payload)
            }
        )
    }

    /**
     * Replaces everything with the contents of [json]. This is a restore, not a merge: every
     * checklist tick, capture count, grid edit, path and saved route currently in the app is dropped
     * first, so what the player ends up with is exactly what the file holds.
     *
     * The database half runs in a single transaction, so a file that fails partway through leaves
     * the existing progress untouched rather than half-overwritten. Settings live in DataStore and
     * can't join that transaction, so they are applied only after the rows land - the ordering that
     * matters, since a failed restore should not have moved the player's game version either.
     *
     * Returns what was restored, for the confirmation message.
     */
    suspend fun restoreJson(json: String): Result<BackupCounts> {
        val backup = BackupCodec.decode(json).getOrElse { return Result.failure(it) }
        return restore(backup)
    }

    /** See [restoreJson]. Split out so a decoded backup can be applied (and tested) directly. */
    suspend fun restore(backup: BackupFile): Result<BackupCounts> {
        // Rows the app could never have written itself are dropped rather than stored: a count the
        // game can't reach, or a character name this build doesn't know, would otherwise sit in the
        // database as data no screen can show or correct.
        val checklists = backup.checklists
            .filter { it.categoryId.isNotBlank() && it.itemId.isNotBlank() }
            .map { ChecklistProgressEntity(it.categoryId, it.itemId, it.isChecked, it.updatedAt) }
        val captures = backup.monsterCaptures
            .filter { it.monsterId.isNotBlank() && it.count > 0 }
            .map {
                MonsterCaptureEntity(it.monsterId, it.count.coerceIn(1, MAX_CAPTURES), it.updatedAt)
            }
        val edits = backup.sphereGridEdits
            .filter { it.nodeId.isNotBlank() }
            .map { SphereGridNodeEntity(it.nodeId, it.content, it.seq) }
        val knownCharacters = GridCharacter.entries.mapTo(HashSet()) { it.name }
        val activations = backup.sphereGridActivations
            .filter { it.nodeId.isNotBlank() && it.character in knownCharacters }
            .map { SphereGridActivationEntity(it.character, it.nodeId, it.seq) }
        val routes = backup.sphereGridRoutes
            .filter { it.payload.isNotBlank() }
            .map { SphereGridRouteEntity(0, it.name, it.gridType, it.createdAt, it.payload) }

        runCatching {
            database.withTransaction {
                checklistDao.clearAll()
                monsterCaptureDao.clearAll()
                nodeDao.clearAll()
                activationDao.clearAll()
                routeDao.clearAll()

                if (checklists.isNotEmpty()) checklistDao.upsertAll(checklists)
                if (captures.isNotEmpty()) monsterCaptureDao.upsertAll(captures)
                if (edits.isNotEmpty()) nodeDao.upsertAll(edits)
                if (activations.isNotEmpty()) activationDao.upsertAll(activations)
                if (routes.isNotEmpty()) routeDao.insertAll(routes)
            }
        }.onFailure { return Result.failure(it) }

        backup.settings?.let { stored ->
            settingsRepository.restore(
                SettingsRepository.Snapshot(
                    gameVersion = stored.gameVersion,
                    theme = stored.theme,
                    showHelp = stored.showHelp,
                    sphereGridTapActivates = stored.sphereGridTapActivates,
                    sphereGridFullNodeEditor = stored.sphereGridFullNodeEditor
                )
            )
        }

        return Result.success(
            BackupCounts(
                checkedItems = checklists.count { it.isChecked },
                capturedFiends = captures.size,
                gridEdits = edits.size,
                gridActivations = activations.size,
                savedRoutes = routes.size
            )
        )
    }
}
