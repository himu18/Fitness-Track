package com.example.fitnesstrack.repository

import android.content.Context
import com.example.fitnesstrack.StepCountManager

/**
 * Repository for managing step count data
 * This abstracts the data source (SharedPreferences) from the ViewModel
 */
class StepCountRepository(private val context: Context) {
    
    fun getCurrentSteps(): Int {
        return StepCountManager.getCurrentSteps(context)
    }
    
    fun getDailyGoal(): Int {
        return StepCountManager.getDailyGoal(context)
    }
    
    fun setDailyGoal(goal: Int) {
        StepCountManager.setDailyGoal(context, goal)
    }
    
    fun getStepsProgress(): Float {
        return StepCountManager.getStepsProgress(context)
    }
    
    fun getHistoryData(days: Int = 7): List<Pair<String, Int>> {
        return StepCountManager.getHistoryData(context, days)
    }
    
    fun isBackgroundTrackingEnabled(): Boolean {
        return context.getSharedPreferences("fitness_tracker_prefs", Context.MODE_PRIVATE)
            .getBoolean("background_tracking", false)
    }
    
    fun setBackgroundTracking(enabled: Boolean) {
        context.getSharedPreferences("fitness_tracker_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("background_tracking", enabled)
            .apply()
    }
}

