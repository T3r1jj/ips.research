package io.github.t3r1jj.ips.research.model.test

import android.content.Context
import io.github.t3r1jj.ips.research.R
import io.github.t3r1jj.ips.research.model.algorithm.Pedometer
import io.github.t3r1jj.ips.research.model.algorithm.filter.FilterFactory
import io.github.t3r1jj.ips.research.model.collector.SensorDelay
import io.github.t3r1jj.ips.research.model.data.Dataset
import io.github.t3r1jj.ips.research.model.data.InertialDataset
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.*

class PedometerTester(private val filterFactory: FilterFactory) {
    val i18n = I18N()

    constructor(filterFactory: FilterFactory, context: Context) : this(filterFactory) {
        i18n.loadI18n(context)
    }

    class I18N {
        var pedometerTestOutput = "Pedometer test output"
        var testDate = "Test date"
        var algorithm = "Algorithm"
        var numberOfTestCases = "Number of test cases"
        var expected = "Expected"
        var detected = "Detected"
        var pedometerTestDebug = "Pedometer test debug (paste into Scilab)"
        var testCasesPlots = "Test cases (x5 plots)"
        var device = "Device"
        var filter = "filter"
        var movement = "movement type"
        var stepCount = "step count"
        var magn = "magn"
        var threshold = "threshold"
        var sensitivity = "sensitivity threshold"
        var pedometerTestInfoOutput = "Pedometer test info output"
        var devices = "Devices"
        var allDevices = "ALL DEVICES"
        var error = "error"
        var steps = "steps"
        var detectedSteps = "detected steps"
        var stepsDifference = "steps difference"
        var stepsPositiveDifference = "steps positive difference"
        var stepsNegativeDifference = "steps negative difference"
        var totalTime = "total time"
        var samplingRate = "sampling rate"
        var accuracy = "accuracy"
        var filterTypes = mapOf(FilterFactory.FilterType.NO_FILTER to "NO_FILTER",
                FilterFactory.FilterType.MOVING_AVERAGE_FILTER to "MOVING_AVERAGE",
                FilterFactory.FilterType.KALMAN_FILTER to "KALMAN"
        )
        var movementTypes = mapOf(
                InertialDataset.InertialMovementType.WALKING to "WALKING",
                InertialDataset.InertialMovementType.RUNNING to "RUNNING",
                InertialDataset.InertialMovementType.STAIRS_UP to "STAIRS UP",
                InertialDataset.InertialMovementType.STAIRS_DOWN to "STAIRS DOWN",
                InertialDataset.InertialMovementType.ELEVATOR_UP to "ELEVEATOR UP",
                InertialDataset.InertialMovementType.ELEVATOR_DOWN to "ELEVATOR DOWN",
                InertialDataset.InertialMovementType.NONE to "NONE"
        )
        var sensorDelays = mapOf(
                SensorDelay.FASTEST to "FASTEST",
                SensorDelay.NORMAL to "NORMAL"
        )


