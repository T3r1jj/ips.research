package io.github.t3r1jj.ips.collector.model

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class InertialSampler(private val context: Context) : SensorEventListener {
    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val samples = mutableListOf<FloatArray>()

    var delay = InertialDelay.NORMAL
//
//    fun maxOf(lastSamplesCount: Int): Float {
//        return samples.drop(Math.max(0, samples.size - lastSamplesCount)).maxBy {
//            it[0]
//        }?.get(0) ?: 0f
//    }
//
//    fun minOf(lastSamplesCount: Int): Float {
//        return samples.drop(Math.max(0, samples.size - lastSamplesCount)).minBy {
//            it[0]
//        }?.get(0) ?: 0f
//    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        println("New accuracy: " + p1)
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        println("sss")
        println(p0)
        p0?.run {
            samples.add(p0.values)
            println(p0.values.joinToString { it.toString() + " " })
        }
    }


    fun startSampling() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stopSampling() {
        sensorManager.unregisterListener(this)
    }

    enum class InertialDelay(val sensorManagerDelay: Int) {
        NORMAL(SensorManager.SENSOR_DELAY_NORMAL), FASTEST(SensorManager.SENSOR_DELAY_FASTEST)
    }
}