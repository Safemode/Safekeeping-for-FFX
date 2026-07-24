package com.safemode.safekeepingforffx.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.safemode.safekeepingforffx.ui.screens.checklist.ChecklistScreen
import com.safemode.safekeepingforffx.ui.screens.home.HomeScreen
import com.safemode.safekeepingforffx.ui.screens.itemlist.ItemListScreen
import com.safemode.safekeepingforffx.ui.screens.mix.MixCalculatorScreen
import com.safemode.safekeepingforffx.ui.screens.monsterarena.MonsterArenaScreen
import com.safemode.safekeepingforffx.ui.screens.settings.SettingsScreen
import com.safemode.safekeepingforffx.ui.screens.spheregrid.SphereGridScreen

private val checklistDestinations = allDestinations.filterIsInstance<FfxDestination.Checklist>()

/** Optional nav argument naming the item a category should open scrolled to and highlighted. */
const val FOCUS_ARG = "focusId"

/** Optional nav argument carrying a search a screen should open with already applied. */
const val SEARCH_ARG = "searchQuery"

/** The pattern NavHost registers: matches both `alBhed` and `alBhed?focusId=primer_3`. */
private fun checklistRoutePattern(categoryId: String) = "$categoryId?$FOCUS_ARG={$FOCUS_ARG}"

/** The concrete route to navigate to. Without a focus it stays the bare id. */
private fun routeFor(categoryId: String, focusId: String?) =
    if (focusId == null) categoryId else "$categoryId?$FOCUS_ARG=$focusId"

/** As above for the arena's search. Encoded, because item names carry spaces and apostrophes. */
private fun arenaRoute(query: String) =
    "${FfxDestination.MonsterArena.route}?$SEARCH_ARG=${Uri.encode(query)}"

/**
 * Single place that knows how to move between top-level destinations, so the drawer and the Home
 * progress cards can't drift apart on back-stack behaviour.
 *
 * [focusId] carries a search result through to the category screen, which scrolls to that item.
 */
fun NavHostController.navigateToDestination(route: String, focusId: String? = null) {
    // Re-navigating to the screen you are already on is a no-op, unless a focus is being applied -
    // that has to go through even from within the same category, or tapping a search result for
    // the open list would do nothing.
    val currentBase = currentDestination?.route?.substringBefore('?')
    if (focusId == null && route == currentBase) return
    navigate(routeFor(route, focusId)) {
        // Keep the back stack flat: browsing categories shouldn't mean a dozen back presses to
        // leave the app.
        //
        // Deliberately no saveState/restoreState. Those are what carried a category's scroll
        // position across a visit, so leaving one and coming back dropped you mid-list. Without
        // them the destination is rebuilt fresh and starts at the top. Rotation is unaffected:
        // that restores through the activity's saved state, not through here.
        popUpTo(graph.startDestinationId)
        launchSingleTop = true
    }
}

/**
 * Opens the Monster Arena with [query] already searched, for the item list's "what carries this?"
 * tap.
 *
 * Deliberately not [navigateToDestination]: this is a lookup you come back from, so the screen you
 * left stays on the stack and back returns you to it, at the row you tapped. The arena has no such
 * tap of its own, so the stack can only ever grow by this one entry.
 */
fun NavHostController.navigateToArenaSearch(query: String) {
    navigate(arenaRoute(query)) { launchSingleTop = true }
}

@Composable
fun FfxNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    /**
     * Lets a screen claim the back press while it has something to dismiss. Keyed by the screen's
     * route so a screen being torn down can only ever retract its own registration.
     */
    onScreenBackHandlerChange: (key: String, handler: (() -> Unit)?) -> Unit = { _, _ -> }
) {
    NavHost(
        navController = navController,
        startDestination = FfxDestination.Home.route,
        modifier = modifier
    ) {
        composable(FfxDestination.Home.route) {
            HomeScreen(
                categories = checklistDestinations.map { it.category }
                    .filter { it.trackProgress && it.showOnHome },
                // Search covers every category, including the ones with no card on Home.
                searchCategories = checklistDestinations.map { it.category },
                onCategoryClick = { route -> navController.navigateToDestination(route) },
                onResultClick = { categoryId, itemId ->
                    navController.navigateToDestination(categoryId, focusId = itemId)
                },
                onSearchDismissChange = { handler ->
                    onScreenBackHandlerChange(FfxDestination.Home.route, handler)
                }
            )
        }

        composable(FfxDestination.Settings.route) { SettingsScreen() }

        composable(FfxDestination.MixCalculator.route) { MixCalculatorScreen() }

        composable(FfxDestination.SphereGrid.route) { SphereGridScreen() }

        // The optional search argument is how an item list row hands over the item it wants the
        // fiends for. Reached without it, the arena opens unfiltered as always.
        composable(
            route = "${FfxDestination.MonsterArena.route}?$SEARCH_ARG={$SEARCH_ARG}",
            arguments = listOf(
                navArgument(SEARCH_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            MonsterArenaScreen(
                initialQuery = backStackEntry.arguments?.getString(SEARCH_ARG),
                onSearchDismissChange = { handler ->
                    onScreenBackHandlerChange(FfxDestination.MonsterArena.route, handler)
                }
            )
        }

        // Same optional focus argument as the checklists, so a Home search result can jump to a
        // particular item.
        composable(
            route = checklistRoutePattern(FfxDestination.ItemList.route),
            arguments = listOf(
                navArgument(FOCUS_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            ItemListScreen(
                focusItemId = backStackEntry.arguments?.getString(FOCUS_ARG),
                onFindInArena = { itemName -> navController.navigateToArenaSearch(itemName) },
                onSearchDismissChange = { handler ->
                    onScreenBackHandlerChange(FfxDestination.ItemList.route, handler)
                }
            )
        }

        // Every checklist category is the same screen with different data.
        checklistDestinations.forEach { destination ->
            composable(
                route = checklistRoutePattern(destination.route),
                arguments = listOf(
                    navArgument(FOCUS_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                ChecklistScreen(
                    category = destination.category,
                    focusItemId = backStackEntry.arguments?.getString(FOCUS_ARG),
                    onSearchDismissChange = { handler ->
                        onScreenBackHandlerChange(destination.route, handler)
                    }
                )
            }
        }
    }
}
