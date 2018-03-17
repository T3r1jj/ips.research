package io.github.t3r1jj.ips.collector

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.github.mikephil.charting.charts.LineChart
import io.github.t3r1jj.ips.collector.model.Dao
import io.github.t3r1jj.ips.collector.model.algorithm.Pedometer
import io.github.t3r1jj.ips.collector.model.algorithm.WifiNavigator
import io.github.t3r1jj.ips.collector.model.collector.InertialSampler
import io.github.t3r1jj.ips.collector.model.collector.InertialSampler.InertialSamplerListener
import io.github.t3r1jj.ips.collector.model.collector.SensorSample
import io.github.t3r1jj.ips.collector.model.collector.WifiSampler
import io.github.t3r1jj.ips.collector.model.data.DatasetType
import io.github.t3r1jj.ips.collector.model.data.WifiDataset
import io.github.t3r1jj.ips.collector.view.RealtimeChart
import io.github.t3r1jj.ips.collector.view.RenderableView
import trikita.anvil.Anvil
import trikita.anvil.BaseDSL
import trikita.anvil.DSL.*
import weka.classifiers.bayes.BayesNet
import java.lang.Thread.sleep

class OnlineActivity : AppCompatActivity() {

    private val navigator = WifiNavigator(BayesNet(), Regex("(eduroam|dziekanat|pb-guest|.*hotspot.*)", RegexOption.IGNORE_CASE))
    private var place = ""
    private var steps = 0
    private lateinit var wifiSampler: WifiSampler
    private lateinit var testSet: WifiDataset
    private var pollerThread: Thread? = null
    lateinit var inertialSampler: InertialSampler
    lateinit var pedometerChart: LineChart
    lateinit var sensitivityChart: LineChart
    lateinit var pedometer: Pedometer
    private lateinit var chartRenderer: RealtimeChartRenderer
    private val pedometerChartLabels = arrayOf("aNorm", "min", "max", "threshold")
    private val sensitivityChartLabels = arrayOf("sensitivity", "max-min")

    private fun createPollerThread(): Thread {
        return Thread({
            try {
                while (!Thread.interrupted() && wifiSampler.running) {
                    sleep(1000)
                }
                testSet = WifiDataset("?", wifiSampler.fingerprints)
                testSet.iterations = wifiSampler.sampleIndex
                place = "CLASSIFYING..."
                classify()
            } catch (iex: InterruptedException) {
            }
        })
    }

