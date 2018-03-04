package io.github.t3r1jj.ips.collector.model.sampler

import android.hardware.Sensor
import android.util.Log

open class SensorSampler {
    var sensorsInfo = ""

    protected fun initSensorsInfo(vararg sensors: Sensor?) {
        val sb = StringBuilder()
        var prefix = ""
        for (sensor in sensors) {
            if (sensor == null) {
                continue
            }
            sb.append(prefix)
            sensorInfo(sensor, sb)
            prefix = "; "
        }
        sensorsInfo = sb.toString()
        Log.d("Sensors", sensorsInfo)
    }

    private fun sensorInfo(sensor: Sensor, sb: StringBuilder): StringBuilder {
        return sb
                .append("name: ")
                .append(sensor.name)
                .append(", ")
                .append("resolution: ")
                .append(sensor.resolution)
                .append(", ")
                .append("maximumRange: ")
                .append(sensor.maximumRange)
                .append(", ")
                .append("type: ")
                .append(sensor.type)
                .append(", ")
                .append("vendor: ")
                .append(sensor.vendor)
                .append(", ")
                .append("version: ")
                .append(sensor.version)
    }
}