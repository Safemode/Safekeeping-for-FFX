package com.safemode.safekeepingforffx.ui.screens.missables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.safemode.safekeepingforffx.FfxApplication
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.data.reference.GameVersion
import com.safemode.safekeepingforffx.data.repository.ChecklistRepository
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import com.safemode.safekeepingforffx.domain.ChecklistItem
import com.safemode.safekeepingforffx.domain.forVersion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** One flagged item on the timeline, tagged with the list it actually lives in. */
data class TimelineItem(
    val categoryId: String,
    val categoryLabel: String,
    val item: ChecklistItem
)

/** A story point and everything flagged that becomes reachable at it. */
data class TimelineStage(
    val label: String,
    val items: List<TimelineItem>
)

data class MissablesTimelineUiState(
    val stages: List<TimelineStage> = emptyList(),
    val totalCount: Int = 0,
    val collectedCount: Int = 0,
    val showHelp: Boolean = true,
    val isLoading: Boolean = true
) {
    /** Nothing flagged for this version - the whole list is empty rather than mid-load. */
    val isEmpty: Boolean get() = !isLoading && stages.isEmpty()
}

/** The story point that holds everything with no stage set - kept last. */
const val UNSTAGED_LABEL = "Not tied to a stage"

/**
 * Pure transform behind the timeline, factored out of the ViewModel so it can be tested without a
 * database or Android at all: given the flagged items and the game version, produce the stage
 * groups in story order.
 *
 * Applies the version first, then keeps only what still carries a caution - so a Guarded item on the
 * PS2 release, whose caution [forVersion] strips, correctly drops out here.
 */
fun buildTimelineStages(items: List<TimelineItem>, version: GameVersion): List<TimelineStage> {
    val flagged = items
        .map { it.copy(item = it.item.forVersion(version)) }
        .filter { it.item.caution != null }

    val ordered = flagged.sortedWith(
        compareBy(
            { it.item.storyStage?.ordinal ?: Int.MAX_VALUE },
            { it.categoryLabel },
            { it.item.title }
        )
    )

    // groupBy keeps first-seen order, and the list is already in stage order, so the unstaged bucket
    // (storyStage == null, sorted last) lands at the bottom on its own.
    return ordered
        .groupBy { it.item.storyStage }
        .map { (stage, entries) -> TimelineStage(stage?.label ?: UNSTAGED_LABEL, entries) }
}

/**
 * Gathers everything the other checklists have flagged - permanently [com.safemode.safekeepingforffx
 * .data.reference.Caution.Missable] or Dark-Aeon-guarded - into one view, ordered by when in the
 * story it first becomes reachable. It stores nothing of its own: the checked state is read straight
 * from each source list through [ChecklistRepository], so ticking an item off in its own list shows
 * up here too, and the reverse.
 *
 * Guarded items drop out on the original PS2 release for the same reason they do everywhere else in
 * the app - no Dark Aeons there - via [ChecklistItem.forVersion], so this list is honest per version.
 */
class MissablesTimelineViewModel(
    checklistRepository: ChecklistRepository,
    settingsRepository: SettingsRepository,
    categories: List<ChecklistCategory>
) : ViewModel() {

    /**
     * Every source category's live progress, folded into one flat list tagged with its origin. The
     * per-category flows are combined rather than read once so a check ticked anywhere updates here.
     */
    private val sourcedItems: Flow<List<TimelineItem>> = combine(
        categories.map { category ->
            checklistRepository.observeCategory(category.id, category.items).map { items ->
                items.map { TimelineItem(category.id, category.label, it) }
            }
        }
    ) { perCategory -> perCategory.toList().flatten() }

    val uiState = combine(
        sourcedItems,
        settingsRepository.gameVersion,
        settingsRepository.showHelp
    ) { items, version, showHelp ->
        val stages = buildTimelineStages(items, version)
        MissablesTimelineUiState(
            stages = stages,
            totalCount = stages.sumOf { it.items.size },
            collectedCount = stages.sumOf { stage -> stage.items.count { it.item.isChecked } },
            showHelp = showHelp,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MissablesTimelineUiState()
    )

    companion object {
        fun factory(categories: List<ChecklistCategory>) = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FfxApplication
                MissablesTimelineViewModel(
                    app.container.checklistRepository,
                    app.container.settingsRepository,
                    categories
                )
            }
        }
    }
}
