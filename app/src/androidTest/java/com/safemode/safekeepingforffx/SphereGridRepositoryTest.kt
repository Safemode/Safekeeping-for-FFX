package com.safemode.safekeepingforffx

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.safemode.safekeepingforffx.data.local.FfxDatabase
import com.safemode.safekeepingforffx.data.reference.BuildScope
import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.RouteEvent
import com.safemode.safekeepingforffx.data.reference.SphereGridBuild
import com.safemode.safekeepingforffx.data.reference.SphereGridNode
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import com.safemode.safekeepingforffx.data.repository.SphereGridRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The Sphere Grid Planner against a real database and the bundled grids: saved routes are never
 * written empty, and every way of re-locking a gate takes it off the paths that ran through it.
 * Instrumented because both need Room transactions and the grid assets.
 */
@RunWith(AndroidJUnit4::class)
class SphereGridRepositoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = Room.inMemoryDatabaseBuilder(context, FfxDatabase::class.java).build()
    private val settingsFile = File(context.cacheDir, "grid-test.preferences_pb").apply { delete() }
    private val settingsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val settings = SettingsRepository(
        PreferenceDataStoreFactory.create(scope = settingsScope) { settingsFile }
    )
    private val repository = SphereGridRepository(
        assets = context.assets,
        database = database,
        nodeDao = database.sphereGridNodeDao(),
        activationDao = database.sphereGridActivationDao(),
        routeDao = database.sphereGridRouteDao(),
        settingsRepository = settings
    )

    @After
    fun tearDown() {
        database.close()
        settingsScope.cancel()
        settingsFile.delete()
    }

    private suspend fun locks(type: GridType): List<SphereGridNode> =
        repository.grid(type).nodes.filter { it.original is NodeContent.Lock }

    private suspend fun stat(type: GridType, index: Int = 0): SphereGridNode =
        repository.grid(type).nodes.filter { it.original is NodeContent.Attribute }[index]

    private suspend fun open(lock: SphereGridNode) =
        repository.setContent(lock.id, NodeContent.Empty, lock.original)

    private suspend fun path(character: GridCharacter): Set<String> =
        repository.observeActivations(character).first().toSet()

    @Test
    fun savingAScopeWithNothingInItAddsNoRoute() = runBlocking {
        // Tidus has a Standard path but nothing on Expert, and there are no edits anywhere.
        repository.setActivation(GridCharacter.TIDUS, stat(GridType.STANDARD).id, true)

        val onExpert = repository.saveCurrentAsRoute(
            "Expert", BuildScope.EDITS_AND_CURRENT, GridCharacter.TIDUS, GridType.EXPERT
        )
        val editsOnly = repository.saveCurrentAsRoute(
            "Edits", BuildScope.EDITS_ONLY, GridCharacter.TIDUS, GridType.STANDARD
        )

        assertNull(onExpert)
        assertNull(editsOnly)
        assertTrue(database.sphereGridRouteDao().snapshot().isEmpty())
        assertNull(repository.exportBuild(BuildScope.EDITS_AND_CURRENT, GridCharacter.TIDUS, GridType.EXPERT))
    }

    @Test
    fun updatingWithAScopeWithNothingInItKeepsTheRoute() = runBlocking {
        repository.setActivation(GridCharacter.TIDUS, stat(GridType.STANDARD).id, true)
        val id = repository.saveCurrentAsRoute(
            "Tidus", BuildScope.EDITS_AND_CURRENT, GridCharacter.TIDUS, GridType.STANDARD
        )
        assertNotNull(id)

        // Yuna has no path and there are no edits, so this would have emptied the route.
        val updated = repository.updateRoute(
            id!!, BuildScope.EDITS_AND_CURRENT, GridCharacter.YUNA, GridType.STANDARD
        )

        assertFalse(updated)
        val route = repository.observeRoutes().first().single()
        assertEquals(id, route.id)
        assertEquals(mapOf(GridCharacter.TIDUS to 1), route.pathCounts)
    }

    @Test
    fun revertingAllEditsTakesReclosedLocksOffEveryPath() = runBlocking {
        val standardLock = locks(GridType.STANDARD).first()
        val expertLock = locks(GridType.EXPERT).first()
        val standardStat = stat(GridType.STANDARD)
        open(standardLock)
        open(expertLock)
        repository.setActivation(GridCharacter.TIDUS, standardLock.id, true)
        repository.setActivation(GridCharacter.TIDUS, standardStat.id, true)
        repository.setActivation(GridCharacter.YUNA, standardLock.id, true)
        repository.setActivation(GridCharacter.YUNA, expertLock.id, true)

        repository.clearOverrides()

        assertTrue(repository.observeOverrides().first().isEmpty())
        // The locks are gates again on both grids; the ordinary node stays on Tidus's path.
        assertEquals(setOf(standardStat.id), path(GridCharacter.TIDUS))
        assertTrue(path(GridCharacter.YUNA).isEmpty())
    }

    @Test
    fun applyingABuildReclosesLocksOnlyForThePathsItLeavesInPlace() = runBlocking {
        val (keptOpen, reclosed) = locks(GridType.STANDARD).take(2)
        val yunaStat = stat(GridType.STANDARD, 0)
        val tidusStat = stat(GridType.STANDARD, 1)
        val expertLock = locks(GridType.EXPERT).first()
        open(keptOpen)
        open(reclosed)
        open(expertLock)
        repository.setActivation(GridCharacter.YUNA, reclosed.id, true)
        repository.setActivation(GridCharacter.YUNA, keptOpen.id, true)
        repository.setActivation(GridCharacter.YUNA, yunaStat.id, true)
        repository.setActivation(GridCharacter.YUNA, expertLock.id, true)

        // Tidus's route opens only one of the two locks and walks through it.
        val build = SphereGridBuild(
            GridType.STANDARD,
            listOf(
                RouteEvent.Edit(keptOpen.id, NodeContent.Empty),
                RouteEvent.Activate(GridCharacter.TIDUS, keptOpen.id),
                RouteEvent.Activate(GridCharacter.TIDUS, tidusStat.id)
            )
        )
        assertTrue(repository.applyBuild(build, GridType.STANDARD).isSuccess)

        assertEquals(setOf(keptOpen.id, tidusStat.id), path(GridCharacter.TIDUS))
        // Yuna loses only the lock the build closed again. The Expert grid is left alone.
        assertEquals(setOf(keptOpen.id, yunaStat.id, expertLock.id), path(GridCharacter.YUNA))
    }
}
