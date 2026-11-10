package com.lugmety.orderassistant.data.local.dao

import androidx.room.*
import com.lugmety.orderassistant.data.local.entity.DiagnosticLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM diagnostic_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentLogs(): Flow<List<DiagnosticLogEntity>>

    @Insert
    suspend fun insertLog(log: DiagnosticLogEntity)

    @Query("DELETE FROM diagnostic_logs")
    suspend fun clearLogs()
}
