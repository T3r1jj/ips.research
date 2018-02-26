package io.github.t3r1jj.ips.collector.model.sampler

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager

class MagneticSampler(context: Context) {
    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val magneticField = mutableListOf<SensorSample>()
    val rotation = mutableListOf<SensorSample>()
    var isRunning = false
    var sampleCount = 50
    val isEmpty: Boolean
        get() {
            return magneticField.isEmpty()
        }
    var delay = SensorDelay.NORMAL
    private val magnetometerListener = SensorSampleUnlimitedEventListener(magneticField)
    private val gyroscopeListener = SensorSampleUnlimitedEventListener(rotation)

    inner class SensorSampleUnlimitedEventListener(samples: MutableList<SensorSample>) : SensorSampleEventListener(samples) {
        override fun onSensorChanged(p0: SensorEvent?) {
            super.onSensorChanged(p0)
            if (sampleCount > 0 && samples.size >= sampleCount) {
                stopSampling()
            }
        }
    }

    fun startSampling() {
        isRunning = true
        magneticField.clear()
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager.registerListener(magnetometerListener, magnetometer, delay.sensorManagerDelay)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorManager.registerListener(gyroscopeListener, gyroscope, delay.sensorManagerDelay)
    }

    fun stopSampling() {
        sensorManager.unregisterListener(magnetometerListener)
        sensorManager.unregisterListener(gyroscopeListener)
        isRunning = false
    }

}