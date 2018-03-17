package io.github.t3r1jj.ips.research.model.collector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager

class MagneticSampler(context: Context) : SensorSampler() {
    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val magneticField = mutableListOf<SensorSample>()
    val gravity = mutableListOf<SensorSample>()
    var isRunning = false
    var sampleCount = -1
    val isEmpty: Boolean
        get() {
            return magneticField.isEmpty()
        }
    var delay = SensorDelay.NORMAL
    private val magnetometerListener = SensorSampleUnlimitedEventListener(magneticField)
    private val gravitySensorListener = SensorSampleUnlimitedEventListener(gravity)

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
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        sensorManager.registerListener(gravitySensorListener, gravitySensor, delay.sensorManagerDelay)
        val linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensorsInfo.isEmpty()) {
            val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            initSensorsInfo(accelerometer, linearAccelerometer, gravitySensor, gyroscope, magnetometer)
        }
    }

    fun stopSampling() {
        sensorManager.unregisterListener(magnetometerListener)
        sensorManager.unregisterListener(gravitySensorListener)
        isRunning = false
    }

}