package io.github.t3r1jj.ips.collector.model.data

import android.annotation.SuppressLint
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.t3r1jj.ips.collector.InertialActivity
import io.github.t3r1jj.ips.collector.model.sampler.SensorSample
import java.text.SimpleDateFormat

data class InertialDataset @JsonCreator constructor(@JsonProperty("movementType") val movementType: InertialActivity.InertialMovementType,
                                                    @JsonProperty("acceleration") val acceleration: List<SensorSample>,
                                                    @JsonProperty("linearAcceleration") val linearAcceleration: List<SensorSample>,
                                                    @JsonProperty("rotation") val rotation: List<SensorSample>,
                                                    @JsonProperty("gravity") val gravity: List<SensorSample>)
    : SensorDataset(DatasetType.INERTIAL) {

    override fun toString(): String {
        return "[" + type.toString() + "] " + movementType.toString() + " (" + dateFormatter.format(timestamp) + ") steps: " + steps.toString() +
                ", samples: " + (acceleration.size + linearAcceleration.size + rotation.size + gravity.size).toString()
    }

    var steps = 0

    companion object {
        @SuppressLint("SimpleDateFormat")
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    }
}