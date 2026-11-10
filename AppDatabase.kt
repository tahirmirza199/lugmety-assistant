package com.lugmety.orderassistant.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lugmety.orderassistant.data.local.dao.ClickProfileDao
import com.lugmety.orderassistant.data.local.dao.LogDao
import com.lugmety.orderassistant.data.local.dao.OrderDao
import com.lugmety.orderassistant.data.local.entity.ClickPointEntity
import com.lugmety.orderassistant.data.local.entity.ClickProfileEntity
import com.lugmety.orderassistant.data.local.entity.DiagnosticLogEntity
import com.lugmety.orderassistant.data.local.entity.OrderEventEntity

@Database(
    entities = [
        OrderEventEntity::class,
        DiagnosticLogEntity::class,
        ClickProfileEntity::class,
        ClickPointEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun logDao(): LogDao
    abstract fun clickProfileDao(): ClickProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from Version 1 to 2:
         * Adds click_profiles and click_points tables without deleting or touching existing
         * order_events or diagnostic_logs tables. Ensures 100% data safety.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `click_profiles` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `intervalMs` INTEGER NOT NULL,
                        `gestureDurationMs` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `click_points` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `profileId` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `x` REAL NOT NULL,
                        `y` REAL NOT NULL,
                        `delayMs` INTEGER NOT NULL,
                        `label` TEXT NOT NULL,
                        FOREIGN KEY(`profileId`) REFERENCES `click_profiles`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_click_points_profileId` ON `click_points` (`profileId`)
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lugmety_assistant.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
