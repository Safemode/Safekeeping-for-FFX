package com.safemode.safekeepingforffx.di

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.safemode.safekeepingforffx.BuildConfig
import com.safemode.safekeepingforffx.data.local.FfxDatabase
import com.safemode.safekeepingforffx.data.repository.BackupRepository
import com.safemode.safekeepingforffx.data.repository.ChecklistRepository
import com.safemode.safekeepingforffx.data.repository.FavoritesRepository
import com.safemode.safekeepingforffx.data.repository.ItemListRepository
import com.safemode.safekeepingforffx.data.repository.MixRepository
import com.safemode.safekeepingforffx.data.repository.MonsterArenaRepository
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import com.safemode.safekeepingforffx.data.repository.SphereGridRepository

/**
 * A settings file that can't be parsed is replaced with defaults rather than left to throw: every
 * screen reads settings, so an unreadable file would otherwise crash the app on each launch. Only
 * preferences live here - progress is in the database and is untouched.
 */
private val Context.settingsDataStore by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    produceMigrations = { context ->
        listOf(SettingsRepository.TapActivatesDefault { context.isUpdatedInstall() })
    }
)

/**
 * True when this app was installed by an update rather than freshly. A fresh install sets both
 * timestamps at once; any later install over it, from Play or a sideloaded APK, moves only the
 * update time. Treated as an update if the package can't be read, which keeps the old behaviour.
 */
private fun Context.isUpdatedInstall(): Boolean = runCatching {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    info.lastUpdateTime != info.firstInstallTime
}.getOrDefault(true)

/** Manual dependency container. The app is small enough that Hilt would be overhead. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database by lazy { FfxDatabase.getInstance(appContext) }

    val checklistRepository by lazy { ChecklistRepository(database.checklistProgressDao()) }

    val favoritesRepository by lazy { FavoritesRepository(database.favoriteDao()) }

    val settingsRepository by lazy { SettingsRepository(appContext.settingsDataStore) }

    val mixRepository by lazy { MixRepository(appContext.assets) }

    val itemListRepository by lazy { ItemListRepository(appContext.assets) }

    val monsterArenaRepository by lazy {
        MonsterArenaRepository(appContext.assets, database.monsterCaptureDao())
    }

    val sphereGridRepository by lazy {
        SphereGridRepository(
            appContext.assets,
            database,
            database.sphereGridNodeDao(),
            database.sphereGridActivationDao(),
            database.sphereGridRouteDao(),
            settingsRepository
        )
    }

    /**
     * Spans every store the player writes to, so it is built here from the DAOs directly rather
     * than layered on the feature repositories - see [BackupRepository]. The app version is passed
     * in rather than read inside, keeping the data layer free of generated build classes.
     */
    val backupRepository by lazy {
        BackupRepository(
            database = database,
            checklistDao = database.checklistProgressDao(),
            monsterCaptureDao = database.monsterCaptureDao(),
            nodeDao = database.sphereGridNodeDao(),
            activationDao = database.sphereGridActivationDao(),
            routeDao = database.sphereGridRouteDao(),
            favoriteDao = database.favoriteDao(),
            settingsRepository = settingsRepository,
            appVersion = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE
        )
    }
}
