package io.github.t3r1jj.ips.research.model.data

import io.github.t3r1jj.ips.research.model.collector.SensorDelay

open class SensorDataset(type: DatasetType) : Dataset(type) {
    lateinit var sensors: String
    var sensorDelay = SensorDelay.NORMAL
}