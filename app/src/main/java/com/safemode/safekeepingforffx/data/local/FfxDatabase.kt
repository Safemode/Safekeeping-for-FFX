package com.safemode.safekeepingforffx.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ChecklistProgressEntity::class,
        MonsterCaptureEntity::class,
        SphereGridNodeEntity::class,
        SphereGridActivationEntity::class,
        SphereGridRouteEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class FfxDatabase : RoomDatabase() {

    abstract fun checklistProgressDao(): ChecklistProgressDao

    abstract fun monsterCaptureDao(): MonsterCaptureDao

    abstract fun sphereGridNodeDao(): SphereGridNodeDao

    abstract fun sphereGridActivationDao(): SphereGridActivationDao

    abstract fun sphereGridRouteDao(): SphereGridRouteDao

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

        /**
         * Adds the Sphere Grid Planner's node table. Additive, same as [MIGRATION_1_2]: existing
         * checklist and capture data is untouched, so an update never costs the player progress.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sphere_grid_node` (" +
                        "`nodeId` TEXT NOT NULL, " +
                        "`activatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`nodeId`))"
                )
            }
        }

        /**
         * The Sphere Grid Planner moved from tracking a boolean "activated" flag per node to storing
         * the player's content edits. The old column has no meaning under the new model, so the
         * table is rebuilt with the new schema. This only ever discards the previous grid feature's
         * data - which never shipped in a release - and leaves the checklist and capture tables
         * untouched.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `sphere_grid_node`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sphere_grid_node` (" +
                        "`nodeId` TEXT NOT NULL, " +
                        "`content` TEXT NOT NULL, " +
                        "PRIMARY KEY(`nodeId`))"
                )
            }
        }

        /**
         * Adds per-character path tracking for the Sphere Grid Planner. Additive: the shared node
         * edits table is untouched, so existing customizations survive the update.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sphere_grid_activation` (" +
                        "`character` TEXT NOT NULL, " +
                        "`nodeId` TEXT NOT NULL, " +
                        "PRIMARY KEY(`character`, `nodeId`))"
                )
            }
        }

        /**
         * Adds Sphere Grid Routes: a `seq` ordering column on the activation and edit tables (so a
         * route can replay in the order it was walked) and the saved-routes library table. Additive:
         * existing activations and edits keep their data and default to `seq = 0` ("order unknown"),
         * so nobody loses progress on update.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `sphere_grid_activation` ADD COLUMN `seq` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `sphere_grid_node` ADD COLUMN `seq` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sphere_grid_route` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`gridType` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`payload` TEXT NOT NULL)"
                )
            }
        }

        /**
         * Backfills the routes `seq` from each row's rowid (insertion order). Activations and edits
         * that predate the routes feature all defaulted to `seq = 0`, so they had no timeline and a
         * saved route replayed them in arbitrary order. rowid tracks insertion order, which is the
         * order the player actually took them, so this gives those rows a real, ordered timeline (and
         * also repairs any rows scrambled by the since-fixed seq race). Data-only: the schema is
         * unchanged from v6.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE `sphere_grid_activation` SET `seq` = `rowid`")
                db.execSQL("UPDATE `sphere_grid_node` SET `seq` = `rowid`")
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
                ).addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                    MIGRATION_6_7
                ).build().also { instance = it }
            }
    }
}
