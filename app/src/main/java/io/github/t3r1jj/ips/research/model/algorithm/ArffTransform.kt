package io.github.t3r1jj.ips.research.model.algorithm

import android.content.Context
import io.github.t3r1jj.ips.research.R
import io.github.t3r1jj.ips.research.WifiActivity
import io.github.t3r1jj.ips.research.model.data.Dataset
import io.github.t3r1jj.ips.research.model.data.DatasetType
import io.github.t3r1jj.ips.research.model.data.Fingerprint
import io.github.t3r1jj.ips.research.model.data.WifiDataset
import java.io.OutputStream
import java.io.PrintWriter
import java.util.*

@Suppress("EnumEntryName")
class ArffTransform(private val ssidRegex: Regex, val opts: Options) {
    constructor(ssidRegex: Regex) : this(ssidRegex, Options())

    class I18n {
        var date = "Date"
        var dataType = "Data type"
        var startDatetime = "Data collecting start datetime"
        var endDatetime = "Data collecting end datetime"
        var attributesFromDevices = "Attributes from devices"
        var devData = "Device data"
        var ssidRegexLabel = "SSID regex:"
        var processingFunction = "Dataset transformation:"
        var processingFunctions = Processing.values().map { it to it.toString() }.toMap()

        fun loadI18n(context: Context) {
            date = i18n(R.string.test_date, context)
            dataType = i18n(R.string.data_type, context)
            startDatetime = i18n(R.string.start_date_time, context)
            endDatetime = i18n(R.string.end_date_time, context)
            attributesFromDevices = i18n(R.string.attributes_from_devices, context)
            devData = i18n(R.string.dev_data, context)
            ssidRegexLabel = i18n(R.string.ssid_regex, context)
            processingFunction = i18n(R.string.dataset_transformation, context)
            processingFunctions = Processing.values().map {
                it to getStringResourceByName(it.toString(), context)
            }.toMap()
        }

        private fun i18n(resId: Int, context: Context): String {
            return context.getString(resId)
        }

        private fun getStringResourceByName(aString: String, context: Context): String {
            val packageName = context.packageName
            val resId = context.resources.getIdentifier(aString, "string", packageName)
            return context.getString(resId)
        }
    }

    val i18n = I18n()

    val attributes = LinkedHashSet<String>()
    val classes = mutableSetOf<String>()
    val objects = mutableMapOf<String, MutableList<String>>()
    val testDevices = mutableSetOf<String>()
    var trainDevices: String = ""
        get() = _trainDevices.joinToString(",", "train.")
    var startTimestamp = mutableMapOf<String, Long>()
    var endTimestamp = mutableMapOf<String, Long>()
    var type = ""
    private var _trainDevices = mutableSetOf<String>()

    class Options(var attributeDataType: ArffTransform.AttributeDataType,
                  var trainProcessing: ArffTransform.Processing,
                  var testProcessing: ArffTransform.Processing) {
        var addUnknownClassForTraining = false
        var addUnknownClassForTesting = false
        var padWithNoSignal = false

        constructor() : this(AttributeDataType.pWatt, Processing.MEDIAN, Processing.MEDIAN)
    }

    enum class AttributeDataType {
        dBm, pWatt
    }

    enum class Processing {
        NONE, AVERAGE, MEDIAN
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
            if (opts.padWithNoSignal) {
                padWithNoSignalObjects(dataObjects, data)
            }

            when (opts.trainProcessing) {
                Processing.MEDIAN -> objects[trainDevices]!!.addAll(toObjects(median(dataObjects, 10), data.place))
                Processing.AVERAGE -> objects[trainDevices]!!.add(toObject(avg(dataObjects), data.place))
                else -> objects[trainDevices]!!.addAll(toObjects(dataObjects, data.place))
            }
        }

        testData.forEach { data ->
            val dataObjects = groupByIterations(data)
            if (opts.padWithNoSignal) {
                padWithNoSignalObjects(dataObjects, data)
            }
            when (opts.testProcessing) {
                Processing.MEDIAN -> objects[data.device]!!.addAll(toObjects(median(dataObjects, 10), data.place))
                Processing.AVERAGE -> objects[data.device]!!.add(toObject(avg(dataObjects), data.place))
                else -> objects[data.device]!!.addAll(toObjects(dataObjects, data.place))
            }
        }

