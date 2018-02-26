package io.github.t3r1jj.ips.collector.model.data

import java.util.*

open class SensorDataset(type: DatasetType) : Dataset(type, Date().time)