package com.example.fitnesstrack.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstrack.StepCountManager
import com.example.fitnesstrack.StepTrackingService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StepTrackingUiState(
    val currentSteps: Int = 0,
    val dailyGoal: Int = 100,
    val progress: Float = 0f,
    val remainingSteps: Int = 0,
    val historyData: List<Pair<String, Int>> = emptyList()
)

class StepTrackingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(StepTrackingUiState())
    val uiState: StateFlow<StepTrackingUiState> = _uiState.asStateFlow()
    
    init {
        loadInitialState()
        
        startPeriodicUpdates()
    }
    
    private fun loadInitialState() {
        val context = getApplication<Application>().applicationContext
        val currentSteps = StepCountManager.getCurrentSteps(context)
        
        // Default goal is 100
        val goal = StepCountManager.getDailyGoal(context)
        
        val progress = if (goal > 0) {
            (currentSteps.toFloat() / goal).coerceIn(0f, 1f)
        } else {
            0f
        }
        val remainingSteps = (goal - currentSteps).coerceAtLeast(0)
        
        _uiState.value = _uiState.value.copy(
            currentSteps = currentSteps,
            dailyGoal = goal,
            progress = progress,
            remainingSteps = remainingSteps
        )
    }
    
    private fun startPeriodicUpdates() {
        viewModelScope.launch {
            var saveCounter = 0
            while (true) {
                updateSteps()
                delay(1000)
                
                // Save data to persistent storage every 10 seconds
                saveCounter++
                if (saveCounter >= 10) {
                    saveDataToStorage()
                    saveCounter = 0
                }
            }
        }
    }
    
    private fun updateSteps() {
        val context = getApplication<Application>().applicationContext
        val currentSteps = StepCountManager.getCurrentSteps(context)
        val goal = _uiState.value.dailyGoal
        val progress = if (goal > 0) {
            (currentSteps.toFloat() / goal).coerceIn(0f, 1f)
        } else {
            0f
        }
        val remainingSteps = (goal - currentSteps).coerceAtLeast(0)
        
        _uiState.value = _uiState.value.copy(
            currentSteps = currentSteps,
            progress = progress,
            remainingSteps = remainingSteps
        )
    }
    
    private fun saveDataToStorage() {
        val context = getApplication<Application>().applicationContext
        val currentSteps = _uiState.value.currentSteps
        StepCountManager.saveStepsPersistently(context, currentSteps)
        StepCountManager.saveDailyHistory(context, currentSteps)
    }
    
    
    fun loadHistoryData() {
        val context = getApplication<Application>().applicationContext
        val history = StepCountManager.getHistoryData(context, 7)
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                historyData = history
            )
        }
    }
    
}

