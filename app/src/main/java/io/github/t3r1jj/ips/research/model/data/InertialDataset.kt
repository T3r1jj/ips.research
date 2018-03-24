package io.github.t3r1jj.ips.research.model.data

import android.content.Context
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.t3r1jj.ips.research.R
import io.github.t3r1jj.ips.research.model.collector.SensorSample

data class InertialDataset @JsonCreator constructor(@JsonProperty("movementType") val movementType: InertialMovementType,
                                                    @JsonProperty("acceleration") val acceleration: List<SensorSample>)
    : SensorDataset(DatasetType.INERTIAL) {

    override fun toString(): String {
        return super.toString() + labelled(", steps: ", steps) + labelled(", displacement [m]: ", displacement) + ", samples: " +
                acceleration.size
    }

    override fun toString(context: Context): String {
        val stepsLabel = context.getString(R.string.steps).toLowerCase()
        val displacement = context.getString(R.string.displacement).toLowerCase()
        val samples = context.getString(R.string.samples).toLowerCase()
        return super.toString(context) + labelled(", $stepsLabel ", steps) + labelled(", $displacement ", this.displacement) + ", $samples: " +
                acceleration.size
    }

    var steps = 0
    var displacement = 0f

    private fun labelled(label: String, value: Number): String {
        return if (value != 0f) {
            label + value.toString()
        } else {
            ""
        }
    }

    enum class InertialMovementType {
        WALKING, RUNNING, STAIRS_UP, STAIRS_DOWN, ELEVATOR_UP, ELEVATOR_DOWN, NONE;
    }
}