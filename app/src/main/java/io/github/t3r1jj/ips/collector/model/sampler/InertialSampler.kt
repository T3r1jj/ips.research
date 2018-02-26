package io.github.t3r1jj.ips.collector.model.sampler

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

class InertialSampler(context: Context) {
    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val acceleration = mutableListOf<SensorSample>()
    val linearAcceleration = mutableListOf<SensorSample>()
    val rotation = mutableListOf<SensorSample>()

    var isRunning = false
    val isEmpty: Boolean
        get() {
            return acceleration.isEmpty() && linearAcceleration.isEmpty() && rotation.isEmpty()
        }
    var delay = SensorDelay.NORMAL
    private val accelerometerListener = SensorSampleEventListener(acceleration)
    private val linearAccelerometerListener = SensorSampleEventListener(linearAcceleration)
    private val gyroscopeListener = SensorSampleEventListener(rotation)

    fun startSampling() {
        isRunning = true
        acceleration.clear()
        linearAcceleration.clear()
        rotation.clear()
        val linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        sensorManager.registerListener(linearAccelerometerListener, linearAccelerometer, delay.sensorManagerDelay)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(accelerometerListener, accelerometer, delay.sensorManagerDelay)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorManager.registerListener(gyroscopeListener, gyroscope, delay.sensorManagerDelay)
    }

    fun stopSampling() {
        isRunning = false
        sensorManager.unregisterListener(linearAccelerometerListener)
        sensorManager.unregisterListener(accelerometerListener)
        sensorManager.unregisterListener(gyroscopeListener)
    }

}