package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<WaterLog>>

    @Query("SELECT * FROM water_logs WHERE dateString = :date ORDER BY timestamp DESC")
    fun getLogsForDate(date: String): Flow<List<WaterLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WaterLog)

    @Delete
    suspend fun deleteLog(log: WaterLog)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    @Query("SELECT SUM(waterEquivalentMl) FROM water_logs WHERE dateString = :dateString")
    fun getTotalIntakeForDate(dateString: String): Flow<Int?>

    @Query("DELETE FROM water_logs")
    suspend fun deleteAllLogs()
}
