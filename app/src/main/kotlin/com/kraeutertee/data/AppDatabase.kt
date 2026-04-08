package com.kraeutertee.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kraeutertee.data.dao.*
import com.kraeutertee.data.entities.*

@Database(
    entities = [HerbNote::class, HerbLocation::class, HarvestReminder::class,
                DryingEntry::class, Recipe::class, CustomHerbEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun herbNoteDao(): HerbNoteDao
    abstract fun herbLocationDao(): HerbLocationDao
    abstract fun harvestReminderDao(): HarvestReminderDao
    abstract fun dryingEntryDao(): DryingEntryDao
    abstract fun recipeDao(): RecipeDao
    abstract fun customHerbDao(): CustomHerbDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kraeutertee.db"
                )
                .fallbackToDestructiveMigration()   // personal app – OK to rebuild on version bump
                .build().also { INSTANCE = it }
            }
    }
}
