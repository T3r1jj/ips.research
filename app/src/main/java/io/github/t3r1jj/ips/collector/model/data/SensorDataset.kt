package io.github.t3r1jj.ips.collector.model.data

open class SensorDataset(type: DatasetType) : Dataset(type) {
    lateinit var sensors: String
}