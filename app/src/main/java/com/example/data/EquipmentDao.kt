package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentDao {
    @Query("SELECT * FROM equipments ORDER BY ordem ASC, id ASC")
    fun getAllEquipments(): Flow<List<Equipment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipment: Equipment): Long

    @Update
    suspend fun updateEquipment(equipment: Equipment)

    @Delete
    suspend fun deleteEquipment(equipment: Equipment)

    @Query("DELETE FROM equipments")
    suspend fun deleteAll()

    @Query("DELETE FROM equipments WHERE tabId = :tabId")
    suspend fun deleteEquipmentsByTabId(tabId: Long)
}
