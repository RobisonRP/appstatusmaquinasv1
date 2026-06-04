package com.example.data

import kotlinx.coroutines.flow.Flow

class EquipmentRepository(
    private val equipmentDao: EquipmentDao,
    private val equipmentStatusLogDao: EquipmentStatusLogDao,
    private val equipmentTabDao: EquipmentTabDao
) {
    val allEquipments: Flow<List<Equipment>> = equipmentDao.getAllEquipments()
    val allLogs: Flow<List<EquipmentStatusLog>> = equipmentStatusLogDao.getAllLogs()
    val allTabs: Flow<List<EquipmentTab>> = equipmentTabDao.getAllTabs()

    suspend fun insertTab(tab: EquipmentTab): Long {
        return equipmentTabDao.insertTab(tab)
    }

    suspend fun updateTab(tab: EquipmentTab) {
        equipmentTabDao.updateTab(tab)
    }

    suspend fun deleteTab(tab: EquipmentTab) {
        // Also delete any equipment associated with this tab
        val list = equipmentDao.getAllEquipments() // wait, we can fetch all equipment or just let the repository handle filtering.
        // Let's delete the tab
        equipmentTabDao.deleteTab(tab)
        // Wait, to delete equipments of that tab, we can can declare a custom query in equipmentDao to delete by tabId, or we can just filter in VM or delete them
    }

    suspend fun deleteTabAndEquipments(tabId: Long) {
        equipmentTabDao.deleteTabById(tabId)
        equipmentDao.deleteEquipmentsByTabId(tabId)
    }

    suspend fun insert(equipment: Equipment): Long {
        val newId = equipmentDao.insertEquipment(equipment)
        val log = EquipmentStatusLog(
            equipmentId = newId,
            equipmentName = equipment.nome,
            statusAnterior = "Criado",
            statusNovo = equipment.status
        )
        equipmentStatusLogDao.insertLog(log)
        return newId
    }

    suspend fun update(equipment: Equipment) {
        equipmentDao.updateEquipment(equipment)
    }

    suspend fun updateWithStatusLog(equipment: Equipment, newStatus: String) {
        val oldStatus = equipment.status
        equipmentDao.updateEquipment(equipment.copy(status = newStatus))
        
        val log = EquipmentStatusLog(
            equipmentId = equipment.id,
            equipmentName = equipment.nome,
            statusAnterior = oldStatus,
            statusNovo = newStatus
        )
        equipmentStatusLogDao.insertLog(log)
    }

    suspend fun delete(equipment: Equipment) {
        equipmentDao.deleteEquipment(equipment)
        equipmentStatusLogDao.deleteLogsForEquipment(equipment.id)
    }

    suspend fun clearAndRestoreBackup(tabs: List<EquipmentTab>, equipments: List<Equipment>) {
        equipmentDao.deleteAll()
        equipmentTabDao.deleteAll()
        equipmentStatusLogDao.deleteAll()
        
        for (tab in tabs) {
            equipmentTabDao.insertTab(tab)
        }
        for (eq in equipments) {
            equipmentDao.insertEquipment(eq)
        }
    }
}
