package io.github.t3r1jj.ips.collector.model.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class Fingerprint @JsonCreator constructor(@JsonProperty("bssid") val bssid: String,
                                                @JsonProperty("rssi") val rssi: Int,
                                                @JsonProperty("timestamp") val timestamp: Long,
                                                @JsonProperty("ssid") val ssid: String
)