package com.safemode.safekeepingforffx.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
import com.safemode.safekeepingforffx.data.reference.AlBhedPrimers
import com.safemode.safekeepingforffx.data.reference.BlitzballKeyTechs
import com.safemode.safekeepingforffx.data.reference.BlitzballRecruits
import com.safemode.safekeepingforffx.data.reference.CelestialWeapons
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.data.reference.EquipmentAbilities
import com.safemode.safekeepingforffx.data.reference.ItemListCsvParser
import com.safemode.safekeepingforffx.data.reference.MONSTER_ARENA_ID
import com.safemode.safekeepingforffx.data.reference.MONSTER_ARENA_LABEL
import com.safemode.safekeepingforffx.data.reference.JechtSpheres
import com.safemode.safekeepingforffx.data.reference.RonsoRages

/**
 * Everything the drawer needs to know about a screen.
 *
 * Adding a future tracker category is now a data change: write the reference object, then add one
 * [Checklist] entry to [drawerDestinations]. The NavHost picks it up automatically.
 */
sealed class FfxDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : FfxDestination("home", "Home", Icons.Filled.Home)

    data object Settings : FfxDestination("settings", "Settings", Icons.Filled.Settings)

    /**
     * Informational only - a lookup tool rather than a list, so it is deliberately not a
     * [Checklist] and has nothing to track.
     */
    data object MixCalculator :
        FfxDestination("mix_calculator", "Mix Calculator", Icons.Filled.Science)

    /**
     * A reference category like any other, but its contents come from a CSV asset rather than from
     * Kotlin, so it cannot be a [Checklist] built at class-init time.
     */
    data object ItemList :
        FfxDestination(ItemListCsvParser.CATEGORY_ID, ItemListCsvParser.LABEL, Icons.Filled.Inventory2)

    /**
     * Tracks a count per fiend rather than a tick per item, so it has its own screen and its own
     * table rather than riding on the checklist machinery.
     */
    data object MonsterArena :
        FfxDestination(MONSTER_ARENA_ID, MONSTER_ARENA_LABEL, Icons.Filled.Pets)

    class Checklist(
        val category: ChecklistCategory,
        icon: ImageVector
    ) : FfxDestination(category.id, category.label, icon)
}

/** The main body of the drawer. */
val drawerDestinations: List<FfxDestination> = listOf(
    FfxDestination.Home,
    FfxDestination.Checklist(AlBhedPrimers.category, Icons.Filled.Translate),
    FfxDestination.Checklist(JechtSpheres.category, Icons.Filled.Movie),
    FfxDestination.Checklist(CelestialWeapons.category, Icons.Filled.AutoAwesome),
    FfxDestination.Checklist(RonsoRages.category, Icons.Filled.Whatshot),
    FfxDestination.Checklist(BlitzballKeyTechs.category, Icons.Filled.SportsSoccer),
    FfxDestination.Checklist(BlitzballRecruits.category, Icons.Filled.Groups),
    FfxDestination.Checklist(EquipmentAbilities.category, Icons.Filled.Build),
    FfxDestination.ItemList,
    FfxDestination.MonsterArena,
    FfxDestination.MixCalculator
)

/** Pinned to the bottom of the drawer, below a divider. */
val settingsDestination = FfxDestination.Settings

val allDestinations: List<FfxDestination> = drawerDestinations + settingsDestination

/**
 * Checklist routes carry an optional `?focusId=` argument, so the pattern NavHost reports back is
 * not the bare category id. Everything before the `?` is the destination.
 */
fun destinationForRoute(route: String?): FfxDestination {
    val base = route?.substringBefore('?')
    return allDestinations.firstOrNull { it.route == base } ?: FfxDestination.Home
}