        fun loadI18n(context: Context) {
            pedometerTestOutput = i18n(R.string.pedometer_test_output, context)
            testDate = i18n(R.string.test_date, context)
            algorithm = i18n(R.string.algorithm, context)
            numberOfTestCases = i18n(R.string.number_of_test_cases, context)
            expected = i18n(R.string.expected, context)
            detected = i18n(R.string.detected, context)
            pedometerTestDebug = i18n(R.string.pedometer_test_debug, context)
            testCasesPlots = i18n(R.string.test_cases_plots, context)
            device = i18n(R.string.device, context)
            filter = i18n(R.string.filter, context)
            movement = i18n(R.string.movement_type, context).toLowerCase()
            stepCount = i18n(R.string.step_count, context)
            magn = i18n(R.string.magn, context)
            threshold = i18n(R.string.threshold, context).toLowerCase()
            sensitivity = i18n(R.string.sensitivity, context).toLowerCase()
            pedometerTestInfoOutput = i18n(R.string.pedometer_test_info, context)
            devices = i18n(R.string.devices, context)
            allDevices = i18n(R.string.all_devices, context)
            error = i18n(R.string.error_model, context)
            accuracy = i18n(R.string.accuracy_model, context)
            steps = i18n(R.string.steps_model, context)
            detectedSteps = i18n(R.string.detected_steps, context)
            stepsDifference = i18n(R.string.steps_difference, context)
            stepsPositiveDifference = i18n(R.string.steps_positive_difference, context)
            stepsNegativeDifference = i18n(R.string.steps_negative_difference, context)
            totalTime = i18n(R.string.total_time, context)
            samplingRate = i18n(R.string.sampling_rate, context)
            filterTypes = FilterFactory.FilterType.values().map {
                it to getStringResourceByName(it.toString(), context).toLowerCase()
            }.toMap()
            movementTypes = InertialDataset.InertialMovementType.values().map {
                it to getStringResourceByName(it.toString(), context).toLowerCase()
            }.toMap()
            sensorDelays = SensorDelay.values().map {
                it to getStringResourceByName(it.toString(), context).toLowerCase()
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

    val output = mutableListOf<Pair<InertialDataset, Int>>()

    fun totalSteps(data: List<Pair<InertialDataset, Int>>) =
            data.sumBy { it.first.steps }

    fun totalStepsDetected(data: List<Pair<InertialDataset, Int>>) =
            data.sumBy { it.second }

    fun totalStepsDifference(data: List<Pair<InertialDataset, Int>>) =
            data.sumBy { Math.abs(it.second - it.first.steps) }

    fun maxAccuracy(data: List<Pair<InertialDataset, Int>>) =
            data.filter {
                it.first.steps > 0
            }.map { 1f - Math.abs(it.second - it.first.steps).div(it.first.steps.toFloat()) }.max()!!

    fun minAccuracy(data: List<Pair<InertialDataset, Int>>) =
            data.filter {
                it.first.steps > 0
            }.map {
                1f - Math.abs(it.second - it.first.steps).div(it.first.steps.toFloat())
            }.min()!!

    fun varAccuracy(data: List<Pair<InertialDataset, Int>>): Double {
        val avg = avgAccuracy(data)
        val filtered = data.filter {
            it.first.steps > 0
        }
        return filtered
                .sumByDouble {
                    val acc = 1.toDouble() - Math.abs(it.second - it.first.steps).div(it.first.steps.toDouble())
                    val diff = avg - acc
                    diff * diff
                } / filtered.size
    }

    fun frequency(data: List<Pair<InertialDataset, Int>>) =
            data.sumBy { it.first.acceleration.size } / totalTime(data).toFloat()

    fun avgAccuracy(data: List<Pair<InertialDataset, Int>>): Double {
        val filtered = data.filter {
            it.first.steps > 0
        }
        return filtered.sumByDouble { 1.toDouble() - Math.abs(it.second - it.first.steps).div(it.first.steps.toDouble()) } / filtered.size
    }

    fun totalStepsDifferencePositive(data: List<Pair<InertialDataset, Int>>) =
            data.sumBy { if (it.first.steps < it.second) Math.abs(it.second - it.first.steps) else 0 }

    fun totalStepsDifferenceNegative(data: List<Pair<InertialDataset, Int>>) =
            data.sumBy { if (it.first.steps > it.second) Math.abs(it.second - it.first.steps) else 0 }

    fun test(dataset: Iterable<InertialDataset>) {
        for (inertialData in dataset) {
            val pedometer = Pedometer(filterFactory.createFilter())
            for (acceleration in inertialData.acceleration) {
                pedometer.processSample(acceleration)
            }
            output.add(inertialData to pedometer.stepCount)
        }
    }

    fun totalError(output: List<Pair<InertialDataset, Int>>) = totalStepsDifference(output).toFloat() / totalSteps(output)

    fun saveOutput(outputStream: OutputStream, context: Context?) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        writer.write(i18n.pedometerTestOutput)
        writer.newLine()
        writer.write("${i18n.testDate}: ")
        writer.write(Dataset.dateFormatter.format(Date()))
        writer.newLine()
        writer.write("${i18n.algorithm}: ")
        writer.write(getFormattedFilterTypeI18n())
        writer.newLine()
        writer.write("${i18n.numberOfTestCases}: ")
        writer.write(output.size.toString())
        writer.newLine()
        val all = output
        all.forEach {
            writer.newLine()
            writer.newLine()
            if (context == null) {
                writer.write(it.first.toString())
            } else {
                writer.write(it.first.toString(context))
            }
            writer.newLine()
            writer.write("${i18n.expected}: ")
            writer.write(it.first.steps.toString())
            writer.newLine()
            writer.write("${i18n.detected}: ")
            writer.write(it.second.toString())
        }
        writer.close()
    }

    fun generateDebug(dataset: Iterable<InertialDataset>, outputStream: OutputStream, context: Context?) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        writer.write("// ${i18n.pedometerTestDebug}")
        writer.newLine()
        writer.write("// ${i18n.testDate}: ")
        writer.write(Dataset.dateFormatter.format(Date()))
        writer.newLine()
        writer.write("// ${i18n.algorithm}: ")
        writer.write(getFormattedFilterTypeI18n())
        writer.newLine()
        writer.write("// ${i18n.testCasesPlots}: ")
        writer.write(output.size.toString())
        writer.newLine()
        writer.newLine()
        writer.write("gcf().figure_size = [1200,600];")
        writer.newLine()
        for (inertialData in dataset) {
            val pedometer = Pedometer(filterFactory.createFilter())
            for (acceleration in inertialData.acceleration) {
                pedometer.processSample(acceleration)
            }
            writer.newLine()
            writer.newLine()
            writer.write("// ${i18n.device}: ")
            if (context == null) {
                writer.write(inertialData.toString())
            } else {
                writer.write(inertialData.toString(context))
            }
            writer.newLine()
            writer.write("// ${i18n.expected}: ")
            writer.write(inertialData.steps.toString())
            writer.newLine()
            writer.write("// ${i18n.detected}: ")
            writer.write(pedometer.stepCount.toString())
            writer.newLine()
            writer.write("x = ")
            writer.write(pedometer.t
                    .map { it - pedometer.t.first() }
                    .map { it / 1000000000f }
                    .joinToString(";", "[", "];")
            )
            writer.newLine()
            writer.write("aX = ")
            writer.write(pedometer.a.map { it[0] }.joinToString(";", "[", "];"))
            writer.newLine()
            writer.write("aY = ")
            writer.write(pedometer.a.map { it[1] }.joinToString(";", "[", "];"))
            writer.newLine()
            writer.write("aZ = ")
            writer.write(pedometer.a.map { it[2] }.joinToString(";", "[", "];"))
            writer.newLine()
            writer.write("aM = ")
            writer.write(pedometer.aMagnitudes.joinToString(";", "[", "];"))
            writer.newLine()
            writer.write("aFM = ")
            writer.write(pedometer.aFilteredMagnitudes.joinToString(";", "[", "];"))
            writer.newLine()
            writer.write("aFMmin = ")
            writer.write(pedometer.min.joinToString(";", "[", "]"))
            writer.newLine()
            writer.write("aFMmax = ")
            writer.write(pedometer.max.joinToString(";", "[", "];"))
            writer.newLine()
            writer.write("aFMsens = ")
            writer.write(pedometer.sensitivities.joinToString(";", "[", "];"))
            writer.newLine()
            val title = "title('${i18n.device}: " + formatDeviceName(inertialData) +
                    ", ${i18n.filter}: " + getFormattedFilterTypeI18n() +
                    ", ${i18n.samplingRate.toLowerCase()} " + (i18n.sensorDelays[inertialData.sensorDelay]
                    ?: inertialData.sensorDelay.toString()) +
                    ", ${i18n.movement} " +
                    (i18n.movementTypes[inertialData.movementType]
                            ?: inertialData.movementType.toString()) +
                    ", ${i18n.stepCount}: " + inertialData.steps +
                    ", ${i18n.detected.toLowerCase()}: " + pedometer.stepCount + "');"
            val xLabel = "xlabel('t[s]');"
            val yLabel = "ylabel('a[m/s^2]');"
            writer.write("plot(x,[aX,aY,aZ]);")
            writer.newLine()
            writer.write(title)
            writer.newLine()
            writer.write(xLabel)
            writer.newLine()
            writer.write(yLabel)
            writer.newLine()
            writer.write("hl=legend(['aX';'aY';'aZ']);")
            writer.newLine()
            writer.write("mkdir('ips');")
            writer.newLine()
            writer.write("xs2png(gcf(),'ips/" + inertialData.timestamp.toString() + "." + getFormattedFilterType() + "-axyz.png');")
            writer.newLine()
            writer.write("clf;")
            writer.newLine()
            writer.write("plot(x,[aM]);")
            writer.newLine()
            writer.write(title)
            writer.newLine()
            writer.write(xLabel)
            writer.newLine()
            writer.write(yLabel)
            writer.newLine()
            writer.write("hl=legend(['${i18n.magn}(a)=sqrt(aX^2+aY^2+aZ^2)']);")
            writer.newLine()
            writer.write("mkdir('ips');")
            writer.newLine()
            writer.write("xs2png(gcf(),'ips/" + inertialData.timestamp.toString() + "." + getFormattedFilterType() + "-amagn.png');")
            writer.newLine()
            writer.write("clf;")
            writer.newLine()
            writer.write("plot(x,aFM);")
            writer.newLine()
            writer.write(title)
            writer.newLine()
            writer.write(xLabel)
            writer.newLine()
            writer.write(yLabel)
            writer.newLine()
            writer.write("hl=legend(['${i18n.filter}(${i18n.magn}(a))']);")
            writer.newLine()
            writer.write("mkdir('ips');")
            writer.newLine()
            writer.write("xs2png(gcf(),'ips/" + inertialData.timestamp.toString() + "." + getFormattedFilterType() + "-afm.png');")
            writer.newLine()
            writer.write("clf;")
            writer.newLine()
            writer.write("plot(x,[aFM, aFMmin, aFMmax, (aFMmin+aFMmax)/2]);")
            writer.newLine()
            writer.write(title)
            writer.newLine()
            writer.write(xLabel)
            writer.newLine()
            writer.write(yLabel)
            writer.newLine()
            writer.write("hl=legend(['${i18n.filter}(${i18n.magn}(a))';'min';'max';'${i18n.threshold}']);")
            writer.newLine()
            writer.write("mkdir('ips');")
            writer.newLine()
            writer.write("xs2png(gcf(),'ips/" + inertialData.timestamp.toString() + "." + getFormattedFilterType() + "-threshold.png');")
            writer.newLine()
            writer.write("clf;")
            writer.newLine()
            writer.write("plot(x,[(aFMmax-aFMmin),aFMsens]);")
            writer.newLine()
            writer.write(title)
            writer.newLine()
            writer.write(xLabel)
            writer.newLine()
            writer.write(yLabel)
            writer.newLine()
            writer.write("hl=legend(['max-min';'${i18n.sensitivity}']);")
            writer.newLine()
            writer.write("mkdir('ips');")
            writer.newLine()
            writer.write("xs2png(gcf(),'ips/" + inertialData.timestamp.toString() + "." + getFormattedFilterType() + "-sensitivity.png');")
            writer.newLine()
            writer.write("clf;")
            writer.newLine()
        }
        writer.close()
    }

