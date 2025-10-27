package com.example.fitnesstrack

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object StepCountManager {
    private const val PREFS_NAME = "fitness_tracker_prefs"
    private const val KEY_CURRENT_STEPS = "current_steps"
    private const val KEY_DAILY_GOAL = "daily_goal"
    private const val KEY_LAST_DATE = "last_date"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun getCurrentSteps(context: Context): Int {
        val prefs = getPrefs(context)
        val lastDate = prefs.getString(KEY_LAST_DATE, "")
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        
        if (lastDate != today) {
            resetDailySteps(context)
        }
        
        return prefs.getInt(KEY_CURRENT_STEPS, 0)
    }
    
    fun addSteps(context: Context, steps: Int) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putInt(KEY_CURRENT_STEPS, steps)
            .putString(KEY_LAST_DATE, LocalDate.now().format(DateTimeFormatter.ISO_DATE))
            .apply()
    }
    
    private fun resetDailySteps(context: Context) {
        val prefs = getPrefs(context)
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        prefs.edit()
            .putInt(KEY_CURRENT_STEPS, 0)
            .putString(KEY_LAST_DATE, today)
            .apply()
    }
    
    fun getDailyGoal(context: Context): Int {
        val prefs = getPrefs(context)
        return prefs.getInt(KEY_DAILY_GOAL, 10000) // Default 10000 steps
    }
    
    fun setDailyGoal(context: Context, goal: Int) {
        val prefs = getPrefs(context)
        prefs.edit().putInt(KEY_DAILY_GOAL, goal).apply()
    }
    
    fun getStepsProgress(context: Context): Float {
        val currentSteps = getCurrentSteps(context)
        val goal = getDailyGoal(context)
        return if (goal > 0) {
            (currentSteps.toFloat() / goal).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    fun saveDailyHistory(context: Context, steps: Int) {
        val prefs = getPrefs(context)
        val date = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        prefs.edit().putInt("history_$date", steps).apply()
    }
    
    fun getHistoryData(context: Context, days: Int = 7): List<Pair<String, Int>> {
        val prefs = getPrefs(context)
        val history = mutableListOf<Pair<String, Int>>()
        val formatter = DateTimeFormatter.ISO_DATE
        val today = LocalDate.now()
        
        val currentSteps = getCurrentSteps(context)
        saveDailyHistory(context, currentSteps)
        
        for (i in 0 until days) {
            val date = today.minusDays(i.toLong())
            val dateStr = date.format(formatter)
            val steps = prefs.getInt("history_$dateStr", 0)
            history.add(Pair(dateStr, steps))
        }
        
        return history.reversed()
    }
    
    fun getHistoryStats(context: Context, days: Int = 30): Map<String, Any> {
        val history = getHistoryData(context, days)
        val totalSteps = history.sumOf { it.second }
        val averageSteps = if (history.isNotEmpty()) totalSteps / history.size else 0
        val maxSteps = history.maxOfOrNull { it.second } ?: 0
        val minSteps = history.minOfOrNull { it.second } ?: 0
        
        return mapOf(
            "total" to totalSteps,
            "average" to averageSteps,
            "max" to maxSteps,
            "min" to minSteps,
            "days" to history.size
        )
    }
    
    fun getTodayHistory(context: Context): Pair<String, Int> {
        val prefs = getPrefs(context)
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val steps = getCurrentSteps(context)
        prefs.edit().putInt("history_$today", steps).apply()
        return Pair(today, steps)
    }
}

