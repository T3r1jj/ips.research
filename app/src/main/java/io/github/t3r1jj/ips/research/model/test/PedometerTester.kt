package io.github.t3r1jj.ips.research.model.test

import android.content.Context
import io.github.t3r1jj.ips.research.R
import io.github.t3r1jj.ips.research.model.algorithm.Pedometer
import io.github.t3r1jj.ips.research.model.algorithm.filter.FilterFactory
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
        var filter = "filtr"
        var movement = "ruch"
        var stepCount = "liczba kroków"
        var magn = "magn"
        var threshold = "próg"
        var sensitivity = "czułość"
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
        var filterTypes = mapOf(FilterFactory.FilterType.NO_FILTER to "brak filtra",
                FilterFactory.FilterType.MOVING_AVERAGE_FILTER to "średnia ruchoma",
                FilterFactory.FilterType.KALMAN_FILTER to "Kalmana"
        )
        var movementTypes = mapOf(
                InertialDataset.InertialMovementType.WALKING to "chodzenie",
                InertialDataset.InertialMovementType.RUNNING to "bieganie",
                InertialDataset.InertialMovementType.STAIRS_UP to "wchodzenie po schodach",
                InertialDataset.InertialMovementType.STAIRS_DOWN to "schodzenie po schodach",
                InertialDataset.InertialMovementType.ELEVATOR_UP to "windą w górę",
                InertialDataset.InertialMovementType.ELEVATOR_DOWN to "windą w dół",
                InertialDataset.InertialMovementType.NONE to "w miejscu"
        )

        fun loadI18n(context: Context) {
            this.pedometerTestOutput = i18n(R.string.pedometer_test_output, context)
            this.testDate = i18n(R.string.test_date, context)
            this.algorithm = i18n(R.string.algorithm, context)
            this.numberOfTestCases = i18n(R.string.number_of_test_cases, context)
            this.expected = i18n(R.string.expected, context)
            this.detected = i18n(R.string.detected, context)
            this.pedometerTestDebug = i18n(R.string.pedometer_test_debug, context)
            this.testCasesPlots = i18n(R.string.test_cases_plots, context)
            this.device = i18n(R.string.device, context)
            this.filter = i18n(R.string.filter, context)
            this.movement = i18n(R.string.movement_type, context).toLowerCase()
            this.stepCount = i18n(R.string.step_count, context)
            this.magn = i18n(R.string.magn, context)
            this.threshold = i18n(R.string.threshold, context).toLowerCase()
            this.sensitivity = i18n(R.string.sensitivity, context).toLowerCase()
            this.pedometerTestInfoOutput = i18n(R.string.pedometer_test_info, context)
            this.devices = i18n(R.string.devices, context)
            this.allDevices = i18n(R.string.all_devices, context)
            this.error = i18n(R.string.error_model, context)
            this.steps = i18n(R.string.steps_model, context)
            this.detectedSteps = i18n(R.string.detected_steps, context)
            this.stepsDifference = i18n(R.string.steps_difference, context)
            this.stepsPositiveDifference = i18n(R.string.steps_positive_difference, context)
            this.stepsNegativeDifference = i18n(R.string.steps_negative_difference, context)
            this.totalTime = i18n(R.string.total_time, context)
            this.filterTypes = FilterFactory.FilterType.values().map {
                it to getStringResourceByName(it.toString(), context).toLowerCase()
            }.toMap()
            this.movementTypes = InertialDataset.InertialMovementType.values().map {
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

    fun saveOutput(outputStream: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        writer.write(i18n.pedometerTestOutput)
        writer.newLine()
        writer.write("${i18n.testDate}: ")
        writer.write(Dataset.dateFormatter.format(Date()))
        writer.newLine()
        writer.write("${i18n.algorithm}: ")
        writer.write(getFormattedFilterType())
        writer.newLine()
        writer.write("${i18n.numberOfTestCases}: ")
        writer.write(output.size.toString())
        writer.newLine()
        val all = output
        all.forEach {
            writer.newLine()
            writer.newLine()
            writer.write(it.first.toString())
            writer.newLine()
            writer.write("${i18n.expected}: ")
            writer.write(it.first.steps.toString())
            writer.newLine()
            writer.write("${i18n.detected}: ")
            writer.write(it.second.toString())
        }
        writer.close()
    }

    fun generateDebug(dataset: Iterable<InertialDataset>, outputStream: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        writer.write("// ${i18n.pedometerTestDebug}")
        writer.newLine()
        writer.write("// ${i18n.testDate}: ")
        writer.write(Dataset.dateFormatter.format(Date()))
        writer.newLine()
        writer.write("// ${i18n.algorithm}: ")
        writer.write(getFormattedFilterType())
        writer.newLine()
        writer.write("// ${i18n.testCasesPlots}: ")
        writer.write(output.size.toString())
        writer.newLine()
        for (inertialData in dataset) {
            val pedometer = Pedometer(filterFactory.createFilter())
            for (acceleration in inertialData.acceleration) {
                pedometer.processSample(acceleration)
            }
            writer.newLine()
            writer.newLine()
            writer.write("// ${i18n.device}: ")
            writer.write(inertialData.toString())
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
            writer.write("aFMN = ")
            writer.write(pedometer.aNormalizedMagnitudes.joinToString(";", "[", "];"))
            writer.newLine()
            writer.write("aFMNmin = ")
            writer.write(pedometer.min.joinToString(";", "[", "]"))
            writer.newLine()
            writer.write("aFMNmax = ")
            writer.write(pedometer.max.joinToString(";", "[", "];"))
            writer.newLine()
            writer.write("aFMNsens = ")
            writer.write(pedometer.sensitivities.joinToString(";", "[", "];"))
            writer.newLine()
            val title = "title('${i18n.device}: " + formatDeviceName(inertialData) +
                    ", ${i18n.filter}: " + getFormattedFilterTypeI18n() +
                    ", ${i18n.movement}: " +
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
            writer.write("xs2png(gcf(),'ips/" + inertialData.timestamp.toString() + "." + filterFactory.filterType + "-axyz.png');")
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
            writer.write("xs2png(gcf(),'ips/" + inertialData.timestamp.toString() + "." + filterFactory.filterType + "-amagn.png');")
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
            writer.write("xs2png(gcf(),'ips/" + inertialData.timestamp.toString() + "." + filterFactory.filterType + "-afm.png');")
            writer.newLine()
            writer.write("clf;")
            writer.newLine()
            writer.write("plot(x,[aFMN, aFMNmin, aFMNmax, (aFMNmin+aFMNmax)/2]);")
            writer.newLine()
            writer.write(title)
            writer.newLine()
            writer.write(xLabel)
            writer.newLine()
            writer.write(yLabel)
            writer.newLine()
            writer.write("hl=legend(['norm(${i18n.filter}(${i18n.magn}(a))))';'min';'max';'${i18n.threshold}']);")
            writer.newLine()
            writer.write("mkdir('ips');")
            writer.newLine()
            writer.write("xs2png(gcf(),'ips/" + inertialData.timestamp.toString() + "." + filterFactory.filterType + "-postfilter.png');")
            writer.newLine()
            writer.write("clf;")
            writer.newLine()
            writer.write("plot(x,[(aFMNmax-aFMNmin)/2,aFMNsens]);")
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
            writer.write("xs2png(gcf(),'ips/" + inertialData.timestamp.toString() + "." + filterFactory.filterType + "-sensitivity.png');")
            writer.newLine()
            writer.write("clf;")
            writer.newLine()
        }
        writer.close()
    }

    private fun getFormattedFilterType(): String {
        return if (filterFactory.filterType == FilterFactory.FilterType.MOVING_AVERAGE_FILTER) {
            filterFactory.filterType.toString() + " (" + filterFactory.averagingWindowLength + ")"
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
        writer.write("${i18n.algorithm}:")
        writer.write(getFormattedFilterType())
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
                    appendDatasetTestInfo(writer, it.value, title + " " + it.key.first + " " + it.key.second)
                }
    }

    private fun appendDeviceMovementTestInfo(writer: BufferedWriter, deviceTestCases: List<Pair<InertialDataset, Int>>, title: String) {
        appendDatasetTestInfo(writer, deviceTestCases, title)
        deviceTestCases
                .groupBy { it.first.movementType }
                .forEach {
                    appendDatasetTestInfo(writer, it.value, title + " " + it.key)
                }
        deviceTestCases
                .groupBy { it.first.sensorDelay }
                .forEach {
                    appendDatasetTestInfo(writer, it.value, title + " " + it.key)
                }
    }

    private fun appendDatasetTestInfo(writer: BufferedWriter, deviceTestCases: List<Pair<InertialDataset, Int>>, title: String) {

        if (totalSteps(deviceTestCases) != 0) {
            writer.write(title)
            writer.write(" ${i18n.error}: ")
            writer.write(totalError(deviceTestCases).times(100f).toString())
            writer.write("%")
            writer.newLine()
        }
        writer.write(title)
        writer.write(" ${i18n.steps}: ")
        writer.write(totalSteps(deviceTestCases).toString())
        writer.newLine()
        writer.write(title)
        writer.write(" ${i18n.detectedSteps}: ")
        writer.write(totalStepsDetected(deviceTestCases).toString())
        writer.newLine()
        writer.write(title)
        writer.write(" ${i18n.stepsDifference}: ")
        writer.write(totalStepsDifference(deviceTestCases).toString())
        writer.newLine()
        writer.write(title)
        writer.write(" ${i18n.stepsPositiveDifference}: ")
        writer.write(totalStepsDifferencePositive(deviceTestCases).toString())
        writer.newLine()
        writer.write(title)
        writer.write(" ${i18n.stepsNegativeDifference}: ")
        writer.write(totalStepsDifferenceNegative(deviceTestCases).toString())
        writer.newLine()
        writer.write(title)
        writer.write(" ${i18n.totalTime}: ")
        writer.write(Math.round(totalTime(deviceTestCases)).toString())
        writer.write(" s")
        writer.newLine()
    }

    private fun totalTime(data: List<Pair<InertialDataset, Int>>) =
            data.sumByDouble { (it.first.acceleration.last().timestamp - it.first.acceleration.first().timestamp).toDouble() / 1000000000 }

}