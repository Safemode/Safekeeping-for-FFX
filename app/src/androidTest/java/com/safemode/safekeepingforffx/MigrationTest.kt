package com.safemode.safekeepingforffx

import android.database.Cursor
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.safemode.safekeepingforffx.data.local.FfxDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Replays the real migrations against the schemas exported to `app/schemas`, so an update can never
 * reach players with a migration that crashes on launch or drops their progress. Destructive
 * fallback is deliberately off (see [FfxDatabase]), which makes a broken migration a crash rather
 * than a silent wipe - and this is where it gets caught instead.
 *
 * Versions 2 and 5 are the ones public releases shipped before schemas were exported (v0.5.x and
 * v0.6.x); their schema files were regenerated from those release tags. Versions 1, 3 and 4 never
 * shipped, so there is no schema for them.
 *
 * Instrumented, because the helper needs a real SQLite: run with `./gradlew connectedDebugAndroidTest`
 * on an emulator or device before releasing any schema change.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(instrumentation, FfxDatabase::class.java)

    /**
     * Every schema exported to `app/schemas`, read from the test APK's assets rather than listed by
     * hand, so a new version's schema - which the build writes on its own - is covered automatically.
     */
    private val exportedVersions: List<Int> =
        instrumentation.context.assets.list(FfxDatabase::class.java.name).orEmpty()
            .mapNotNull { it.removeSuffix(".json").toIntOrNull() }
            .sorted()

    private val latest: Int get() = exportedVersions.last()

    @After
    fun deleteTestDatabase() {
        instrumentation.targetContext.deleteDatabase(TEST_DB)
    }

    @Test
    fun v2ChecklistTicksAndCapturesSurviveToLatest() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL("INSERT INTO checklist_progress VALUES ('albhed', 'albhed_01', 1, 100)")
            db.execSQL("INSERT INTO monster_capture VALUES ('besaid_dingo', 7, 200)")
        }

        helper.runMigrationsAndValidate(TEST_DB, latest, true, *FfxDatabase.ALL_MIGRATIONS).use { db ->
            db.query("SELECT isChecked, updatedAt FROM checklist_progress WHERE itemId = 'albhed_01'")
                .use { assertRow(it, 1L, 100L) }
            db.query("SELECT count FROM monster_capture WHERE monsterId = 'besaid_dingo'")
                .use { assertRow(it, 7L) }
        }
    }

    @Test
    fun v5GridWorkSurvivesAndGetsAnOrderedTimeline() {
        helper.createDatabase(TEST_DB, 5).use { db ->
            db.execSQL("INSERT INTO sphere_grid_node VALUES ('n10', 'A|STRENGTH|4')")
            db.execSQL("INSERT INTO sphere_grid_activation VALUES ('TIDUS', 'n1')")
            db.execSQL("INSERT INTO sphere_grid_activation VALUES ('TIDUS', 'n2')")
        }

        helper.runMigrationsAndValidate(TEST_DB, latest, true, *FfxDatabase.ALL_MIGRATIONS).use { db ->
            db.query("SELECT content FROM sphere_grid_node WHERE nodeId = 'n10'")
                .use { assertRow(it, "A|STRENGTH|4") }
            // 6 -> 7 backfills seq from insertion order, so the path replays in the order it was
            // walked rather than all at "order unknown".
            db.query("SELECT seq FROM sphere_grid_activation ORDER BY seq").use { cursor ->
                val seqs = buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) }
                assertEquals(2, seqs.size)
                assertTrue("seq should be backfilled and increasing: $seqs", seqs[0] in 1 until seqs[1])
            }
        }
    }

    @Test
    fun v8RoutesGetUpdatedAtSeededFromCreatedAt() {
        helper.createDatabase(TEST_DB, 8).use { db ->
            db.execSQL(
                "INSERT INTO sphere_grid_route (name, gridType, createdAt, payload) " +
                    "VALUES ('Tidus build', 'STANDARD', 12345, '{}')"
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, latest, true, *FfxDatabase.ALL_MIGRATIONS).use { db ->
            db.query("SELECT createdAt, updatedAt FROM sphere_grid_route").use { assertRow(it, 12345L, 12345L) }
        }
    }

    /**
     * The check that matters most: Room itself, built exactly as the app builds it, opens a database
     * left at every exported version. This is what fails if a migration is written but never added to
     * [FfxDatabase.ALL_MIGRATIONS], or if the version is bumped without one.
     */
    @Test
    fun appOpensDatabaseFromEveryExportedVersion() {
        // Guards the guard: an empty listing would make the loop below pass without testing anything.
        assertTrue("schemas missing from test assets: $exportedVersions", exportedVersions.size >= 6)
        val context = instrumentation.targetContext
        exportedVersions.forEach { version ->
            context.deleteDatabase(TEST_DB)
            helper.createDatabase(TEST_DB, version).close()

            val database = Room.databaseBuilder(context, FfxDatabase::class.java, TEST_DB)
                .addMigrations(*FfxDatabase.ALL_MIGRATIONS)
                .build()
            try {
                runBlocking { database.checklistProgressDao().snapshot() }
            } finally {
                database.close()
            }
        }
    }

    private fun assertRow(cursor: Cursor, vararg expected: Any) {
        assertTrue("expected a row", cursor.moveToFirst())
        expected.forEachIndexed { column, value ->
            val actual: Any = when (value) {
                is String -> cursor.getString(column)
                else -> cursor.getLong(column)
            }
            assertEquals(value, actual)
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
