package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MatchEntity::class, TeamRosterEntity::class],
    version = 2,
    exportSchema = false
)
abstract class RoketRoDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun rosterDao(): TeamRosterDao

    companion object {
        @Volatile
        private var INSTANCE: RoketRoDatabase? = null

        fun getDatabase(context: Context): RoketRoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RoketRoDatabase::class.java,
                    "roket_ro_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