    private fun getFormattedFilterType(): String {
        return if (filterFactory.filterType == FilterFactory.FilterType.MOVING_AVERAGE_FILTER) {
            filterFactory.filterType.toString() + "-" + filterFactory.averagingWindowLength
        } else {
            filterFactory.filterType.toString()
        }
    }

    private fun getFormattedFilterTypeI18n(): String {
        return if (filterFactory.filterType == FilterFactory.FilterType.MOVING_AVERAGE_FILTER) {
            (i18n.filterTypes[filterFactory.filterType]
                    ?: filterFactory.filterType.toString()) + " (" + filterFactory.averagingWindowLength + ")"
        } else {
            i18n.filterTypes[filterFactory.filterType] ?: filterFactory.filterType.toString()
        }
    }

    fun saveOutputInfo(outputStream: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        val deviceDatasets = output.groupBy { it.first.device }
        writer.write(i18n.pedometerTestInfoOutput)
        writer.newLine()
        writer.write("${i18n.testDate}: ")
        writer.write(Dataset.dateFormatter.format(Date()))
        writer.newLine()
        writer.write("${i18n.algorithm}: ")
        writer.write(getFormattedFilterTypeI18n())
        writer.newLine()
        writer.write("${i18n.devices}: ")
        writer.write(deviceDatasets.keys.joinToString())
        writer.newLine()
        writer.write("${i18n.numberOfTestCases}: ")
        writer.write(output.size.toString())
        writer.newLine()
        writer.newLine()
        appendDeviceTestInfo(writer, output, i18n.allDevices)
        for (deviceDataset in deviceDatasets) {
            writer.newLine()
            writer.newLine()
            appendDeviceTestInfo(writer, deviceDataset.value, deviceDataset.key)
        }
        writer.close()
    }

