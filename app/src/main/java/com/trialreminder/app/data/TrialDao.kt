package com.trialreminder.app.data

import androidx.room.*
import com.trialreminder.app.model.Trial
import kotlinx.coroutines.flow.Flow

@Dao
interface TrialDao {
    @Query("SELECT * FROM trials ORDER BY endDate ASC")
    fun getAllTrials(): Flow<List<Trial>>

    @Query("SELECT * FROM trials ORDER BY endDate ASC")
    suspend fun getAllTrialsList(): List<Trial>

    @Query("SELECT * FROM trials WHERE id = :trialId LIMIT 1")
    suspend fun getById(trialId: Int): Trial?

    @Insert
    suspend fun insert(trial: Trial): Long

    @Update
    suspend fun update(trial: Trial)

    @Delete
    suspend fun delete(trial: Trial)

    @Query("DELETE FROM trials WHERE id = :trialId")
    suspend fun deleteById(trialId: Int)
}
