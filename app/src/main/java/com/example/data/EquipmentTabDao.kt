package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentTabDao {
    @Query("SELECT * FROM equipment_tabs ORDER BY id ASC")
    fun getAllTabs(): Flow<List<EquipmentTab>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: EquipmentTab): Long

    @Update
    suspend fun updateTab(tab: EquipmentTab)

    @Delete
    suspend fun deleteTab(tab: EquipmentTab)

    @Query("DELETE FROM equipment_tabs WHERE id = :tabId")
    suspend fun deleteTabById(tabId: Long)

    @Query("DELETE FROM equipment_tabs")
    suspend fun deleteAll()
}
