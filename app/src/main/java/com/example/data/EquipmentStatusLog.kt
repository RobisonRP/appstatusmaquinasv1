package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipment_status_logs")
data class EquipmentStatusLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val equipmentId: Long,
    val equipmentName: String,
    val statusAnterior: String,
    val statusNovo: String,
    val timestamp: Long = System.currentTimeMillis()
)
