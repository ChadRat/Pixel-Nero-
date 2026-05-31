package com.example.data

import kotlinx.coroutines.flow.Flow

class WaterRepository(private val waterLogDao: WaterLogDao) {
    fun getAllLogs(): Flow<List<WaterLog>> = waterLogDao.getAllLogs()

    fun getLogsForDate(date: String): Flow<List<WaterLog>> = waterLogDao.getLogsForDate(date)

    fun getTotalIntakeForDate(date: String): Flow<Int?> = waterLogDao.getTotalIntakeForDate(date)

    suspend fun insertLog(log: WaterLog) {
        waterLogDao.insertLog(log)
    }

    suspend fun deleteLog(log: WaterLog) {
        waterLogDao.deleteLog(log)
    }

    suspend fun deleteLogById(id: Int) {
        waterLogDao.deleteLogById(id)
    }

    suspend fun deleteAllLogs() {
        waterLogDao.deleteAllLogs()
    }
}
