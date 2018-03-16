package io.github.t3r1jj.ips.collector.model.collector

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

open class SensorSampleEventListener(protected val samples: MutableList<SensorSample>) : SensorEventListener {
    var sensorSamplerListener: InertialSampler.InertialSamplerListener? = null
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        p0?.run {
            val sample = SensorSample(p0.values, p0.timestamp)
            samples.add(sample)
            sensorSamplerListener?.onSampleReceived(sample)
        }
    }
}