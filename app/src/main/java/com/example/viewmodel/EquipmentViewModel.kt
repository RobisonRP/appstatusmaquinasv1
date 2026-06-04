package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Equipment
import com.example.data.EquipmentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EquipmentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EquipmentRepository

    val equipmentsState: StateFlow<List<Equipment>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = EquipmentRepository(database.equipmentDao())
        
        equipmentsState = repository.allEquipments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed initial data if the database is completely empty
        viewModelScope.launch {
            try {
                val currentList = repository.allEquipments.first()
                if (currentList.isEmpty()) {
                    repository.insert(Equipment(nome = "Escavadeira ES218", status = "Disponível", obs = ""))
                    repository.insert(Equipment(nome = "Pá Carregadeira 02", status = "Em Manutenção", obs = "Troca de mangueira hidráulica"))
                    repository.insert(Equipment(nome = "Caminhão Caçamba 05", status = "Preventiva", obs = "Revisão dos 500h"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addEquipment(nome: String) {
        viewModelScope.launch {
            if (nome.isNotBlank()) {
                repository.insert(Equipment(nome = nome.trim(), status = "Disponível", obs = ""))
            }
        }
    }

    fun updateEquipmentStatus(equipment: Equipment, status: String) {
        viewModelScope.launch {
            repository.update(equipment.copy(status = status))
        }
    }

    fun updateEquipmentObs(equipment: Equipment, obs: String) {
        viewModelScope.launch {
            repository.update(equipment.copy(obs = obs))
        }
    }

    fun deleteEquipment(equipment: Equipment) {
        viewModelScope.launch {
            repository.delete(equipment)
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
