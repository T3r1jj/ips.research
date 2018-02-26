package io.github.t3r1jj.ips.collector.model.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.t3r1jj.ips.collector.InertialActivity
import io.github.t3r1jj.ips.collector.model.sampler.SensorSample

data class InertialDataset @JsonCreator constructor(@JsonProperty("movementType") val movementType: InertialActivity.InertialMovementType,
                                                    @JsonProperty("acceleration") val acceleration: List<SensorSample>,
                                                    @JsonProperty("linearAcceleration") val linearAcceleration: List<SensorSample>,
                                                    @JsonProperty("rotation") val rotation: List<SensorSample>)
    : SensorDataset(DatasetType.INERTIAL) {

    override fun toString(): String {
        return "[" + type.toString() + "] " + movementType.toString() + " (" + timestamp.toString() + ") " + "samples: " + (acceleration.size + linearAcceleration.size + rotation.size).toString()
    }
}