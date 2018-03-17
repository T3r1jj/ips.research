package io.github.t3r1jj.ips.research.model.algorithm

import io.github.t3r1jj.ips.research.WifiActivity
import io.github.t3r1jj.ips.research.model.data.Dataset
import io.github.t3r1jj.ips.research.model.data.DatasetType
import io.github.t3r1jj.ips.research.model.data.Fingerprint
import io.github.t3r1jj.ips.research.model.data.WifiDataset
import java.io.OutputStream
import java.io.PrintWriter
import java.lang.IndexOutOfBoundsException
import java.util.*

class ArffTransform(private val ssidRegex: Regex) {

    val attributes = LinkedHashSet<String>()
    val classes = mutableSetOf<String>()
    val objects = mutableMapOf<String, MutableList<String>>()
    val testDevices = mutableSetOf<String>()
    var trainDevices: String = ""
        get() = _trainDevices.joinToString(",", "train.")
    var startTimestamp = mutableMapOf<String, Long>()
    var endTimestamp = mutableMapOf<String, Long>()
    var addUnknownClassForTraining = false
    var addUnknownClassForTesting = false
    var padWithNoSignal = false
    var attributeDataType = AttributeDataType.POWER
    var type = ""
    private var _trainDevices = mutableSetOf<String>()

    enum class AttributeDataType {
        dBm, POWER
    }

    companion object {
        private const val NO_SIGNAL = -100
        private const val UNKNOWN_PLACE = "UNKNOWN"

        internal fun dBmToPikoWatt(dBm: Double): Int {
            return Math.pow(10.toDouble(), (dBm + 90) / 10).toInt()
        }

        internal fun pikoWattToDBm(pWatt: Double): Int {
            return if (pWatt <= 0) {
                NO_SIGNAL
            } else {
                Math.round(10 * Math.log10(pWatt) - 90).toInt()
            }
        }
    }

    fun apply(trainData: Iterable<WifiDataset>, testData: Iterable<WifiDataset>) {
        trainData.forEach {
            extractTrainMetadata(it)
        }
        testData.forEach {
            testDevices.add(it.device)
            extractMetadata(it.device, it.timestamp, it.type)
        }

        trainData.forEach { data ->
            extractMetadata(trainDevices, data.timestamp, data.type)
            val dataObjects = groupByIterations(data)
            if (padWithNoSignal) {
                padWithNoSignalObjects(dataObjects, data)
            }
            objects[trainDevices]!!.addAll(dataObjects)
        }

        testData.forEach { data ->
            val dataObjects = groupByIterations(data)
            if (padWithNoSignal) {
                padWithNoSignalObjects(dataObjects, data)
            }
            objects[data.device]!!.add(avg(dataObjects).joinToString(",") + "," + data.place)
        }

        if (addUnknownClassForTraining) {
            val avgObjectsPerClass = objects[trainDevices]!!.size / classes.size
            for (i in 0 until avgObjectsPerClass) {
                objects[trainDevices]!!.add(toObject(mutableMapOf(), UNKNOWN_PLACE).toString())
                classes.add(UNKNOWN_PLACE)
            }
        }
        if (addUnknownClassForTesting) {
            testDevices.forEach {
                objects[it]!!.add(toObject(mutableMapOf(), UNKNOWN_PLACE).toString())
            }
        }
    }

    private fun padWithNoSignalObjects(dataObjects: MutableList<String>, data: WifiDataset) {
        val objectCount = dataObjects.size
        for (i in objectCount until data.iterations) {
            dataObjects.add(toObject(mutableMapOf(), data.place).toString())
        }
    }

