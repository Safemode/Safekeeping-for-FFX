package com.safemode.safekeepingforffx.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
import com.safemode.safekeepingforffx.data.reference.Aeons
import com.safemode.safekeepingforffx.data.reference.AlBhedPrimers
import com.safemode.safekeepingforffx.data.reference.BlitzballKeyTechs
import com.safemode.safekeepingforffx.data.reference.BlitzballRecruits
import com.safemode.safekeepingforffx.data.reference.CelestialWeapons
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.data.reference.DarkAeonsAndPenance
import com.safemode.safekeepingforffx.data.reference.DestructionSpheres
import com.safemode.safekeepingforffx.data.reference.EquipmentAbilities
import com.safemode.safekeepingforffx.data.reference.ItemListCsvParser
import com.safemode.safekeepingforffx.data.reference.MONSTER_ARENA_ID
import com.safemode.safekeepingforffx.data.reference.MONSTER_ARENA_LABEL
import com.safemode.safekeepingforffx.data.reference.JechtSpheres
import com.safemode.safekeepingforffx.data.reference.OverdriveModes
import com.safemode.safekeepingforffx.data.reference.RonsoRages
import com.safemode.safekeepingforffx.data.reference.SPHERE_GRID_ID
import com.safemode.safekeepingforffx.data.reference.SPHERE_GRID_LABEL
import com.safemode.safekeepingforffx.data.reference.Trophies

/**
 * Everything the drawer needs to know about a screen.
 *
 * Adding a future tracker category is now a data change: write the reference object, then add one
 * [Checklist] entry to a group in [drawerLayout]. The NavHost picks it up automatically.
 */
sealed class FfxDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : FfxDestination("home", "Home", Icons.Filled.Home)

    /**
     * Not a list of its own - it shows what has been starred in the other lists, so it owns no
     * reference data and nothing can be starred here that wasn't starred somewhere else first.
     */
    data object Favorites : FfxDestination("favorites", "Favorites", Icons.Filled.Star)

    data object Settings : FfxDestination("settings", "Settings", Icons.Filled.Settings)

    /**
     * A cross-cutting read-only view rather than a list of its own: it gathers every item the other
     * lists have flagged missable or Dark-Aeon-guarded, in story order, and links back to each one
     * where it lives. Like [Favorites] it owns no reference data.
     */
    data object MissablesTimeline :
        FfxDestination("missables_timeline", "Missables Timeline", Icons.Filled.Timeline)

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

    /**
     * A spatial planner rather than a list: its own pan/zoom screen and its own table, so like
     * [MonsterArena] it isn't a [Checklist].
     */
    data object SphereGrid :
        FfxDestination(SPHERE_GRID_ID, SPHERE_GRID_LABEL, Icons.Filled.Hub)

    class Checklist(
        val category: ChecklistCategory,
        icon: ImageVector
    ) : FfxDestination(category.id, category.label, icon)
}

/** One line of the drawer: a screen on its own, or a collapsible group of related screens. */
sealed interface DrawerEntry {

    data class Single(val destination: FfxDestination) : DrawerEntry

    data class Group(
        /** Stable key for remembering which groups are open and for looking up their count. */
        val id: String,
        val label: String,
        val icon: ImageVector,
        val destinations: List<FfxDestination>,
        /** False for a group of tools and lookups, which has nothing to count. */
        val showProgress: Boolean = true
    ) : DrawerEntry
}

/**
 * The main body of the drawer, grouped by game system. The cross-cutting views that draw on every
 * list stay ungrouped at the top; everything else sits in a group that starts collapsed.
 */
