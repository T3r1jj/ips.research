package io.github.t3r1jj.ips.research.model.collector

import android.hardware.SensorManager

enum class SensorDelay(val sensorManagerDelay: Int) {
    NORMAL(SensorManager.SENSOR_DELAY_NORMAL), FASTEST(SensorManager.SENSOR_DELAY_FASTEST)
}