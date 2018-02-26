package io.github.t3r1jj.ips.collector.model.sampler

import android.hardware.SensorManager

enum class SensorDelay(val sensorManagerDelay: Int) {
    NORMAL(SensorManager.SENSOR_DELAY_NORMAL), FASTEST(SensorManager.SENSOR_DELAY_FASTEST)
}