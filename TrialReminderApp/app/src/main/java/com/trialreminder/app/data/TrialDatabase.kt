package com.trialreminder.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.trialreminder.app.model.Trial

@Database(entities = [Trial::class], version = 1, exportSchema = false)
abstract class TrialDatabase : RoomDatabase() {
    abstract fun trialDao(): TrialDao

    companion object {
        @Volatile
        private var INSTANCE: TrialDatabase? = null

        fun getDatabase(context: Context): TrialDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrialDatabase::class.java,
                    "trial_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
