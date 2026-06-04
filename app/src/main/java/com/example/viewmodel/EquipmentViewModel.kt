package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Equipment
import com.example.data.EquipmentRepository
import com.example.data.EquipmentStatusLog
import com.example.data.EquipmentTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EquipmentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EquipmentRepository

    val equipmentsState: StateFlow<List<Equipment>>
    val logsState: StateFlow<List<EquipmentStatusLog>>
    val tabsState: StateFlow<List<EquipmentTab>>
    val selectedTabId = MutableStateFlow<Long>(1L)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = EquipmentRepository(
            database.equipmentDao(),
            database.equipmentStatusLogDao(),
            database.equipmentTabDao()
        )
        
        equipmentsState = repository.allEquipments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        logsState = repository.allLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        tabsState = repository.allTabs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed initial data if the database is completely empty
        viewModelScope.launch {
            try {
                // Ensure at least one tab exists
                val currentTabs = repository.allTabs.first()
                var targetTabId = 1L
                if (currentTabs.isEmpty()) {
                    targetTabId = repository.insertTab(EquipmentTab(id = 1L, name = "Geral"))
                } else {
                    targetTabId = currentTabs.first().id
                }
                selectedTabId.value = targetTabId

                val currentList = repository.allEquipments.first()
                if (currentList.isEmpty()) {
                    repository.insert(Equipment(nome = "Escavadeira ES218", status = "Disponível", obs = "", tabId = targetTabId))
                    repository.insert(Equipment(nome = "Pá Carregadeira 02", status = "Em Manutenção", obs = "Troca de mangueira hidráulica", tabId = targetTabId))
                    repository.insert(Equipment(nome = "Caminhão Caçamba 05", status = "Preventiva", obs = "Revisão dos 500h", tabId = targetTabId))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addEquipment(nome: String) {
        viewModelScope.launch {
            if (nome.isNotBlank()) {
                repository.insert(Equipment(nome = nome.trim(), status = "Disponível", obs = "", tabId = selectedTabId.value))
            }
        }
    }

    fun addEquipmentToTab(nome: String, tabId: Long) {
        viewModelScope.launch {
            if (nome.isNotBlank()) {
                repository.insert(Equipment(nome = nome.trim(), status = "Disponível", obs = "", tabId = tabId))
            }
        }
    }

    fun addTab(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val newId = repository.insertTab(EquipmentTab(name = name.trim()))
                selectedTabId.value = newId
            }
        }
    }

    fun renameTab(tabId: Long, newName: String) {
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                repository.updateTab(EquipmentTab(id = tabId, name = newName.trim()))
            }
        }
    }

    fun deleteTab(tabId: Long) {
        viewModelScope.launch {
            repository.deleteTabAndEquipments(tabId)
            
            // Re-adjust selectedTabId if we deleted the current active tab
            if (selectedTabId.value == tabId) {
                val currentTabs = repository.allTabs.first()
                val remainingTab = currentTabs.firstOrNull { it.id != tabId }
                if (remainingTab != null) {
                    selectedTabId.value = remainingTab.id
                } else {
                    // Recreate a default tab if no tabs are left
                    val defaultTabId = repository.insertTab(EquipmentTab(name = "Geral"))
                    selectedTabId.value = defaultTabId
                }
            }
        }
    }

    fun selectTab(tabId: Long) {
        selectedTabId.value = tabId
    }

    fun updateEquipmentStatus(equipment: Equipment, status: String) {
        viewModelScope.launch {
            if (equipment.status != status) {
                repository.updateWithStatusLog(equipment, status)
            }
        }
    }

    fun updateEquipmentObs(equipment: Equipment, obs: String) {
        viewModelScope.launch {
            repository.update(equipment.copy(obs = obs))
        }
    }

    fun updateEquipmentName(equipment: Equipment, newName: String) {
        viewModelScope.launch {
            repository.update(equipment.copy(nome = newName))
        }
    }

    fun deleteEquipment(equipment: Equipment) {
        viewModelScope.launch {
            repository.delete(equipment)
        }
    }

    fun moveEquipmentUp(equipment: Equipment) {
        viewModelScope.launch {
            val list = equipmentsState.value
                .filter { it.tabId == equipment.tabId }
                .sortedWith(compareBy({ it.ordem }, { it.id }))
            val index = list.indexOfFirst { it.id == equipment.id }
            if (index > 0) {
                val updatedList = list.toMutableList()
                val prev = updatedList[index - 1]
                updatedList[index - 1] = equipment
                updatedList[index] = prev
                
                updatedList.forEachIndexed { idx, eq ->
                    repository.update(eq.copy(ordem = idx))
                }
            }
        }
    }

    fun moveEquipmentDown(equipment: Equipment) {
        viewModelScope.launch {
            val list = equipmentsState.value
                .filter { it.tabId == equipment.tabId }
                .sortedWith(compareBy({ it.ordem }, { it.id }))
            val index = list.indexOfFirst { it.id == equipment.id }
            if (index >= 0 && index < list.size - 1) {
                val updatedList = list.toMutableList()
                val next = updatedList[index + 1]
                updatedList[index + 1] = equipment
                updatedList[index] = next
                
                updatedList.forEachIndexed { idx, eq ->
                    repository.update(eq.copy(ordem = idx))
                }
            }
        }
    }

    fun getBackupJsonString(): String {
        val json = org.json.JSONObject()
        try {
            json.put("version", 1)
            
            val tabsArray = org.json.JSONArray()
            for (tab in tabsState.value) {
                val tabObj = org.json.JSONObject()
                tabObj.put("id", tab.id)
                tabObj.put("name", tab.name)
                tabsArray.put(tabObj)
            }
            json.put("tabs", tabsArray)
            
            val equipmentsArray = org.json.JSONArray()
            for (eq in equipmentsState.value) {
                val eqObj = org.json.JSONObject()
                eqObj.put("id", eq.id)
                eqObj.put("nome", eq.nome)
                eqObj.put("status", eq.status)
                eqObj.put("obs", eq.obs)
                eqObj.put("tabId", eq.tabId)
                eqObj.put("ordem", eq.ordem)
                equipmentsArray.put(eqObj)
            }
            json.put("equipments", equipmentsArray)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return json.toString(2)
    }

    fun importBackupJsonString(jsonStr: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = org.json.JSONObject(jsonStr)
                if (!json.has("tabs") || !json.has("equipments")) {
                    onError("Formato de backup inválido. Chaves 'tabs' ou 'equipments' ausentes.")
                    return@launch
                }
                
                val tabsArray = json.getJSONArray("tabs")
                val equipmentsArray = json.getJSONArray("equipments")
                
                val tabsToInsert = mutableListOf<EquipmentTab>()
                for (i in 0 until tabsArray.length()) {
                    val tabObj = tabsArray.getJSONObject(i)
                    val id = tabObj.getLong("id")
                    val name = tabObj.getString("name").trim()
                    tabsToInsert.add(EquipmentTab(id = id, name = name))
                }
                
                val equipmentsToInsert = mutableListOf<Equipment>()
                for (i in 0 until equipmentsArray.length()) {
                    val eqObj = equipmentsArray.getJSONObject(i)
                    val id = eqObj.getLong("id")
                    val nome = eqObj.getString("nome").trim()
                    val status = eqObj.getString("status")
                    val obs = eqObj.optString("obs", "")
                    val tabId = eqObj.getLong("tabId")
                    val ordem = eqObj.optInt("ordem", 0)
                    equipmentsToInsert.add(Equipment(id = id, nome = nome, status = status, obs = obs, tabId = tabId, ordem = ordem))
                }
                
                if (tabsToInsert.isEmpty()) {
                    onError("O backup precisa conter pelo menos uma aba.")
                    return@launch
                }
                
                repository.clearAndRestoreBackup(tabsToInsert, equipmentsToInsert)
                
                // Select back the first tab
                selectedTabId.value = tabsToInsert.first().id
                
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError("Erro ao ler JSON: ${e.localizedMessage ?: "formato incorreto"}")
            }
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EquipmentViewModel(application) as T
                }
            }
    }
}
