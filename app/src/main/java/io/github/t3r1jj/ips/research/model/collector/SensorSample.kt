package io.github.t3r1jj.ips.research.model.collector

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class SensorSample @JsonCreator constructor(@JsonProperty("data") val data: FloatArray,
                                                 @JsonProperty("timestamp") val timestamp: Long) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorSample

        if (!Arrays.equals(data, other.data)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(data)
        result = 31 * result + timestamp.hashCode()
        return result
    }
}