package com.safemode.safekeepingforffx.data.repository

import com.safemode.safekeepingforffx.data.local.ChecklistProgressDao
import com.safemode.safekeepingforffx.data.local.ChecklistProgressEntity
import com.safemode.safekeepingforffx.data.reference.ReferenceItem
import com.safemode.safekeepingforffx.domain.ChecklistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Joins compiled-in reference data with stored progress. The join happens in Kotlin rather than
 * SQL because the two halves deliberately live in different places - see [ChecklistProgressEntity].
 */
class ChecklistRepository(private val dao: ChecklistProgressDao) {

    fun observeCategory(
        categoryId: String,
        reference: List<ReferenceItem>
    ): Flow<List<ChecklistItem>> =
        dao.observeCategory(categoryId).map { rows ->
            val checked = rows.filter { it.isChecked }.mapTo(HashSet()) { it.itemId }
            reference.map { item ->
                ChecklistItem(
                    id = item.id,
                    title = item.title,
                    location = item.location,
                    detail = item.detail,
                    caution = item.caution,
                    isChecked = item.id in checked,
                    section = item.section,
                    tag = item.tag,
                    imageRes = item.imageRes,
                    storyStage = item.storyStage,
                    stageNote = item.stageNote
                )
            }
        }

    suspend fun setChecked(categoryId: String, itemId: String, checked: Boolean) {
        dao.upsert(
            ChecklistProgressEntity(
                categoryId = categoryId,
                itemId = itemId,
                isChecked = checked,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearCategory(categoryId: String) = dao.clearCategory(categoryId)

    /**
     * Wipes every checked item in every category. Irreversible - callers must confirm with the
     * user first. Only touches progress; settings live in DataStore and are unaffected.
     */
    suspend fun clearAllProgress() = dao.clearAll()
}
