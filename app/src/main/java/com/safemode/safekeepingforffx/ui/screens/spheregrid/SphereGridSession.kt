package com.safemode.safekeepingforffx.ui.screens.spheregrid

import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.PathMode
import com.safemode.safekeepingforffx.data.reference.SphereGridBuild
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * What the player had open in the Sphere Grid Planner, held for as long as the app runs.
 *
 * The planner's ViewModel is cleared when the player backs out of the screen, so anything kept only
 * there - which grid and character were on screen, a planned path, a route being replayed, where
 * each view was panned to - was gone on the way back in. This lives in the app container instead, so
 * leaving the planner and coming back returns to exactly where the player was. It is in memory only:
 * a cold start opens the planner fresh, and progress itself is always in the database regardless.
 *
 * Only the ViewModel writes here, apart from [gridViews], which the canvas updates as it is panned.
 */
class SphereGridSession {
    val gridType = MutableStateFlow(GridType.DEFAULT)
    val character = MutableStateFlow(GridCharacter.DEFAULT)

    /** The path being planned, if any. */
    val pathRequest = MutableStateFlow<PathRequest?>(null)

    /** The mode the next plan starts in: whichever the player last picked. */
    var lastPathMode: PathMode = PathMode.DEFAULT

    /** The saved route being replayed, and the build and row behind it. */
    val routeView = MutableStateFlow<RouteViewState?>(null)
    var openRouteBuild: SphereGridBuild? = null
    var openRouteId: Long? = null

    /** The point-to-point route being stepped through, if any. */
    val pointView = MutableStateFlow<PointRouteViewState?>(null)

    /** The grid and character on screen before [pointView] opened, put back when it closes. */
    var beforePointView: Pair<GridType, GridCharacter>? = null

    /** Each view's pan and zoom, by the keys [gridViewKey], [routeViewKey] and [pointViewKey]. */
    internal val gridViews: MutableMap<String, GridView> = HashMap()
}
