@file:OptIn(ExperimentalSerializationApi::class)

package com.ybhgl.reminder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.serialization.ExperimentalSerializationApi
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ReminderItem::class, TagItem::class], version = 10, exportSchema = false)
@TypeConverters(com.ybhgl.reminder.data.TypeConverters::class)
abstract class ReminderDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN repeatInfo TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN notificationConfig TEXT NOT NULL DEFAULT '{\"isEnabled\":false,\"useAppNotification\":true,\"useSystemCalendar\":false,\"isContinuous\":false,\"notificationTimes\":[]}'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. 创建 tags 表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tags` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `color` TEXT NOT NULL DEFAULT '#2196F3', 
                        `sortOrder` INTEGER NOT NULL DEFAULT 0
                    )
                """)
                // 2. 创建索引
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")
                
                // 3. 将现存 reminders 去重 category 作为初始数据导入
                db.execSQL("""
                    INSERT OR IGNORE INTO `tags` (name, color, sortOrder)
                    SELECT DISTINCT category, '#2196F3', 0 
                    FROM reminders 
                    WHERE category IS NOT NULL AND category != ''
                """)
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders RENAME COLUMN category TO tag")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN isCustomized INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN customHeaderColor TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reminders ADD COLUMN customFont TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN cardBackgroundType TEXT NOT NULL DEFAULT 'DEFAULT'")
                db.execSQL("ALTER TABLE reminders ADD COLUMN cardBackgroundColor TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reminders ADD COLUMN cardBackgroundImagePath TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reminders ADD COLUMN cardBackgroundBlurRadius REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN cardBackgroundGlassEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN cardBackgroundGlassFrosted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN cardBackgroundGlassDensity REAL NOT NULL DEFAULT 0.5")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN cardBackgroundTextColor TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN cardBackgroundGlassRefraction REAL NOT NULL DEFAULT 0.24")
                db.execSQL("ALTER TABLE reminders ADD COLUMN cardBackgroundGlassTransparency REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN cardBackgroundGlassBlur REAL NOT NULL DEFAULT 12.0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN customFontEffect TEXT NOT NULL DEFAULT 'AUTO'")
                db.execSQL("ALTER TABLE reminders ADD COLUMN customFontColor TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reminders ADD COLUMN customFontOpacity REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN customFontBlur REAL NOT NULL DEFAULT 8.0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN customFontGlassRefraction REAL NOT NULL DEFAULT 0.24")
                db.execSQL("ALTER TABLE reminders ADD COLUMN customFontGlassTransparency REAL NOT NULL DEFAULT 0.7")
                db.execSQL("ALTER TABLE reminders ADD COLUMN customFontGlassBlur REAL NOT NULL DEFAULT 4.0")
            }
        }

        fun getDatabase(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminder_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
