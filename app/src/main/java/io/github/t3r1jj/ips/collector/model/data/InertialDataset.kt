package io.github.t3r1jj.ips.collector.model.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.t3r1jj.ips.collector.model.sampler.SensorSample

data class InertialDataset @JsonCreator constructor(@JsonProperty("movementType") val movementType: InertialMovementType,
                                                    @JsonProperty("acceleration") val acceleration: List<SensorSample>,
                                                    @JsonProperty("linearAcceleration") val linearAcceleration: List<SensorSample>)
    : SensorDataset(DatasetType.INERTIAL) {

    override fun toString(): String {
        return super.toString() + labelled(", steps: ", steps) + labelled(", dx[m]: ", dx) + labelled(", dy[m]: ", dy) + ", samples: " +
                (acceleration.size + linearAcceleration.size).toString()
    }

    var steps = 0
    var dx = 0f
    var dy = 0f

    private fun labelled(label: String, value: Number): String {
        return if (value != 0f) {
            label + value.toString()
        } else {
            ""
        }
    }

    enum class InertialMovementType {
        WALKING, RUNNING, STAIRS_UP, STAIRS_DOWN, NONE
    }
}