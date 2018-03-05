package io.github.t3r1jj.ips.collector.model.sampler

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

class InertialSampler(context: Context) : SensorSampler() {
    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val acceleration = mutableListOf<SensorSample>()
    val linearAcceleration = mutableListOf<SensorSample>()
    val magneticField = mutableListOf<SensorSample>()
    val gravity = mutableListOf<SensorSample>()

    var isRunning = false
    val isEmpty: Boolean
        get() {
            return acceleration.isEmpty() && linearAcceleration.isEmpty()
        }
    var delay = SensorDelay.NORMAL
    private val accelerometerListener = SensorSampleEventListener(acceleration)
    private val linearAccelerometerListener = SensorSampleEventListener(linearAcceleration)
    private val magnetometerListener = SensorSampleEventListener(magneticField)
    private val gravityListener = SensorSampleEventListener(gravity)

    fun startSampling() {
        isRunning = true
        acceleration.clear()
        linearAcceleration.clear()
        val linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        sensorManager.registerListener(linearAccelerometerListener, linearAccelerometer, delay.sensorManagerDelay)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(accelerometerListener, accelerometer, delay.sensorManagerDelay)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager.registerListener(magnetometerListener, magnetometer, delay.sensorManagerDelay)
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        sensorManager.registerListener(gravityListener, gravitySensor, delay.sensorManagerDelay)
        if (sensorsInfo.isEmpty()) {
            val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            initSensorsInfo(accelerometer, linearAccelerometer, gravitySensor, gyroscope, magnetometer)
        }
    }

    fun stopSampling() {
        isRunning = false
        sensorManager.unregisterListener(linearAccelerometerListener)
        sensorManager.unregisterListener(accelerometerListener)
        sensorManager.unregisterListener(magnetometerListener)
        sensorManager.unregisterListener(gravityListener)
    }

}