package com.safemode.safekeepingforffx.ui.screens.checklist

import com.safemode.safekeepingforffx.data.reference.GridCharacter
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Which character each per-character list (Overdrive Modes) is showing, held for as long as the app
 * runs. A list's ViewModel is cleared when the player leaves it, so a character kept only there went
 * back to Tidus on every visit, while the Sphere Grid Planner remembered whoever was picked. Kept in
 * the app container instead, the same way, and in memory only: a cold start opens on the default.
 */
class ChecklistCharacterMemory {

    private val selected = ConcurrentHashMap<String, MutableStateFlow<GridCharacter>>()

    /** The selection for [categoryId], shared by every ViewModel that list gets. */
    fun forCategory(categoryId: String): MutableStateFlow<GridCharacter> =
        selected.getOrPut(categoryId) { MutableStateFlow(GridCharacter.DEFAULT) }
}