    private fun formatDeviceName(inertialData: InertialDataset) =
            inertialData.device.replace("Unknown ", "").replace("Android Combo", "Pentagram Combo 4-Core")

    private fun appendDeviceTestInfo(writer: BufferedWriter, deviceTestCases: List<Pair<InertialDataset, Int>>, title: String) {
        appendDeviceMovementTestInfo(writer, deviceTestCases, title)
        deviceTestCases
                .groupBy { Pair(it.first.movementType, it.first.sensorDelay) }
                .forEach {
                    appendDatasetTestInfo(writer, it.value, "$title, " + (i18n.movementTypes[it.key.first]
                            ?: it.key.first).toString().toUpperCase() + ", " + (i18n.sensorDelays[it.key.second]
                            ?: it.key.second).toString().toUpperCase())
                }
    }

    private fun appendDeviceMovementTestInfo(writer: BufferedWriter, deviceTestCases: List<Pair<InertialDataset, Int>>, title: String) {
        appendDatasetTestInfo(writer, deviceTestCases, title)
        deviceTestCases
                .groupBy { it.first.movementType }
                .forEach {
                    appendDatasetTestInfo(writer, it.value, "$title, " + (i18n.movementTypes[it.key]
                            ?: it.key).toString().toUpperCase())
                }
        deviceTestCases
                .groupBy { it.first.sensorDelay }
                .forEach {
                    appendDatasetTestInfo(writer, it.value, "$title, " + (i18n.sensorDelays[it.key]
                            ?: it.key).toString().toUpperCase())
                }
    }

