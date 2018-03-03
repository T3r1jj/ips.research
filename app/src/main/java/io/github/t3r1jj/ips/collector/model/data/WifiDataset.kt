package io.github.t3r1jj.ips.collector.model.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class WifiDataset @JsonCreator constructor(@JsonProperty("place") val place: String,
                                                @JsonProperty("fingerprints") val fingerprints: List<Fingerprint>)
    : Dataset(DatasetType.WIFI) {

    override fun toString(): String {
        return "{" + place + "} " + super.toString() + ", samples: " + fingerprints.size.toString()
    }
}