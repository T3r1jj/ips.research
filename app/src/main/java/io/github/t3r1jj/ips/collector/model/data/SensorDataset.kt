package io.github.t3r1jj.ips.collector.model.data

import io.github.t3r1jj.ips.collector.model.collector.SensorDelay

open class SensorDataset(type: DatasetType) : Dataset(type) {
    lateinit var sensors: String
    var sensorDelay = SensorDelay.NORMAL
}