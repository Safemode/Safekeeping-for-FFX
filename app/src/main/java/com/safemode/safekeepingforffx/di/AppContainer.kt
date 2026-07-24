package com.safemode.safekeepingforffx.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.safemode.safekeepingforffx.BuildConfig
import com.safemode.safekeepingforffx.data.local.FfxDatabase
import com.safemode.safekeepingforffx.data.repository.BackupRepository
import com.safemode.safekeepingforffx.data.repository.ChecklistRepository
import com.safemode.safekeepingforffx.data.repository.ItemListRepository
import com.safemode.safekeepingforffx.data.repository.MixRepository
import com.safemode.safekeepingforffx.data.repository.MonsterArenaRepository
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import com.safemode.safekeepingforffx.data.repository.SphereGridRepository

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Manual dependency container. The app is small enough that Hilt would be overhead. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database by lazy { FfxDatabase.getInstance(appContext) }

    val checklistRepository by lazy { ChecklistRepository(database.checklistProgressDao()) }

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
            database.sphereGridRouteDao()
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
            settingsRepository = settingsRepository,
            appVersion = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE
        )
    }
}
