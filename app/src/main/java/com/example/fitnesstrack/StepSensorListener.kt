package com.example.fitnesstrack

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class StepSensorListener(private val context: Context) : SensorEventListener {
    
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var stepDetector: Sensor? = null
    private var stepCounter: Sensor? = null
    
    private var initialSteps = 0
    private var isInitialized = false
    private var previousSensorSteps = 0
    
    init {
        val base = StepCountManager.getStepCounterBase(context)
        if (base == -1) {
            previousSensorSteps = 0
        } else {
            previousSensorSteps = base
        }
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    val sensorSteps = it.values[0].toInt()
                    
                    if (!isInitialized) {
                        initialSteps = sensorSteps
                        StepCountManager.setStepCounterBase(context, sensorSteps)
                        isInitialized = true
                        previousSensorSteps = sensorSteps
                    } else {
                        // Check if goal is already achieved before processing new steps
                        if (StepCountManager.isGoalAchieved(context)) {
                            return
                        }
                        
                        val newSteps = if (sensorSteps >= previousSensorSteps) {
                            sensorSteps - previousSensorSteps
                        } else {
                            sensorSteps - StepCountManager.getStepCounterBase(context)
                        }
                        
                        if (newSteps > 0) {
                            val storedSteps = StepCountManager.getStoredSteps(context)
                            val updatedSteps = storedSteps + newSteps
                            StepCountManager.addSteps(context, updatedSteps)
                            previousSensorSteps = sensorSteps
                        }
                    }
                }
                Sensor.TYPE_STEP_DETECTOR -> {
                    // Check if goal is already achieved
                    if (StepCountManager.isGoalAchieved(context)) {
                        return
                    }
                    
                    if (it.values[0] == 1f) {
                        val storedSteps = StepCountManager.getStoredSteps(context)
                        StepCountManager.addSteps(context, storedSteps + 1)
                    }
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
    
    fun startListening() {
        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        
        val sensor = stepCounter ?: stepDetector
        sensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }
    
    fun stopListening() {
        sensorManager.unregisterListener(this)
    }
}

