package io.github.t3r1jj.ips.collector.model.test

import io.github.t3r1jj.ips.collector.model.algorithm.Pedometer
import io.github.t3r1jj.ips.collector.model.data.Dataset
import io.github.t3r1jj.ips.collector.model.data.InertialDataset
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.*

class PedometerTester {
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
            val pedometer = Pedometer()
            for (acceleration in inertialData.acceleration) {
                pedometer.processSample(acceleration)
            }
            output.add(inertialData to pedometer.stepCount)
        }
    }

    fun totalError(output: List<Pair<InertialDataset, Int>>) = totalStepsDifference(output).toFloat() / totalSteps(output)

    fun saveOutput(outputStream: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        writer.write("Pedometer test output")
        writer.newLine()
        writer.write("Test date: ")
        writer.write(Dataset.dateFormatter.format(Date()))
        writer.newLine()
        val all = output
        all.forEach {
            writer.newLine()
            writer.newLine()
            writer.write(it.first.toString())
            writer.newLine()
            writer.write("Expected: ")
            writer.write(it.first.steps.toString())
            writer.newLine()
            writer.write("Detected: ")
            writer.write(it.second.toString())
        }
        writer.close()
    }

    fun generateDebug(dataset: Iterable<InertialDataset>, outputStream: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        writer.write("// Pedometer test debug (paste into Scilab)")
        writer.newLine()
        writer.write("// Test date: ")
        writer.write(Dataset.dateFormatter.format(Date()))
        writer.newLine()
        for (inertialData in dataset) {
            val pedometer = Pedometer()
            for (acceleration in inertialData.acceleration) {
                pedometer.processSample(acceleration)
            }
            writer.newLine()
            writer.newLine()
            writer.write("// Device: ")
            writer.write(inertialData.toString())
            writer.newLine()
            writer.write("// Expected: ")
            writer.write(inertialData.steps.toString())
            writer.newLine()
            writer.write("// Detected: ")
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
            writer.write("plot(x,[aFMN, aFMNmin, aFMNmax, (aFMNmin+aFMNmax)/2]);")
            writer.newLine()
            val title = "title('Urządzenie: " + formatDeviceName(inertialData) +
                    ", ruch: " + inertialData.movementType.toStringPL() +
                    ", liczba kroków: " + inertialData.steps +
                    ", wykryto: " + pedometer.stepCount + "');"
            writer.write(title)
            writer.newLine()
            val xLabel = "xlabel('t[s]');"
            writer.write(xLabel)
            writer.newLine()
            val yLabel = "ylabel('a[m/s^2]');"
            writer.write(yLabel)
            writer.newLine()
            writer.write("hl=legend(['norm(kalman(lowPass(magn(a))))';'min';'max';'threshold']);")
            writer.newLine()
            writer.write("mkdir('ips');")
            writer.newLine()
            writer.write("xs2png(gcf(),'ips/" + inertialData.timestamp.toString() + "-postfilter.png');")
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
            writer.write("hl=legend(['max-min';'sensitivity']);")
            writer.newLine()
            writer.write("mkdir('ips');")
            writer.newLine()
            writer.write("xs2png(gcf(),'ips/" + inertialData.timestamp.toString() + "-sensitivity.png');")
            writer.newLine()
            writer.write("clf;")
            writer.newLine()
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
            writer.write("xs2png(gcf(),'ips/" + inertialData.timestamp.toString() + "-axyz.png');")
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
            writer.write("hl=legend(['magn(a)=sqrt(aX^2+aY^2+aZ^2)']);")
            writer.newLine()
            writer.write("mkdir('ips');")
            writer.newLine()
            writer.write("xs2png(gcf(),'ips/" + inertialData.timestamp.toString() + "-amagn.png');")
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
            writer.write("hl=legend(['lowPass(magn(a))']);")
            writer.newLine()
            writer.write("mkdir('ips');")
            writer.newLine()
            writer.write("xs2png(gcf(),'ips/" + inertialData.timestamp.toString() + "-afm.png');")
            writer.newLine()
            writer.write("clf;")
            writer.newLine()
        }
        writer.close()
    }

    fun saveOutputInfo(outputStream: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        val deviceDatasets = output.groupBy { it.first.device }
        writer.write("Pedometer test info output")
        writer.newLine()
        writer.write("Test date: ")
        writer.write(Dataset.dateFormatter.format(Date()))
        writer.newLine()
        writer.write("Devices : ")
        writer.write(deviceDatasets.keys.joinToString())
        writer.newLine()
        writer.write("Datasets: ")
        writer.write(output.size.toString())
        writer.newLine()
        writer.newLine()
        appendDeviceTestInfo(writer, output, "ALL DEVICES")
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
            writer.write(" error: ")
            writer.write(totalError(deviceTestCases).times(100f).toString())
            writer.write("%")
            writer.newLine()
        }
        writer.write(title)
        writer.write(" steps: ")
        writer.write(totalSteps(deviceTestCases).toString())
        writer.newLine()
        writer.write(title)
        writer.write(" detected steps: ")
        writer.write(totalStepsDetected(deviceTestCases).toString())
        writer.newLine()
        writer.write(title)
        writer.write(" steps difference: ")
        writer.write(totalStepsDetected(deviceTestCases).toString())
        writer.newLine()
        writer.write(title)
        writer.write(" steps positive difference: ")
        writer.write(totalStepsDifferencePositive(deviceTestCases).toString())
        writer.newLine()
        writer.write(title)
        writer.write(" steps negative difference: ")
        writer.write(totalStepsDifferenceNegative(deviceTestCases).toString())
        writer.newLine()
    }

}