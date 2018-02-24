package io.github.t3r1jj.ips.collector.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class WifiDataset @JsonCreator constructor(@JsonProperty("place") val place: String,
                                                @JsonProperty("fingerprints") val fingerprints: List<Fingerprint>)
    : Dataset(DatasetType.WIFI, Date().time)

open class Dataset(val type: DatasetType, val timestamp: Long)

enum class DatasetType {
    WIFI, INERTIAL
}

