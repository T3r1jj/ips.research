package io.github.t3r1jj.ips.research.model.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.text.SimpleDateFormat
import java.util.*

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = InertialDataset::class, name = "INERTIAL"),
        JsonSubTypes.Type(value = MagneticDataset::class, name = "MAGNETIC"),
        JsonSubTypes.Type(value = WifiDataset::class, name = "WIFI"))
open class Dataset(val type: DatasetType) {

    companion object {
        @SuppressLint("SimpleDateFormat")
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        private fun getDeviceName(): String {
            val manufacturer = Build.MANUFACTURER ?: "UNKNOWN MANUFACTURER"
            val model = Build.MODEL ?: "UNKNOWN MODEL"
            return if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
                capitalize(model)
            } else {
                capitalize(manufacturer) + " " + model
            }
        }


        private fun capitalize(text: String?): String {
            if (text == null || text.isEmpty()) {
                return ""
            }
            val first = text[0]
            return if (Character.isUpperCase(first)) {
                text
            } else {
                Character.toUpperCase(first) + text.substring(1)
            }
        }
    }

    val timestamp = Date().time
    val device = getDeviceName()

    override fun toString(): String {
        return "[" + type.toString() + "] " +
                device.replace("UNKNOWN ", "").replace("Unknown ", "") +
                " (" + dateFormatter.format(timestamp) + ")"
    }

    open fun toString(context: Context): String {
        return "[" + type.toString(context) + "] " +
                device.replace("UNKNOWN ", "").replace("Unknown ", "") +
                " (" + dateFormatter.format(timestamp) + ")"
    }
}