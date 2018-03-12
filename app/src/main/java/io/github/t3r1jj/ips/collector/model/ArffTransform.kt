package io.github.t3r1jj.ips.collector.model

import io.github.t3r1jj.ips.collector.WifiActivity
import io.github.t3r1jj.ips.collector.model.data.Dataset
import io.github.t3r1jj.ips.collector.model.data.Fingerprint
import io.github.t3r1jj.ips.collector.model.data.WifiDataset
import java.io.FileOutputStream
import java.io.PrintWriter
import java.util.*

class ArffTransform(private val ssidRegex: Regex) {

    val attributes = LinkedHashSet<String>()
    val classes = mutableSetOf<String>()
    val objects = mutableMapOf<String, MutableList<String>>()
    val devices = mutableSetOf<String>()
    var startTimestamp = Long.MAX_VALUE
    var endTimestamp = Long.MIN_VALUE
    lateinit var type: String

    companion object {
        private const val NO_SIGNAL = -100
        private const val ADD_UNKNOWN = false
        private const val UNKNOWN_PLACE = "UNKNOWN"
    }

    fun apply(dataset: Iterable<WifiDataset>) {
        dataset.forEach {
            type = it.type.toString()
            devices.add(it.device)
            classes.add(it.place)
            if (objects[it.device] == null) {
                objects[it.device] = mutableListOf()
            }
            startTimestamp = Math.min(startTimestamp, it.timestamp)
            endTimestamp = Math.max(endTimestamp, it.timestamp)
            it.fingerprints
                    .filter { it.ssid.matches(ssidRegex) }
                    .forEach {
                        attributes.add(it.bssid)
                    }
        }
        dataset.forEach { data ->
            val datasetObjects = mutableListOf<String>()
            var attributeValues = mutableMapOf<String, Int>()
            var lastTimestamp = 0L
            var index = 0
            data.fingerprints
                    .sortedBy { it.timestamp }
                    .forEach {
                        if (attributeValues.containsKey(it.bssid) || isNewSamplingGroup(it, lastTimestamp)) {
                            val objectBuilder = toObject(attributeValues, data.place)
                            datasetObjects.add(objectBuilder.toString())
                            attributeValues = mutableMapOf()
                            index++
                        } else {
                            attributeValues.put(it.bssid, it.rssi)
                        }
                        lastTimestamp = it.timestamp
                    }
            for (i in index until data.iterations) {
                datasetObjects.add(toObject(mutableMapOf(), data.place).toString())
            }
            if (!data.device.contains("Combo")) {
                val sums = mutableListOf<Int>()
                for (obj in datasetObjects) {
                    val values = obj.split(",")
                    for (i in 0 until values.size - 1) {
                        try {
                            sums[i] += values[i].toInt()
                        } catch (ex: java.lang.IndexOutOfBoundsException) {
                            sums.add(values[i].toInt())
                        }
                    }
                }
                for (i in 0 until sums.size) {
                    sums[i] = sums[i] / datasetObjects.size
                }
                objects[data.device]!!.add(sums.joinToString() + "," + data.place)
            } else {
                objects[data.device]!!.addAll(datasetObjects)
            }
        }

        if (ADD_UNKNOWN) {
            devices.forEach {
                if (it.contains("Combo")) {
                    val avgObjectsPerClass = objects[it]!!.size / classes.size
                    for (i in 0 until avgObjectsPerClass) {
                        objects[it]!!.add(toObject(mutableMapOf(), UNKNOWN_PLACE).toString())
                    }
                } else {
                    objects[it]!!.add(toObject(mutableMapOf(), UNKNOWN_PLACE).toString())
                }
            }
            classes.add(UNKNOWN_PLACE)
        }
    }

    private fun isNewSamplingGroup(fingerprint: Fingerprint, previousTimestamp: Long): Boolean {
        return previousTimestamp != 0L && (fingerprint.timestamp - previousTimestamp >= WifiActivity.SamplingRate._500MS.delay)
    }

    private fun toObject(attributeValues: MutableMap<String, Int>, classValue: String): StringBuilder {
        val objectBuilder = StringBuilder()
        var prefix = ""
        attributes.forEach {
            val value = attributeValues[it] ?: NO_SIGNAL
            objectBuilder
                    .append(prefix)
                    .append(dBmToDecyPikoWatts(value.toDouble()))
            prefix = ","
        }
        objectBuilder
                .append(prefix)
                .append(classValue)
        return objectBuilder
    }

    private fun dBmToDecyPikoWatts(dBm: Double): Int {
        return Math.pow(10.toDouble(), (dBm + 100) / 10).toInt()
    }

    fun writeToFile(outputStream: FileOutputStream, device: String) {
        val writer = PrintWriter(outputStream)
        writer.println("% Date: " + Dataset.dateFormatter.format(Date()))
        writer.println("% Data type: " + type)
        writer.println("% Data collecting start: " + Dataset.dateFormatter.format(Date(startTimestamp)))
        writer.println("% Data collecting end: " + Dataset.dateFormatter.format(Date(endTimestamp)))
        writer.println("% Attributes from devices: " + Arrays.toString(devices.toTypedArray()))
        writer.println("% Device data: " + device)
        writer.println("% SSID regex: " + ssidRegex.pattern)

        writer.println("@RELATION " + type)
        writer.println()
        for (attribute in attributes) {
            writer.println("@ATTRIBUTE " + attribute + " NUMERIC")
        }
        writer.println()
        writer.println("@ATTRIBUTE place " + classes.sorted().joinToString(",", "{", "}"));
        writer.println()
        writer.println("@Data")
        objects[device]!!.forEach(writer::println)
        writer.close()
        outputStream.close()
    }
}