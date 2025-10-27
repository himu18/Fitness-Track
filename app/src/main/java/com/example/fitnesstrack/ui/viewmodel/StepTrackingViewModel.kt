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
    val dailyGoal: Int = 10000,
    val progress: Float = 0f,
    val remainingSteps: Int = 0,
    val isTracking: Boolean = false,
    val historyData: List<Pair<String, Int>> = emptyList(),
    val backgroundTrackingEnabled: Boolean = false,
    val historyStats: Map<String, Any> = emptyMap(),
    val historyDays: Int = 7
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
        val goal = StepCountManager.getDailyGoal(context)
        val progress = if (goal > 0) {
            (currentSteps.toFloat() / goal).coerceIn(0f, 1f)
        } else {
            0f
        }
        val remainingSteps = (goal - currentSteps).coerceAtLeast(0)
        val backgroundTracking = context.getSharedPreferences("fitness_tracker_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("background_tracking", false)
        
        _uiState.value = _uiState.value.copy(
            currentSteps = currentSteps,
            dailyGoal = goal,
            progress = progress,
            remainingSteps = remainingSteps,
            backgroundTrackingEnabled = backgroundTracking
        )
    }
    
    private fun startPeriodicUpdates() {
        viewModelScope.launch {
            while (true) {
                updateSteps()
                delay(1000)
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
    
    fun setDailyGoal(goal: Int) {
        val context = getApplication<Application>().applicationContext
        StepCountManager.setDailyGoal(context, goal)
        
        val currentSteps = _uiState.value.currentSteps
        val progress = if (goal > 0) {
            (currentSteps.toFloat() / goal).coerceIn(0f, 1f)
        } else {
            0f
        }
        val remainingSteps = (goal - currentSteps).coerceAtLeast(0)
        
        _uiState.value = _uiState.value.copy(
            dailyGoal = goal,
            progress = progress,
            remainingSteps = remainingSteps
        )
    }
    
    fun loadHistoryData(days: Int = 7) {
        val context = getApplication<Application>().applicationContext
        val history = StepCountManager.getHistoryData(context, days)
        val stats = StepCountManager.getHistoryStats(context, days)
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                historyData = history,
                historyStats = stats,
                historyDays = days
            )
        }
    }
    
    fun updateBackgroundTracking(enabled: Boolean) {
        val context = getApplication<Application>().applicationContext
        context.getSharedPreferences("fitness_tracker_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("background_tracking", enabled)
            .apply()
        
        _uiState.value = _uiState.value.copy(
            backgroundTrackingEnabled = enabled
        )
        
        if (enabled) {
            StepTrackingService.startService(context)
        } else {
            StepTrackingService.stopService(context)
        }
    }
    
    fun setTrackingState(isTracking: Boolean) {
        _uiState.value = _uiState.value.copy(
            isTracking = isTracking
        )
    }
}

