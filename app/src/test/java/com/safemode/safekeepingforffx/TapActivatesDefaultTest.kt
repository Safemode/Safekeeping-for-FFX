package com.safemode.safekeepingforffx

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Tap-to-activate became the default in 1.0.6. A fresh install gets it on; an update keeps the
 * tap-to-open behaviour its player has been using, since an untouched setting was never stored and
 * would otherwise flip with the default.
 */
class TapActivatesDefaultTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val file by lazy { folder.newFile("settings.preferences_pb").apply { delete() } }

    /** Opens the settings store as a launch would, reads the setting, then closes it again. */
    private fun launch(isExistingInstall: Boolean): Boolean = runBlocking {
        val job = Job()
        val store = PreferenceDataStoreFactory.create(
            migrations = listOf(SettingsRepository.TapActivatesDefault { isExistingInstall }),
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { file }
        )
        val value = SettingsRepository(store).sphereGridTapActivates.first()
        // Only one store may hold the file at a time, so release it before the next launch.
        job.cancelAndJoin()
        value
    }

    @Test
    fun aFreshInstallTapsToActivate() {
        assertEquals(true, launch(isExistingInstall = false))
    }

    @Test
    fun anUpdatedInstallKeepsTapToOpen() {
        assertEquals(false, launch(isExistingInstall = true))
    }

    @Test
    fun theChoiceIsMadeOnceAndKept() {
        assertEquals(true, launch(isExistingInstall = false))
        // A later update must not re-run it and switch a fresh install back to the old default.
        assertEquals(true, launch(isExistingInstall = true))
    }

    @Test
    fun aSettingThePlayerChoseIsLeftAlone() = runBlocking {
        var asked = false
        val migration = SettingsRepository.TapActivatesDefault { asked = true; true }
        val chosen = preferencesOf(booleanPreferencesKey("sphere_grid_tap_activates") to true)

        assertFalse(migration.shouldMigrate(chosen))
        assertFalse("The install check isn't needed once the player has chosen", asked)
    }
}