    private inner class RealtimeChartRenderer(context: Context) : RealtimeChart(context) {

        var lastRenderIndex = 0

        override fun render(): Boolean {
            try {
                for (i in lastRenderIndex + 1 until pedometer.t.size) {
                    val pedometerData = FloatArray(4)
                    pedometerData[0] = pedometer.aNormalizedMagnitudes[i]
                    pedometerData[1] = pedometer.min[i]
                    pedometerData[2] = pedometer.max[i]
                    pedometerData[3] = (pedometerData[1] + pedometerData[2]) / 2f
                    val time = (pedometer.t[i] - pedometer.t.first()) / 1000000000f
                    chartRenderer.labels = pedometerChartLabels
                    chartRenderer.addChartEntry(pedometerChart, pedometerData, time)
                    val pedometerSensitivity = FloatArray(2)
                    pedometerSensitivity[0] = pedometer.sensitivities[i]
                    pedometerSensitivity[1] = pedometer.max[i] - pedometer.min[i]
                    chartRenderer.labels = sensitivityChartLabels
                    chartRenderer.addChartEntry(sensitivityChart, pedometerSensitivity, time)
                    lastRenderIndex = i
                }
            } catch (ioobe: IndexOutOfBoundsException) {
                Log.w("Charting", "IndexOutOfBoundsException")
                lastRenderIndex = 0
            }
            return inertialSampler.isRunning
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifiSampler = WifiSampler(this)
        chartRenderer = RealtimeChartRenderer(this)
        inertialSampler = InertialSampler(this)
        pedometerChart = chartRenderer.createChart(0.5f, 1.5f)
        pedometerChart.description.text = "Pedometer algorithm"
        sensitivityChart = chartRenderer.createChart(0f, 1.5f)
        sensitivityChart.description.text = "Pedometer sensitivity"
        setContentView(object : RenderableView(this) {
            override fun view() {
                linearLayout {
                    size(MATCH, MATCH)
                    orientation(LinearLayout.VERTICAL)

                    textView {
                        size(BaseDSL.MATCH, WRAP)
                        BaseDSL.padding(BaseDSL.dip(10))
                        text("Your location will be predicted based on collected WiFi data after " + wifiSampler.sampleCount + " samples")
                    }
                    textView {
                        size(BaseDSL.MATCH, WRAP)
                        gravity(BaseDSL.CENTER_HORIZONTAL)
                        BaseDSL.padding(BaseDSL.dip(10))
                        text("Sampling iteration (WiFi): " + wifiSampler.sampleIndex)
                    }

                    linearLayout {
                        size(MATCH, WRAP)
                        orientation(LinearLayout.HORIZONTAL)
                        textView {
                            size(WRAP, WRAP)
                            text("Place: ")
                        }
                        textView {
                            size(WRAP, WRAP)
                            text(place)
                        }
                    }
                    linearLayout {
                        size(MATCH, WRAP)
                        orientation(LinearLayout.HORIZONTAL)
                        textView {
                            size(WRAP, WRAP)
                            text("Steps: ")
                        }
                        textView {
                            size(WRAP, WRAP)
                            text(steps.toString())
                        }
                    }

                    linearLayout {
                        size(BaseDSL.MATCH, 0)
                        orientation(LinearLayout.VERTICAL)
                        textView {
                            text("Real time data:")
                            size(BaseDSL.WRAP, BaseDSL.WRAP)
                        }
                        linearLayout {
                            orientation(LinearLayout.VERTICAL)
                            size(BaseDSL.MATCH, 0)
                            customView(pedometerChart)
                            weight(1f)
                        }
                        linearLayout {
                            orientation(LinearLayout.VERTICAL)
                            size(BaseDSL.MATCH, 0)
                            customView(sensitivityChart)
                            weight(1f)
                        }
                        weight(1f)
                    }

                    linearLayout {
                        size(MATCH, WRAP)
                        orientation(LinearLayout.HORIZONTAL)
                        button {
                            size(0, WRAP)
                            weight(1f)
                            text("Start")
                            enabled(!wifiSampler.running)
                            onClick {
                                try {
                                    pedometer = Pedometer()
                                    stopSampling()
                                    inertialSampler.samplerListener = object : InertialSamplerListener {
                                        override fun onSampleReceived(sensorSample: SensorSample) {
                                            pedometer.processSample(sensorSample)
                                            if (steps != pedometer.stepCount) {
                                                steps = pedometer.stepCount
                                                Anvil.render()
                                            }
                                        }
                                    }
                                    place = "COLLECTING SAMPLES..."
                                    steps = 0
                                    wifiSampler.startSampling()
                                    pollerThread = createPollerThread()
                                    pollerThread!!.start()
                                    inertialSampler.startSampling()
                                    chartRenderer.startRendering()
                                } catch (ex: Exception) {
                                    Toast.makeText(this@OnlineActivity, "Error: " + ex.toString(), Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        button {
                            size(0, WRAP)
                            weight(1f)
                            enabled(wifiSampler.running || inertialSampler.isRunning)
                            text("Stop")
                            onClick {
                                stopSampling()
                                wifiSampler.stopSampling()
                                pollerThread!!.join()
                            }
                        }
                    }
                }
            }

            private fun customView(chart: View) {
                if (chart.parent is ViewGroup) {
                    (chart.parent as ViewGroup).removeView(chart)
                }
                Anvil.currentView<ViewGroup>().addView(chart, ViewGroup.LayoutParams(BaseDSL.MATCH, BaseDSL.MATCH))
            }
        })
    }

    override fun onPause() {
        super.onPause()
        wifiSampler.stopSampling()
        if (pollerThread != null) {
            pollerThread!!.interrupt()
        }
    }

    private fun stopSampling() {
        inertialSampler.stopSampling()
        chartRenderer.stopRendering()
        chartRenderer.lastRenderIndex = 0
    }

    private fun classify() {
        if (testSet.fingerprints.isEmpty()) {
            place = "NONE"
            Anvil.render()
            return
        }
        try {
            val trainSet = Dao(this@OnlineActivity).findAll().values
                    .filter { it.device.contains("Combo") }
                    .filter { it.type == DatasetType.WIFI }
                    .map { it as WifiDataset }
            place = navigator.classify(
                    trainSet,
                    testSet)
            Anvil.render()
        } catch (ex: Exception) {
            runOnUiThread {
                Toast.makeText(this@OnlineActivity,
                        "Something went wrong, check collected data, log: " + ex.toString(),
                        Toast.LENGTH_LONG).show()
            }
            ex.printStackTrace()
        }
    }
}