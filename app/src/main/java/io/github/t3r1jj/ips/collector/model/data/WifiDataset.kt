package io.github.t3r1jj.ips.collector.model.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class WifiDataset @JsonCreator constructor(@JsonProperty("place") val place: String,
                                                @JsonProperty("fingerprints") val fingerprints: List<Fingerprint>)
    : Dataset(DatasetType.WIFI, Date().time) {

    override fun toString(): String {
        return "[" + type.toString() + "] " + place + " (" + timestamp.toString() + ") " + "samples: " + fingerprints.size.toString()
    }
}