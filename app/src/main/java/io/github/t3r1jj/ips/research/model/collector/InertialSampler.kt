package io.github.t3r1jj.ips.research.model.collector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

class InertialSampler(context: Context) : SensorSampler() {
    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val acceleration = mutableListOf<SensorSample>()

    var isRunning = false
    val isEmpty: Boolean
        get() {
            return acceleration.isEmpty()
        }
    var delay = SensorDelay.NORMAL
    private val accelerometerListener = SensorSampleEventListener(acceleration)
    var samplerListener: InertialSampleListener? = null
        set(value) {
            field = value
            accelerometerListener.sensorSamplerListener = value
        }

    fun startSampling() {
        isRunning = true
        acceleration.clear()
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(accelerometerListener, accelerometer, delay.sensorManagerDelay)
        if (sensorsInfo.isEmpty()) {
            initSensorsInfo(accelerometer)
        }
    }

    fun stopSampling() {
        isRunning = false
        sensorManager.unregisterListener(accelerometerListener)
    }

    interface InertialSampleListener {
        fun onSampleReceived(sensorSample: SensorSample)
    }
}