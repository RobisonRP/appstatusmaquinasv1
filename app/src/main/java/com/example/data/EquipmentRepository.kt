package com.example.data

import kotlinx.coroutines.flow.Flow

class EquipmentRepository(private val equipmentDao: EquipmentDao) {
    val allEquipments: Flow<List<Equipment>> = equipmentDao.getAllEquipments()

    suspend fun insert(equipment: Equipment): Long {
        return equipmentDao.insertEquipment(equipment)
    }

    suspend fun update(equipment: Equipment) {
        equipmentDao.updateEquipment(equipment)
    }

    suspend fun delete(equipment: Equipment) {
        equipmentDao.deleteEquipment(equipment)
    }
}