    private fun appendDatasetTestInfo(writer: BufferedWriter, deviceTestCases: List<Pair<InertialDataset, Int>>, title: String) {
        writer.write(title)
        writer.write(": ${i18n.numberOfTestCases} = ")
        writer.write(deviceTestCases.size.toString())
        writer.newLine()
        if (totalSteps(deviceTestCases) != 0) {
            writer.write(title)
            writer.write(": ${i18n.error} = ")
            writer.write(totalError(deviceTestCases).times(100f).toString())
            writer.write("%")
            writer.newLine()
            writer.write(title)
            writer.write(": ${i18n.accuracy} = ")
            writer.write(100f.minus(totalError(deviceTestCases).times(100f)).toString())
            writer.write("%")
            writer.newLine()
            writer.write(title)
            writer.write(": min ${i18n.accuracy} = ")
            writer.write(minAccuracy(deviceTestCases).times(100f).toString())
            writer.write("%")
            writer.newLine()
            writer.write(title)
            writer.write(": max ${i18n.accuracy} = ")
            writer.write(maxAccuracy(deviceTestCases).times(100f).toString())
            writer.write("%")
            writer.newLine()
            writer.write(title)
            writer.write(": avg ${i18n.accuracy} per test = ")
            writer.write(avgAccuracy(deviceTestCases).times(100f).toString())
            writer.write("%")
            writer.newLine()
            writer.write(title)
            writer.write(": var ${i18n.accuracy} per test = ")
            writer.write(varAccuracy(deviceTestCases).times(100f).toString())
            writer.write("%")
            writer.newLine()
        }
        writer.write(title)
        writer.write(": ${i18n.steps} = ")
        writer.write(totalSteps(deviceTestCases).toString())
        writer.newLine()
        writer.write(title)
        writer.write(": ${i18n.detectedSteps} = ")
        writer.write(totalStepsDetected(deviceTestCases).toString())
        writer.newLine()
        writer.write(title)
        writer.write(": ${i18n.stepsDifference} = ")
        writer.write(totalStepsDifference(deviceTestCases).toString())
        writer.newLine()
        writer.write(title)
        writer.write(": ${i18n.stepsPositiveDifference} = ")
        writer.write(totalStepsDifferencePositive(deviceTestCases).toString())
        writer.newLine()
        writer.write(title)
        writer.write(": ${i18n.stepsNegativeDifference} = ")
        writer.write(totalStepsDifferenceNegative(deviceTestCases).toString())
        writer.newLine()
        writer.write(title)
        writer.write(": ${i18n.totalTime} = ")
        writer.write(Math.round(totalTime(deviceTestCases)).toString())
        writer.write(" s")
        writer.newLine()
        writer.write(title)
        writer.write(": f = ")
        writer.write(frequency(deviceTestCases).toString())
        writer.write(" Hz")
        writer.newLine()
    }

    private fun totalTime(data: List<Pair<InertialDataset, Int>>) =
            data.sumByDouble { (it.first.acceleration.last().timestamp - it.first.acceleration.first().timestamp).toDouble() / 1000000000 }

}