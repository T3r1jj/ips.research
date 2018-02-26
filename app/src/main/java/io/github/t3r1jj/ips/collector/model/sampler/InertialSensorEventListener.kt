package io.github.t3r1jj.ips.collector.model.sampler

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

open class SensorSampleEventListener(private val samples: MutableList<SensorSample>) : SensorEventListener {
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        println("New accuracy: " + p1)
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        p0?.run {
            samples.add(SensorSample(p0.values, p0.timestamp))
            println(p0.values.joinToString { it.toString() + " " })
            println(p0.accuracy)
        }
    }
}