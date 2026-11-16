package com.safemode.safekeepingforffx

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.safemode.safekeepingforffx.data.local.FfxDatabase
import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridPath
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.local.SphereGridRouteEntity
import com.safemode.safekeepingforffx.data.reference.PathMode
import com.safemode.safekeepingforffx.data.reference.RouteEvent
import com.safemode.safekeepingforffx.data.reference.SphereGridBuild
import com.safemode.safekeepingforffx.data.reference.SphereGridBuildCodec
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import com.safemode.safekeepingforffx.data.repository.SphereGridRepository
import com.safemode.safekeepingforffx.ui.screens.spheregrid.SphereGridSession
import com.safemode.safekeepingforffx.ui.screens.spheregrid.SphereGridViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Backing out of the planner clears its ViewModel. The session it reads from outlives that, so a new
 * ViewModel - the one built when the player comes back in - opens exactly where they were.
 */
@RunWith(AndroidJUnit4::class)
class SphereGridSessionTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = Room.inMemoryDatabaseBuilder(context, FfxDatabase::class.java).build()
    private val settingsFile = File(context.cacheDir, "session-test.preferences_pb").apply { delete() }
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
        pointRouteDao = database.pointRouteDao(),
        settingsRepository = settings
    )

    @After
    fun tearDown() {
        database.close()
        settingsScope.cancel()
        settingsFile.delete()
    }

    @Test
    fun comingBackInFindsThePlannerWhereItWasLeft() = runBlocking {
        val session = SphereGridSession()
        val target = repository.grid(GridType.EXPERT).nodes[10]

        val before = SphereGridViewModel(repository, settings, session)
        before.setGridType(GridType.EXPERT)
        before.setCharacter(GridCharacter.LULU)
        before.planPathTo(target)
        before.setPathMode(PathMode.NO_LOCKS)

        val after = SphereGridViewModel(repository, settings, session)
        val state = withTimeout(10_000) {
            after.uiState.first { !it.isLoading && it.gridType == GridType.EXPERT }
        }
        assertEquals(GridCharacter.LULU, state.character)
        val plan = withTimeout(10_000) { after.pathPlan.first { it != null } }!!
        assertEquals(target.id, plan.request.toId)
        assertEquals(PathMode.NO_LOCKS, plan.request.mode)
    }

    @Test
    fun aRouteBeingSteppedThroughIsStillOpenOnTheWayBackIn() = runBlocking {
        val session = SphereGridSession()
        val nodes = repository.grid(GridType.STANDARD).nodes
        val path = GridPath(listOf(nodes[0].id, nodes[1].id), 4, emptyList())
        val id = repository.savePointRoute("Short", GridType.STANDARD, GridCharacter.AURON, PathMode.SHORTEST, path)

        val before = SphereGridViewModel(repository, settings, session)
        before.viewPointRoute(id)
        withTimeout(10_000) { before.pointView.first { it != null } }
        before.setPointStep(1)

        val after = SphereGridViewModel(repository, settings, session)
        val view = after.pointView.value!!
        assertEquals(id, view.routeId)
        assertEquals(1, view.stepIndex)
        assertEquals(GridCharacter.AURON, withTimeout(10_000) { after.uiState.first { !it.isLoading } }.character)
    }

    @Test
    fun closingAStepThroughReturnsToTheGridItWasOpenedFrom() = runBlocking {
        val session = SphereGridSession()
        val nodes = repository.grid(GridType.STANDARD).nodes
        val path = GridPath(listOf(nodes[0].id, nodes[1].id), 4, emptyList())
        val id = repository.savePointRoute("Short", GridType.STANDARD, GridCharacter.AURON, PathMode.SHORTEST, path)

        val vm = SphereGridViewModel(repository, settings, session)
        vm.setGridType(GridType.EXPERT)
        vm.setCharacter(GridCharacter.YUNA)
        vm.viewPointRoute(id)
        withTimeout(10_000) { vm.pointView.first { it != null } }
        assertEquals(GridType.STANDARD, session.gridType.value)
        assertEquals(GridCharacter.AURON, session.character.value)

        vm.exitPointView()

        assertEquals(null, vm.pointView.value)
        assertEquals(GridType.EXPERT, session.gridType.value)
        assertEquals(GridCharacter.YUNA, session.character.value)
    }

    private suspend fun saveStandardRoute(): Long {
        val first = repository.grid(GridType.STANDARD).nodes.first().id
        val build = SphereGridBuild(
            GridType.STANDARD,
            events = listOf(RouteEvent.Activate(GridCharacter.TIDUS, first)),
            name = "Opener"
        )
        return database.sphereGridRouteDao().insert(
            SphereGridRouteEntity(
                name = "Opener", gridType = "STANDARD", createdAt = 1, updatedAt = 1,
                payload = SphereGridBuildCodec.encode(build)
            )
        )
    }

    @Test
    fun closingARouteReplayReturnsToTheGridItWasOpenedFrom() = runBlocking {
        val id = saveStandardRoute()
        val session = SphereGridSession()
        val vm = SphereGridViewModel(repository, settings, session)
        vm.setGridType(GridType.EXPERT)

        vm.enterRouteView(id)
        withTimeout(10_000) { vm.routeView.first { it != null } }
        assertEquals(GridType.STANDARD, session.gridType.value)

        vm.exitRouteView()

        assertEquals(null, vm.routeView.value)
        assertEquals(GridType.EXPERT, session.gridType.value)
    }

    @Test
    fun adoptingARouteStaysOnItsGrid() = runBlocking {
        val id = saveStandardRoute()
        val session = SphereGridSession()
        val vm = SphereGridViewModel(repository, settings, session)
        vm.setGridType(GridType.EXPERT)
        vm.enterRouteView(id)
        withTimeout(10_000) { vm.routeView.first { it != null } }

        vm.applyRouteToProgress()
        withTimeout(10_000) { vm.routeView.first { it == null } }

        // The adopted path is on the Standard grid, so that is where the player is left.
        assertEquals(GridType.STANDARD, session.gridType.value)
    }
}
