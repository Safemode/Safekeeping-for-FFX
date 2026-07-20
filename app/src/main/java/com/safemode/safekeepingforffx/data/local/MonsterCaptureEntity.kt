package com.safemode.safekeepingforffx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * How many of one fiend the player has captured, 0 to 10.
 *
 * This needs its own table rather than a row in `checklist_progress`, because that table records a
 * boolean and this is a count. Rows only exist once a fiend has been captured at least once;
 * absent means zero.
 */
@Entity(tableName = "monster_capture")
data class MonsterCaptureEntity(
    @PrimaryKey val monsterId: String,
    val count: Int,
    val updatedAt: Long
)
