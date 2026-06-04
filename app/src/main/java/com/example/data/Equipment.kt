package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipments")
data class Equipment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val status: String, // "Disponível", "Em Manutenção", "Preventiva"
    val obs: String = "",
    val tabId: Long = 1L,
    val ordem: Int = 0
)
