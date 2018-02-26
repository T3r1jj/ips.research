package io.github.t3r1jj.ips.collector.model.sampler

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager

class MagneticSampler(context: Context) {
    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val magneticField = mutableListOf<SensorSample>()
    var isRunning = false
    var sampleCount = 100
    val isEmpty: Boolean
        get() {
            return magneticField.isEmpty()
        }
    var delay = SensorDelay.NORMAL
    private val magnetometerListener = object : SensorSampleEventListener(magneticField) {
        override fun onSensorChanged(p0: SensorEvent?) {
            super.onSensorChanged(p0)
            if (sampleCount > 0 && magneticField.size >= sampleCount) {
                stopSampling()
            }
        }
    }

    fun startSampling() {
        isRunning = true
        magneticField.clear()
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager.registerListener(magnetometerListener, magnetometer, delay.sensorManagerDelay)
    }

    fun stopSampling() {
        isRunning = false
        sensorManager.unregisterListener(magnetometerListener)
    }

}