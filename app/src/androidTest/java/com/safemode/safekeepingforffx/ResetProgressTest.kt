package com.safemode.safekeepingforffx

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.safemode.safekeepingforffx.data.local.ChecklistProgressEntity
import com.safemode.safekeepingforffx.data.local.FavoriteEntity
import com.safemode.safekeepingforffx.data.local.FfxDatabase
import com.safemode.safekeepingforffx.data.local.MonsterCaptureEntity
import com.safemode.safekeepingforffx.data.local.SphereGridActivationEntity
import com.safemode.safekeepingforffx.data.local.SphereGridNodeEntity
import com.safemode.safekeepingforffx.data.local.SphereGridRouteEntity
import com.safemode.safekeepingforffx.data.reference.ThemePreference
import com.safemode.safekeepingforffx.data.repository.BackupRepository
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * "Reset all progress" against a real database: everything the player has done is cleared, while
 * the saved routes library and the settings survive. Instrumented because the reset runs in a Room
 * transaction, which needs a real SQLite.
 */
@RunWith(AndroidJUnit4::class)
class ResetProgressTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = Room.inMemoryDatabaseBuilder(context, FfxDatabase::class.java).build()
    private val settingsFile = File(context.cacheDir, "reset-test.preferences_pb").apply { delete() }
    private val settingsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val settings = SettingsRepository(
        PreferenceDataStoreFactory.create(scope = settingsScope) { settingsFile }
    )
    private val repository = BackupRepository(
        database = database,
        checklistDao = database.checklistProgressDao(),
        monsterCaptureDao = database.monsterCaptureDao(),
        nodeDao = database.sphereGridNodeDao(),
        activationDao = database.sphereGridActivationDao(),
        routeDao = database.sphereGridRouteDao(),
        favoriteDao = database.favoriteDao(),
        settingsRepository = settings,
        appVersion = "test",
        appVersionCode = 0
    )

    @After
    fun tearDown() {
        database.close()
        settingsScope.cancel()
        settingsFile.delete()
    }

    @Test
    fun resetClearsEveryKindOfProgressButKeepsRoutesAndSettings() = runBlocking {
        database.checklistProgressDao().upsert(ChecklistProgressEntity("al_bhed_primers", "albhed_01", true, 1))
        database.favoriteDao().upsert(FavoriteEntity("monster_arena", "besaid_dingo", 1))
        database.monsterCaptureDao().upsert(MonsterCaptureEntity("besaid_dingo", 5, 1))
        database.sphereGridNodeDao().upsert(SphereGridNodeEntity("n10", "A|STRENGTH|4", 1))
        database.sphereGridActivationDao().upsert(SphereGridActivationEntity("TIDUS", "n1", 2))
        val routeId = database.sphereGridRouteDao()
            .insert(SphereGridRouteEntity(name = "Tidus", gridType = "STANDARD", createdAt = 1, updatedAt = 1, payload = "{}"))
        settings.setActiveSphereGridRouteId(routeId)
        settings.setTheme(ThemePreference.DARK)

        repository.clearAllProgress()

        assertTrue(database.checklistProgressDao().snapshot().isEmpty())
        assertTrue(database.favoriteDao().snapshot().isEmpty())
        assertTrue(database.monsterCaptureDao().getAll().isEmpty())
        assertTrue(database.sphereGridNodeDao().snapshot().isEmpty())
        assertTrue(database.sphereGridActivationDao().snapshot().isEmpty())
        // The library and the settings are not progress, so they stay.
        assertEquals(1, database.sphereGridRouteDao().snapshot().size)
        assertEquals(ThemePreference.DARK, settings.theme.first())
        // The emptied live grid no longer matches the route it was loaded from.
        assertNull(settings.activeSphereGridRouteId.first())
    }
}
