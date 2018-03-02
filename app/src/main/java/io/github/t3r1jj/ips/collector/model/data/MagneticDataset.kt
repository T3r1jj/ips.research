package io.github.t3r1jj.ips.collector.model.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.t3r1jj.ips.collector.model.sampler.SensorSample

data class MagneticDataset @JsonCreator constructor(@JsonProperty("place") val place: String,
                                                    @JsonProperty("magneticField") val magneticField: List<SensorSample>,
                                                    @JsonProperty("gravity") val gravity: List<SensorSample>)
    : SensorDataset(DatasetType.MAGNETIC) {

    override fun toString(): String {
        return "[" + type.toString() + "] " + place + " (" + timestamp.toString() + ") " + "samples: " + (magneticField.size + gravity.size).toString()
    }
}