        if (opts.addUnknownClassForTraining) {
            val avgObjectsPerClass = objects[trainDevices]!!.size / classes.size
            for (i in 0 until avgObjectsPerClass) {
                objects[trainDevices]!!.add(toObject(toObjectValues(mutableMapOf()), UNKNOWN_PLACE))
                classes.add(UNKNOWN_PLACE)
            }
        }
        if (opts.addUnknownClassForTesting) {
            testDevices.forEach {
                objects[it]!!.add(toObject(toObjectValues(mutableMapOf()), UNKNOWN_PLACE))
            }
        }
    }

    private fun padWithNoSignalObjects(dataObjects: MutableList<List<Int>>, data: WifiDataset) {
        for (i in dataObjects.size until data.iterations) {
            dataObjects.add(toObjectValues(mutableMapOf()))
        }
    }

    private fun groupByIterations(data: WifiDataset): MutableList<List<Int>> {
        val datasetObjects = mutableListOf<List<Int>>()
        var attributeValues = mutableMapOf<String, Int>()
        var lastTimestamp = 0L
        data.fingerprints
                .sortedBy { it.timestamp }
                .forEach {
                    if (attributeValues.containsKey(it.bssid) || isNewSamplingGroup(it, lastTimestamp)) {
                        datasetObjects.add(toObjectValues(attributeValues))
                        attributeValues = mutableMapOf()
                    } else {
                        attributeValues[it.bssid] = it.rssi
                    }
                    lastTimestamp = it.timestamp
                }
        if (attributeValues.isNotEmpty()) {
            datasetObjects.add(toObjectValues(attributeValues))
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

    fun avg(datasetObjects: MutableList<List<Int>>): List<Float> {
        val sums = FloatArray(datasetObjects.first().size)
        for (obj in datasetObjects) {
            for (i in 0 until obj.size) {
                sums[i] += obj[i].toFloat()
            }
        }
        for (i in 0 until sums.size) {
            sums[i] = sums[i] / datasetObjects.size
        }
        return sums.asList()
    }

    fun median(datasetObjects: MutableList<List<Int>>, medianLength: Int): List<List<Float>> {
        val groupedObjects = datasetObjects
                .withIndex()
                .groupBy { it.index / medianLength }
                .map { it.value.map { it.value } }
        val outObjects = mutableListOf<List<Float>>()
        for (group in groupedObjects) {
            val matrix = Array<MutableList<Int>>(group.first().size, { _ -> mutableListOf() })
            for (obj in group) {
                for (i in 0 until obj.size) {
                    matrix[i].add(obj[i])
                }
            }
            outObjects.add(matrix.map { median(it) })
        }
        return outObjects
    }

    private fun median(values: List<Int>): Float {
        val sortedValues = values.sorted()
        return if (values.size % 2 == 0) {
            (sortedValues[values.size / 2] + sortedValues[values.size / 2 - 1]) / 2f
        } else {
            sortedValues[values.size / 2].toFloat()
        }
    }

    private fun isNewSamplingGroup(fingerprint: Fingerprint, previousTimestamp: Long): Boolean {
        return previousTimestamp != 0L && (fingerprint.timestamp - previousTimestamp >= WifiActivity.SamplingRate._500MS.delay)
    }

    private fun toObject(attributeValues: List<Number>, classValue: String): String {
        return attributeValues.joinToString(",", "", ",$classValue")
    }

    private fun toObjects(attributeValues: List<List<Number>>, classValue: String): List<String> {
        return attributeValues.map { toObject(it, classValue) }
    }

    private fun toObjectValues(attributeValues: MutableMap<String, Int>) = attributes.map {
        attributeValues[it] ?: NO_SIGNAL
    }.map {
        if (opts.attributeDataType == AttributeDataType.dBm) {
            it
        } else {
            dBmToPikoWatt(it.toDouble())
        }
    }


    fun writeToFile(outputStream: OutputStream, device: String) {
        val writer = PrintWriter(outputStream)
        writer.println("% ${i18n.date}: " + Dataset.dateFormatter.format(Date()))
        writer.println("% ${i18n.dataType}: $type " + opts.attributeDataType.toString())
        writer.println("% ${i18n.startDatetime}: " + Dataset.dateFormatter.format(Date(startTimestamp[device]
                ?: System.currentTimeMillis())))
        writer.println("% ${i18n.endDatetime}: " + Dataset.dateFormatter.format(Date(endTimestamp[device]
                ?: System.currentTimeMillis())))
        writer.println("% ${i18n.attributesFromDevices}: " + trainDevices)
        writer.println("% ${i18n.devData}: " + device)
        writer.println("% ${i18n.ssidRegexLabel} " + ssidRegex.pattern)
        if (trainDevices == device) {
            writer.println("% ${i18n.processingFunction}: " + i18n.processingFunctions[opts.trainProcessing])
        } else if (testDevices.contains(device)) {
            writer.println("% ${i18n.processingFunction}: " + i18n.processingFunctions[opts.testProcessing])
        }
        writer.println("@RELATION ${type}_" + opts.attributeDataType.toString())
        writer.println()
        for (attribute in attributes) {
            writer.println("@ATTRIBUTE $attribute NUMERIC")
        }
        writer.println()
        writer.println("@ATTRIBUTE place " + classes.sorted().joinToString(",", "{", "}"))
        writer.println()
        writer.println("@Data")
        objects[device]?.forEach(writer::println)
        writer.close()
        outputStream.close()
    }
}