    private fun groupByIterations(data: WifiDataset): MutableList<String> {
        val datasetObjects = mutableListOf<String>()
        var attributeValues = mutableMapOf<String, Int>()
        var lastTimestamp = 0L
        data.fingerprints
                .sortedBy { it.timestamp }
                .forEach {
                    if (attributeValues.containsKey(it.bssid) || isNewSamplingGroup(it, lastTimestamp)) {
                        val objectBuilder = toObject(attributeValues, data.place)
                        datasetObjects.add(objectBuilder.toString())
                        attributeValues = mutableMapOf()
                    } else {
                        attributeValues.put(it.bssid, it.rssi)
                    }
                    lastTimestamp = it.timestamp
                }
        if (attributeValues.isNotEmpty()) {
            val objectBuilder = toObject(attributeValues, data.place)
            datasetObjects.add(objectBuilder.toString())
        }
        return datasetObjects
    }

    private fun extractTrainMetadata(it: WifiDataset) {
        _trainDevices.add(it.device)
        classes.add(it.place)
        it.fingerprints
                .filter { it.ssid.matches(ssidRegex) }
                .forEach {
                    attributes.add(it.bssid)
                }
    }

    private fun extractMetadata(device: String, timestamp: Long, type: DatasetType) {
        if (this.type.isBlank()) {
            this.type = type.toString()
        } else if (this.type != type.toString()) {
            throw RuntimeException("Trying to transform data that does not have common DatasetType")
        }
        if (objects[device] == null) {
            initFields(device)
        }
        startTimestamp[device] = Math.min(startTimestamp[device]!!, timestamp)
        endTimestamp[device] = Math.max(endTimestamp[device]!!, timestamp)
    }

    private fun initFields(device: String) {
        objects[device] = mutableListOf()
        startTimestamp[device] = Long.MAX_VALUE
        endTimestamp[device] = Long.MIN_VALUE
    }

    private fun avg(datasetObjects: MutableList<String>): List<Int> {
        val sums = mutableListOf<Int>()
        for (obj in datasetObjects) {
            val values = obj.split(",")
            for (i in 0 until values.size - 1) {
                try {
                    sums[i] += values[i].toInt()
                } catch (ex: IndexOutOfBoundsException) {
                    sums.add(values[i].toInt())
                }
            }
        }
        for (i in 0 until sums.size) {
            sums[i] = sums[i] / datasetObjects.size
        }
        return sums
    }

    private fun isNewSamplingGroup(fingerprint: Fingerprint, previousTimestamp: Long): Boolean {
        return previousTimestamp != 0L && (fingerprint.timestamp - previousTimestamp >= WifiActivity.SamplingRate._500MS.delay)
    }

    private fun toObject(attributeValues: MutableMap<String, Int>, classValue: String): StringBuilder {
        val objectBuilder = StringBuilder()
        var prefix = ""
        attributes.forEach {
            val value = attributeValues[it] ?: NO_SIGNAL
            objectBuilder.append(prefix)
            if (attributeDataType == AttributeDataType.dBm) {
                objectBuilder.append(value)
            } else {
                objectBuilder.append(dBmToPikoWatt(value.toDouble()))
            }
            prefix = ","
        }
        objectBuilder
                .append(prefix)
                .append(classValue)
        return objectBuilder
    }

    fun writeToFile(outputStream: OutputStream, device: String) {
        val writer = PrintWriter(outputStream)
        writer.println("% Date: " + Dataset.dateFormatter.format(Date()))
        writer.println("% Data type: " + type)
        writer.println("% Data collecting start: " + Dataset.dateFormatter.format(Date(startTimestamp[device] ?: System.currentTimeMillis())))
        writer.println("% Data collecting end: " + Dataset.dateFormatter.format(Date(endTimestamp[device] ?: System.currentTimeMillis())))
        writer.println("% Attributes from devices: " + trainDevices)
        writer.println("% Device data: " + device)
        writer.println("% SSID regex: " + ssidRegex.pattern)

        writer.println("@RELATION " + type)
        writer.println()
        for (attribute in attributes) {
            writer.println("@ATTRIBUTE $attribute NUMERIC")
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