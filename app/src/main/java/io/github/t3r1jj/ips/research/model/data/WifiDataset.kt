package io.github.t3r1jj.ips.research.model.data

import android.content.Context
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.t3r1jj.ips.research.R

data class WifiDataset @JsonCreator constructor(@JsonProperty("place") var place: String,
                                                @JsonProperty("fingerprints") val fingerprints: List<Fingerprint>)
    : Dataset(DatasetType.WIFI) {

    var iterations = 10

    override fun toString(): String {
        return "{" + place + "} " + super.toString() + ", samples: " + fingerprints.size.toString()
    }

    override fun toString(context: Context): String {
        val samples = context.getString(R.string.samples).toLowerCase()
        return "{" + place + "} " + super.toString(context) + ", $samples: " + fingerprints.size.toString()
    }
}