package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentStatusLogDao {
    @Query("SELECT * FROM equipment_status_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<EquipmentStatusLog>>

    @Query("SELECT * FROM equipment_status_logs WHERE equipmentId = :equipmentId ORDER BY timestamp DESC")
    fun getLogsForEquipment(equipmentId: Long): Flow<List<EquipmentStatusLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: EquipmentStatusLog): Long

    @Query("DELETE FROM equipment_status_logs WHERE equipmentId = :equipmentId")
    suspend fun deleteLogsForEquipment(equipmentId: Long)

    @Query("DELETE FROM equipment_status_logs")
    suspend fun deleteAll()
}
