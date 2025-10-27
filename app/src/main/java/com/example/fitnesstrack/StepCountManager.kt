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
            .commit() // Use commit() instead of apply() for immediate persistence
    }
    
    fun saveStepsPersistently(context: Context, steps: Int) {
        val prefs = getPrefs(context)
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        prefs.edit()
            .putInt(KEY_CURRENT_STEPS, steps)
            .putString(KEY_LAST_DATE, today)
            .commit() // Use commit() for immediate write to disk
    }
    
    private fun resetDailySteps(context: Context) {
        val prefs = getPrefs(context)
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_DATE)
        
        // Save yesterday's steps to history before resetting
        val yesterdaySteps = prefs.getInt("history_$yesterday", 0)
        if (yesterdaySteps == 0) {
            val currentSteps = prefs.getInt(KEY_CURRENT_STEPS, 0)
            if (currentSteps > 0) {
                // Save yesterday's data
                prefs.edit().putInt("history_$yesterday", currentSteps).commit()
            }
        }
        
        prefs.edit()
            .putInt(KEY_CURRENT_STEPS, 0)
            .putString(KEY_LAST_DATE, today)
            .commit() // Use commit() for immediate persistence
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
        prefs.edit().putInt("history_$date", steps).commit() // Use commit() for immediate persistence
    }
    
    fun getHistoryData(context: Context, days: Int = 7): List<Pair<String, Int>> {
        val prefs = getPrefs(context)
        val formatter = DateTimeFormatter.ISO_DATE
        val today = LocalDate.now()
        
        // Save current day's steps to history
        val currentSteps = getCurrentSteps(context)
        saveDailyHistory(context, currentSteps)
        
        // Clean up old data (keep only last 7 days)
        cleanupOldHistory(context)
        
        val history = mutableListOf<Pair<String, Int>>()
        
        for (i in 0 until days) {
            val date = today.minusDays(i.toLong())
            val dateStr = date.format(formatter)
            val steps = prefs.getInt("history_$dateStr", 0)
            history.add(Pair(dateStr, steps))
        }
        
        return history.reversed()
    }
    
    private fun cleanupOldHistory(context: Context) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()
        val today = LocalDate.now()
        
        // Remove data older than 7 days
        for (i in 7..30) {
            val date = today.minusDays(i.toLong())
            val dateStr = date.format(DateTimeFormatter.ISO_DATE)
            editor.remove("history_$dateStr")
        }
        editor.commit() // Use commit() for immediate persistence
    }
    
    fun getTodayHistory(context: Context): Pair<String, Int> {
        val prefs = getPrefs(context)
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val steps = getCurrentSteps(context)
        prefs.edit().putInt("history_$today", steps).apply()
        return Pair(today, steps)
    }
}

