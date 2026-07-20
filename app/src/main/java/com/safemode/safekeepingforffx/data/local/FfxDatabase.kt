package com.safemode.safekeepingforffx.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChecklistProgressEntity::class, MonsterCaptureEntity::class],
    version = 2,
    exportSchema = false
)
abstract class FfxDatabase : RoomDatabase() {

    abstract fun checklistProgressDao(): ChecklistProgressDao

    abstract fun monsterCaptureDao(): MonsterCaptureDao

    companion object {
        /**
         * Monster Arena tracks a count, not a tick, so it could not reuse `checklist_progress`.
         * Additive only: the existing table is untouched, so nobody loses their checkmarks on
         * update. Destructive fallback is deliberately not enabled - a failed migration should be
         * loud, not silently wipe the player's progress.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `monster_capture` (" +
                        "`monsterId` TEXT NOT NULL, " +
                        "`count` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`monsterId`))"
                )
            }
        }

        @Volatile
        private var instance: FfxDatabase? = null

        fun getInstance(context: Context): FfxDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FfxDatabase::class.java,
                    "ffx_tracker.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
