package com.example.fitnesstrack

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class StepSensorListener(private val context: Context) : SensorEventListener {
    
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = null
    private var stepDetector: Sensor? = null
    private var stepCounter: Sensor? = null
    
    private var initialSteps = 0
    private var isInitialized = false
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    if (!isInitialized) {
                        initialSteps = it.values[0].toInt()
                        isInitialized = true
                    }
                    val currentSteps = it.values[0].toInt() - initialSteps
                    StepCountManager.addSteps(context, currentSteps)
                }
                Sensor.TYPE_STEP_DETECTOR -> {
                    if (it.values[0] == 1f) {
                        val currentSteps = StepCountManager.getCurrentSteps(context) + 1
                        StepCountManager.addSteps(context, currentSteps)
                    }
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for step counting
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