val drawerLayout: List<DrawerEntry> = listOf(
    DrawerEntry.Single(FfxDestination.Home),
    DrawerEntry.Single(FfxDestination.Favorites),
    DrawerEntry.Single(FfxDestination.MissablesTimeline),
    DrawerEntry.Group(
        id = "collectibles",
        label = "Collectibles",
        icon = Icons.Filled.CollectionsBookmark,
        destinations = listOf(
            FfxDestination.Checklist(AlBhedPrimers.category, Icons.Filled.Translate),
            FfxDestination.Checklist(JechtSpheres.category, Icons.Filled.Movie),
            FfxDestination.Checklist(DestructionSpheres.category, Icons.Filled.Adjust)
        )
    ),
    DrawerEntry.Group(
        id = "party_aeons",
        label = "Party & Aeons",
        icon = Icons.Filled.Shield,
        destinations = listOf(
            FfxDestination.Checklist(Aeons.category, Icons.Filled.Flare),
            FfxDestination.Checklist(CelestialWeapons.category, Icons.Filled.AutoAwesome),
            FfxDestination.Checklist(OverdriveModes.category, Icons.Filled.Bolt),
            FfxDestination.Checklist(RonsoRages.category, Icons.Filled.Whatshot)
        )
    ),
    DrawerEntry.Group(
        id = "blitzball",
        label = "Blitzball",
        icon = Icons.Filled.SportsHandball,
        destinations = listOf(
            FfxDestination.Checklist(BlitzballKeyTechs.category, Icons.Filled.SportsSoccer),
            FfxDestination.Checklist(BlitzballRecruits.category, Icons.Filled.Groups)
        )
    ),
    DrawerEntry.Group(
        id = "endgame",
        label = "Endgame",
        icon = Icons.Filled.MilitaryTech,
        destinations = listOf(
            FfxDestination.MonsterArena,
            FfxDestination.Checklist(DarkAeonsAndPenance.category, Icons.Filled.DarkMode),
            FfxDestination.Checklist(Trophies.category, Icons.Filled.EmojiEvents)
        )
    ),
    DrawerEntry.Group(
        id = "tools_reference",
        label = "Tools & Reference",
        icon = Icons.Filled.Handyman,
        destinations = listOf(
            FfxDestination.SphereGrid,
            FfxDestination.MixCalculator,
            FfxDestination.ItemList,
            FfxDestination.Checklist(EquipmentAbilities.category, Icons.Filled.Build)
        ),
        showProgress = false
    )
)

/**
 * Every drawer screen in the order the drawer reads, groups flattened. Favorites orders its sections
 * by this, and the NavHost registers routes from it, so neither has to know the drawer is grouped.
 */
val drawerDestinations: List<FfxDestination> = drawerLayout.flatMap { entry ->
    when (entry) {
        is DrawerEntry.Single -> listOf(entry.destination)
        is DrawerEntry.Group -> entry.destinations
    }
}

/** Pinned to the bottom of the drawer, below a divider. */
val settingsDestination = FfxDestination.Settings

val allDestinations: List<FfxDestination> = drawerDestinations + settingsDestination

/**
 * Where Favorites draws from.
 *
 * Defined by what it leaves out rather than by listing the lists, so a category added to the drawer
 * later can be starred without anyone remembering to name it here twice. The exclusions are the
 * screens with no items to star: Home and Settings aren't lists, the Sphere Grid Planner is a canvas
 * rather than a list of things, the Mix Calculator is a lookup table, and Favorites itself holds
 * nothing of its own.
 */
object favoriteSources {

    private val excluded = setOf(
        FfxDestination.Home,
        FfxDestination.Favorites,
        FfxDestination.MissablesTimeline,
        FfxDestination.Settings,
        FfxDestination.SphereGrid,
        FfxDestination.MixCalculator
    )

    /**
     * Category id to the name Favorites groups it under, in drawer order - which is the order the
     * groups read down the Favorites screen.
     */
    val ordered: List<Pair<String, String>> = drawerDestinations
        .filterNot { it in excluded }
        .map { it.route to it.label }

    /**
     * The compiled-in categories among them, for looking a starred item back up. The item list and
     * the Monster Arena are absent on purpose: their contents are parsed from CSV at runtime, so
     * only a repository can produce them.
     */
    val categories: List<ChecklistCategory> = drawerDestinations
        .filterIsInstance<FfxDestination.Checklist>()
        .map { it.category }
}

/**
 * Checklist routes carry an optional `?focusId=` argument, so the pattern NavHost reports back is
 * not the bare category id. Everything before the `?` is the destination.
 */
fun destinationForRoute(route: String?): FfxDestination {
    val base = route?.substringBefore('?')
    return allDestinations.firstOrNull { it.route == base } ?: FfxDestination.Home
}
