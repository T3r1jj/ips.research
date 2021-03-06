package io.github.t3r1jj.ips.research.model.data

import android.content.Context
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.t3r1jj.ips.research.R
import io.github.t3r1jj.ips.research.model.collector.SensorSample

data class MagneticDataset @JsonCreator constructor(@JsonProperty("route") val route: String,
                                                    @JsonProperty("magneticField") val magneticField: List<SensorSample>,
                                                    @JsonProperty("gravity") val gravity: List<SensorSample>)
    : SensorDataset(DatasetType.MAGNETIC) {

    override fun toString(): String {
        return "{" + route + "} " + super.toString() + ", samples: " + (magneticField.size + gravity.size).toString()
    }

    override fun toString(context: Context): String {
        val samples = context.getString(R.string.samples).toLowerCase()
        return "{" + route + "} " + super.toString(context) + ", $samples: " + (magneticField.size + gravity.size).toString()
    }
}