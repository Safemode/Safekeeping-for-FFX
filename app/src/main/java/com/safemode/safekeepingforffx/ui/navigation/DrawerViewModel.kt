package com.safemode.safekeepingforffx.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.safemode.safekeepingforffx.FfxApplication
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.data.reference.GameVersion
import com.safemode.safekeepingforffx.data.repository.ChecklistRepository
import com.safemode.safekeepingforffx.data.repository.MonsterArenaRepository
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import com.safemode.safekeepingforffx.domain.ProgressCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** One list's count, tagged with the drawer group it rolls up into. */
internal data class GroupCount(
    val groupId: String,
    /** The checklist behind the count, or null for the Monster Arena, which exists on every release. */
    val category: ChecklistCategory?,
    val count: ProgressCount
)

/**
 * Adds up each group's lists, leaving out any list [version] doesn't have. That is the same rule
 * Home applies, so a group header and the Home total never disagree about what counts.
 */
internal fun sumByGroup(counts: List<GroupCount>, version: GameVersion): Map<String, ProgressCount> =
    counts
        .filter { it.category?.isAvailableOn(version) ?: true }
        .groupBy({ it.groupId }, { it.count })
        .mapValues { (_, group) -> group.reduce(ProgressCount::plus) }

/**
 * The overall progress shown on each drawer group's header. Lives beside the drawer rather than in
 * any one screen's ViewModel, since the drawer outlives every screen it opens.
 *
 * Counted the same way Home counts: every character's copy of a per-character list, fully captured
 * fiends for the Monster Arena, and nothing for reference lists that aren't tracked.
 */
class DrawerViewModel(
    checklistRepository: ChecklistRepository,
    monsterArenaRepository: MonsterArenaRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private class Source(
        val groupId: String,
        val category: ChecklistCategory?,
        val progress: Flow<ProgressCount>
    )

    /** Every counted list in a counted group, in drawer order. */
    private val sources: List<Source> = drawerLayout
        .filterIsInstance<DrawerEntry.Group>()
        .filter { it.showProgress }
        .flatMap { group ->
            group.destinations.mapNotNull { destination ->
                when (destination) {
                    is FfxDestination.Checklist -> destination.category
                        .takeIf { it.trackProgress }
                        ?.let { Source(group.id, it, checklistRepository.observeProgress(it)) }

                    FfxDestination.MonsterArena ->
                        Source(group.id, null, monsterArenaRepository.observeCaptureProgress())

                    else -> null
                }
            }
        }

    /** Group id to its summed count. Empty until the first counts arrive, which hides the numbers. */
    val groupProgress: StateFlow<Map<String, ProgressCount>> = combine(
        combine(sources.map { it.progress }) { it.toList() },
        settingsRepository.gameVersion
    ) { counts, version ->
        sumByGroup(
            sources.zip(counts) { source, count -> GroupCount(source.groupId, source.category, count) },
            version
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap()
    )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FfxApplication
                DrawerViewModel(
                    app.container.checklistRepository,
                    app.container.monsterArenaRepository,
                    app.container.settingsRepository
                )
            }
        }
    }
}
