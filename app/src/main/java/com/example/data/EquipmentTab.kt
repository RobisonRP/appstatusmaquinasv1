package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipment_tabs")
data class EquipmentTab(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
